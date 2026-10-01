package bf.formation.assoue.signalement.service;

import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.service.EntrepriseService;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import bf.formation.assoue.signalement.dto.SignalementCreateDTO;
import bf.formation.assoue.signalement.dto.SignalementMarketplaceDTO;
import bf.formation.assoue.signalement.dto.SignalementResponseDTO;
import bf.formation.assoue.signalement.exception.AccesRefuseException;
import bf.formation.assoue.signalement.exception.SignalementDejaPrisException;
import bf.formation.assoue.signalement.mapper.SignalementMapper;
import bf.formation.assoue.signalement.model.Signalement;
import bf.formation.assoue.signalement.model.StatutSignalement;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.signalement.repository.SignalementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Auparavant NotificationClient/EntrepriseClient (Feign) etaient mockes.
 * Avec la fusion, SignalementService injecte directement SmsGatewayService
 * et EntrepriseService (auth) : ce sont eux qu'on mocke ici.
 */
@ExtendWith(MockitoExtension.class)
class SignalementServiceTest {

    @Mock private SignalementRepository signalementRepository;
    @Mock private SmsGatewayService smsGatewayService;
    @Mock private EntrepriseService entrepriseService;
    @Mock private GeocodingService geocodingService;
    private final SignalementMapper signalementMapper = new SignalementMapper();

    private SignalementService signalementService;

    @BeforeEach
    void setUp() {
        signalementService = new SignalementService(signalementRepository, signalementMapper, smsGatewayService,
                entrepriseService, geocodingService);
    }

    @Test
    void creer_shouldNotifyAllMatchingEntreprises_closestFirst() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.PLASTIQUE);
        requete.setPhotoUrl("photo.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        // Une entreprise proche (~1km) et une tres loin (Bobo-Dioulasso, ~330km) :
        // les deux doivent etre informees, la plus proche en priorite (envoyee en premier).
        EntrepriseResponseDTO proche = EntrepriseResponseDTO.builder()
                .id(1L).raisonSociale("Recyclage Proche").telephone("70000001")
                .latitude(12.38).longitude(-1.52).typesDechetGeres(Set.of(TypeDechet.PLASTIQUE)).build();
        EntrepriseResponseDTO lointaine = EntrepriseResponseDTO.builder()
                .id(2L).raisonSociale("Recyclage Lointain").telephone("70000002")
                .latitude(11.1771).longitude(-4.2979).typesDechetGeres(Set.of(TypeDechet.PLASTIQUE)).build();

        // Ordre de retour volontairement inverse pour verifier que le tri par distance est bien applique.
        when(entrepriseService.listerParType(TypeDechet.PLASTIQUE)).thenReturn(List.of(lointaine, proche));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> {
            Signalement s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result.getNumeroSuivi()).startsWith("SIG-");
        assertThat(result.getStatut()).isEqualTo(StatutSignalement.EN_ATTENTE);

        InOrder ordre = inOrder(smsGatewayService);
        ordre.verify(smsGatewayService).envoyer(argThat((SmsRequestDTO sms) -> sms.getTelephone().equals("70000001")));
        ordre.verify(smsGatewayService).envoyer(argThat((SmsRequestDTO sms) -> sms.getTelephone().equals("70000002")));
    }

    @Test
    void creer_shouldNotFail_whenEntrepriseServiceThrows() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.AUTRE);
        requete.setPhotoUrl("photo.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        when(entrepriseService.listerParType(TypeDechet.AUTRE)).thenThrow(new RuntimeException("erreur interne"));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result).isNotNull();
        verifyNoInteractions(smsGatewayService);
    }

    @Test
    void creer_shouldStoreAdresse_fromReverseGeocoding() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.AUTRE);
        requete.setPhotoUrl("photo.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        when(geocodingService.reverse(12.3714, -1.5197))
                .thenReturn(Optional.of("Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso"));
        when(entrepriseService.listerParType(TypeDechet.AUTRE)).thenReturn(List.of());
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result.getAdresse()).isEqualTo("Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso");
    }

    @Test
    void creer_shouldLeaveAdresseNull_whenGeocodingUnavailable() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.AUTRE);
        requete.setPhotoUrl("photo.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        when(geocodingService.reverse(any(), any())).thenReturn(Optional.empty());
        when(entrepriseService.listerParType(TypeDechet.AUTRE)).thenReturn(List.of());
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result.getAdresse()).isNull();
    }

    @Test
    void creer_PNEU_shouldAutoAssignToAssoue_sansPasserParLeMarketplace() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.PNEU);
        requete.setPhotoUrl("pneu.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        EntrepriseResponseDTO assoue = EntrepriseResponseDTO.builder()
                .id(1L).raisonSociale("As'Soue Group").telephone("70000099")
                .latitude(12.37).longitude(-1.52).typesDechetGeres(Set.of(TypeDechet.PNEU)).estAssoue(true).build();
        when(entrepriseService.obtenirCompteAssoue()).thenReturn(Optional.of(assoue));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> {
            Signalement s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result.getStatut()).isEqualTo(StatutSignalement.PRIS_EN_CHARGE);
        assertThat(result.getEntrepriseId()).isEqualTo(1L);
        verify(smsGatewayService).envoyer(argThat((SmsRequestDTO sms) -> sms.getTelephone().equals("70000099")));
        verify(entrepriseService, never()).listerParType(any());
    }

    @Test
    void creer_PNEU_shouldStayEnAttente_whenNoAssoueAccountRegistered() {
        SignalementCreateDTO requete = new SignalementCreateDTO();
        requete.setTypeDechet(TypeDechet.PNEU);
        requete.setPhotoUrl("pneu.jpg");
        requete.setLatitude(12.3714);
        requete.setLongitude(-1.5197);

        when(entrepriseService.obtenirCompteAssoue()).thenReturn(Optional.empty());
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.creer(10L, requete);

        assertThat(result.getStatut()).isEqualTo(StatutSignalement.EN_ATTENTE);
        assertThat(result.getEntrepriseId()).isNull();
        verifyNoInteractions(smsGatewayService);
    }

    @Test
    void prendreEnCharge_shouldAssignEntreprise_whenEnAttente() {
        Signalement signalement = Signalement.builder()
                .id(1L).utilisateurId(10L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("p.jpg")
                .latitude(12.0).longitude(-1.0).numeroSuivi("SIG-1").statut(StatutSignalement.EN_ATTENTE).build();
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(signalement));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.prendreEnCharge(1L, 99L);

        assertThat(result.getStatut()).isEqualTo(StatutSignalement.PRIS_EN_CHARGE);
        assertThat(result.getEntrepriseId()).isEqualTo(99L);
    }

    @Test
    void prendreEnCharge_shouldThrow_whenAlreadyTaken() {
        Signalement signalement = Signalement.builder()
                .id(1L).utilisateurId(10L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("p.jpg")
                .latitude(12.0).longitude(-1.0).numeroSuivi("SIG-1")
                .statut(StatutSignalement.PRIS_EN_CHARGE).entrepriseId(50L).build();
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(signalement));

        assertThatThrownBy(() -> signalementService.prendreEnCharge(1L, 99L))
                .isInstanceOf(SignalementDejaPrisException.class);
        verify(signalementRepository, never()).save(any());
    }

    @Test
    void marquerCollecte_shouldSucceed_whenSameEntreprise() {
        Signalement signalement = Signalement.builder()
                .id(1L).utilisateurId(10L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("p.jpg")
                .latitude(12.0).longitude(-1.0).numeroSuivi("SIG-1")
                .statut(StatutSignalement.PRIS_EN_CHARGE).entrepriseId(99L).build();
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(signalement));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));

        SignalementResponseDTO result = signalementService.marquerCollecte(1L, 99L);

        assertThat(result.getStatut()).isEqualTo(StatutSignalement.COLLECTE);
    }

    @Test
    void marquerCollecte_shouldThrow_whenDifferentEntreprise() {
        Signalement signalement = Signalement.builder()
                .id(1L).utilisateurId(10L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("p.jpg")
                .latitude(12.0).longitude(-1.0).numeroSuivi("SIG-1")
                .statut(StatutSignalement.PRIS_EN_CHARGE).entrepriseId(99L).build();
        when(signalementRepository.findById(1L)).thenReturn(Optional.of(signalement));

        assertThatThrownBy(() -> signalementService.marquerCollecte(1L, 42L))
                .isInstanceOf(AccesRefuseException.class);
        verify(signalementRepository, never()).save(any());
    }

    @Test
    void fluxEntreprise_shouldReturnOpenSignalements_sortedByDistance() {
        EntrepriseResponseDTO entreprise = EntrepriseResponseDTO.builder()
                .id(99L).raisonSociale("Recyclage Faso").telephone("70000001")
                .latitude(12.3714).longitude(-1.5197).typesDechetGeres(Set.of(TypeDechet.PLASTIQUE)).build();
        when(entrepriseService.obtenir(99L)).thenReturn(entreprise);

        Signalement loin = Signalement.builder()
                .id(1L).utilisateurId(1L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("a.jpg")
                .latitude(11.1771).longitude(-4.2979).numeroSuivi("SIG-A").statut(StatutSignalement.EN_ATTENTE).build();
        Signalement proche = Signalement.builder()
                .id(2L).utilisateurId(1L).typeDechet(TypeDechet.PLASTIQUE).photoUrl("b.jpg")
                .latitude(12.38).longitude(-1.52).numeroSuivi("SIG-B").statut(StatutSignalement.EN_ATTENTE).build();

        when(signalementRepository.findByTypeDechetAndStatut(TypeDechet.PLASTIQUE, StatutSignalement.EN_ATTENTE))
                .thenReturn(List.of(loin, proche));

        List<SignalementMarketplaceDTO> resultat = signalementService.fluxEntreprise(99L);

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).getNumeroSuivi()).isEqualTo("SIG-B"); // le plus proche en premier
        assertThat(resultat.get(0).getDistanceKm()).isLessThan(resultat.get(1).getDistanceKm());
    }
}
