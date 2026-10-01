package bf.formation.assoue.formation.service;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.service.AuthService;
import bf.formation.assoue.formation.dto.FormationCreateDTO;
import bf.formation.assoue.formation.dto.FormationResponseDTO;
import bf.formation.assoue.formation.dto.InscriptionFormationResponseDTO;
import bf.formation.assoue.formation.exception.DejaInscritFormationException;
import bf.formation.assoue.formation.exception.FormationCompleteException;
import bf.formation.assoue.formation.mapper.FormationMapper;
import bf.formation.assoue.formation.model.Formation;
import bf.formation.assoue.formation.model.InscriptionFormation;
import bf.formation.assoue.formation.repository.FormationRepository;
import bf.formation.assoue.formation.repository.InscriptionFormationRepository;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock private FormationRepository formationRepository;
    @Mock private InscriptionFormationRepository inscriptionRepository;
    @Mock private SmsGatewayService smsGatewayService;
    @Mock private AuthService authService;
    private final FormationMapper formationMapper = new FormationMapper();

    private FormationService formationService;

    @BeforeEach
    void setUp() {
        formationService = new FormationService(formationRepository, inscriptionRepository, formationMapper,
                smsGatewayService, authService);
    }

    @Test
    void creer_shouldSaveFormation() {
        FormationCreateDTO requete = new FormationCreateDTO();
        requete.setTitre("Initiation au tri des dechets");
        requete.setLieu("Ouagadougou, salle municipale");
        requete.setDateFormation(LocalDateTime.now().plusDays(10));
        requete.setPlacesDisponibles(30);

        when(formationRepository.save(any(Formation.class))).thenAnswer(inv -> {
            Formation f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        FormationResponseDTO result = formationService.creer(requete);

        assertThat(result.getTitre()).isEqualTo("Initiation au tri des dechets");
        assertThat(result.getPlacesRestantes()).isEqualTo(30);
    }

    @Test
    void sInscrire_shouldSaveInscriptionAndSendSms() {
        Formation formation = Formation.builder()
                .id(1L).titre("Initiation au tri des dechets").lieu("Ouagadougou")
                .dateFormation(LocalDateTime.now().plusDays(10)).placesDisponibles(30).build();
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(inscriptionRepository.existsByFormationIdAndUtilisateurId(1L, 10L)).thenReturn(false);
        when(inscriptionRepository.countByFormationId(1L)).thenReturn(5L);
        when(inscriptionRepository.save(any(InscriptionFormation.class))).thenAnswer(inv -> {
            InscriptionFormation i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(authService.obtenirParId(10L)).thenReturn(
                UtilisateurResponseDTO.builder().id(10L).nom("Awa").telephone("70000000")
                        .role(Role.CITOYEN).telephoneVerifie(true).build());

        InscriptionFormationResponseDTO result = formationService.sInscrire(10L, 1L);

        assertThat(result.getTitreFormation()).isEqualTo("Initiation au tri des dechets");
        verify(smsGatewayService).envoyer(argThat((SmsRequestDTO sms) -> sms.getTelephone().equals("70000000")));
    }

    @Test
    void sInscrire_shouldThrow_whenAlreadyRegistered() {
        Formation formation = Formation.builder().id(1L).titre("Formation").lieu("Ouaga")
                .dateFormation(LocalDateTime.now().plusDays(10)).build();
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(inscriptionRepository.existsByFormationIdAndUtilisateurId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> formationService.sInscrire(10L, 1L))
                .isInstanceOf(DejaInscritFormationException.class);
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void sInscrire_shouldThrow_whenComplete() {
        Formation formation = Formation.builder().id(1L).titre("Formation").lieu("Ouaga")
                .dateFormation(LocalDateTime.now().plusDays(10)).placesDisponibles(20).build();
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(inscriptionRepository.existsByFormationIdAndUtilisateurId(1L, 10L)).thenReturn(false);
        when(inscriptionRepository.countByFormationId(1L)).thenReturn(20L);

        assertThatThrownBy(() -> formationService.sInscrire(10L, 1L))
                .isInstanceOf(FormationCompleteException.class);
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void sInscrire_shouldSucceed_whenNoCapacityLimit() {
        Formation formation = Formation.builder().id(1L).titre("Formation illimitee").lieu("Ouaga")
                .dateFormation(LocalDateTime.now().plusDays(10)).placesDisponibles(null).build();
        when(formationRepository.findById(1L)).thenReturn(Optional.of(formation));
        when(inscriptionRepository.existsByFormationIdAndUtilisateurId(1L, 10L)).thenReturn(false);
        when(inscriptionRepository.save(any(InscriptionFormation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authService.obtenirParId(10L)).thenReturn(
                UtilisateurResponseDTO.builder().id(10L).telephone("70000000").role(Role.CITOYEN).build());

        InscriptionFormationResponseDTO result = formationService.sInscrire(10L, 1L);

        assertThat(result).isNotNull();
        verify(inscriptionRepository, never()).countByFormationId(any());
    }

    @Test
    void lister_shouldComputePlacesRestantes() {
        Formation formation = Formation.builder().id(1L).titre("Formation").lieu("Ouaga")
                .dateFormation(LocalDateTime.now().plusDays(10)).placesDisponibles(10).build();
        when(formationRepository.findAllByOrderByDateFormationAsc()).thenReturn(List.of(formation));
        when(inscriptionRepository.countByFormationId(1L)).thenReturn(3L);

        List<FormationResponseDTO> result = formationService.lister();

        assertThat(result.get(0).getPlacesRestantes()).isEqualTo(7);
    }
}
