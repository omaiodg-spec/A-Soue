package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.dto.EntrepriseCreateDTO;
import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.exception.TelephoneDejaUtiliseException;
import bf.formation.assoue.auth.mapper.EntrepriseMapper;
import bf.formation.assoue.auth.model.ProfilEntreprise;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.ProfilEntrepriseRepository;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.exception.LocalisationManquanteException;
import bf.formation.assoue.common.geocoding.GeocodingService;
import bf.formation.assoue.common.geocoding.dto.CoordonneesDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private ProfilEntrepriseRepository profilEntrepriseRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private final EntrepriseMapper entrepriseMapper = new EntrepriseMapper();
    private final TelephoneHasher telephoneHasher = new TelephoneHasher();
    @Mock private GeocodingService geocodingService;

    private EntrepriseService entrepriseService;

    @BeforeEach
    void setUp() {
        setField(telephoneHasher, "cle", "cle_de_test_hachage_telephone_2026");
        entrepriseService = new EntrepriseService(utilisateurRepository, profilEntrepriseRepository, passwordEncoder,
                entrepriseMapper, telephoneHasher, geocodingService);
    }

    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void creer_shouldCreateAccountAlreadyVerified_whenTelephoneFree() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("Recyclage Faso SARL");
        requete.setTelephone("71000000");
        requete.setMotDePasse("motdepasse123");
        requete.setLatitude(12.35);
        requete.setLongitude(-1.53);
        requete.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71000000"))).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(profilEntrepriseRepository.save(any(ProfilEntreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponseDTO result = entrepriseService.creer(requete);

        assertThat(result.getRaisonSociale()).isEqualTo("Recyclage Faso SARL");
        assertThat(result.getTypesDechetGeres()).containsExactly(TypeDechet.PLASTIQUE);

        // Verifie que le compte est cree comme ENTREPRISE deja verifiee (pas d'auto-inscription/OTP).
        org.mockito.ArgumentCaptor<Utilisateur> captor = org.mockito.ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ENTREPRISE);
        assertThat(captor.getValue().isTelephoneVerifie()).isTrue();
        assertThat(captor.getValue().getMotDePasseHash()).isEqualTo("hash");
    }

    @Test
    void creer_shouldThrow_whenTelephoneAlreadyUsed() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setTelephone("71000000");
        requete.setRaisonSociale("Recyclage Faso SARL");
        requete.setMotDePasse("motdepasse123");
        requete.setLatitude(12.35);
        requete.setLongitude(-1.53);
        requete.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71000000"))).thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.creer(requete))
                .isInstanceOf(TelephoneDejaUtiliseException.class);
        verify(profilEntrepriseRepository, never()).save(any());
    }

    @Test
    void creer_shouldCreateAsAssoue_whenFlagSetAndNoneExistsYet() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("As'Soue Group");
        requete.setTelephone("71999999");
        requete.setMotDePasse("motdepasse123");
        requete.setLatitude(12.35);
        requete.setLongitude(-1.53);
        requete.setTypesDechetGeres(Set.of(TypeDechet.PNEU));
        requete.setEstAssoue(true);

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71999999"))).thenReturn(false);
        when(profilEntrepriseRepository.existsByEstAssoueTrue()).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profilEntrepriseRepository.save(any(ProfilEntreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponseDTO result = entrepriseService.creer(requete);

        assertThat(result.isEstAssoue()).isTrue();
    }

    @Test
    void creer_shouldThrow_whenAssoueAccountAlreadyExists() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("As'Soue Group Bis");
        requete.setTelephone("71888888");
        requete.setMotDePasse("motdepasse123");
        requete.setLatitude(12.35);
        requete.setLongitude(-1.53);
        requete.setTypesDechetGeres(Set.of(TypeDechet.PNEU));
        requete.setEstAssoue(true);

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71888888"))).thenReturn(false);
        when(profilEntrepriseRepository.existsByEstAssoueTrue()).thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.creer(requete))
                .isInstanceOf(bf.formation.assoue.auth.exception.AssoueDejaEnregistreeException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void creer_shouldReverseGeocode_whenCoordinatesProvidedDirectly() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("Recyclage Faso SARL");
        requete.setTelephone("71000001");
        requete.setMotDePasse("motdepasse123");
        requete.setLatitude(12.35);
        requete.setLongitude(-1.53);
        requete.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71000001"))).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profilEntrepriseRepository.save(any(ProfilEntreprise.class))).thenAnswer(inv -> inv.getArgument(0));
        when(geocodingService.reverse(12.35, -1.53))
                .thenReturn(java.util.Optional.of("Zone du Bois, Ouagadougou, Burkina Faso"));

        EntrepriseResponseDTO result = entrepriseService.creer(requete);

        assertThat(result.getLatitude()).isEqualTo(12.35);
        assertThat(result.getAdresse()).isEqualTo("Zone du Bois, Ouagadougou, Burkina Faso");
        verify(geocodingService, never()).forward(any());
    }

    @Test
    void creer_shouldForwardGeocode_whenOnlyAdresseProvided() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("Recyclage Faso SARL");
        requete.setTelephone("71000002");
        requete.setMotDePasse("motdepasse123");
        requete.setAdresse("Avenue Kwame Nkrumah, Ouagadougou");
        requete.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71000002"))).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profilEntrepriseRepository.save(any(ProfilEntreprise.class))).thenAnswer(inv -> inv.getArgument(0));
        when(geocodingService.forward("Avenue Kwame Nkrumah, Ouagadougou")).thenReturn(
                CoordonneesDTO.builder().latitude(12.3714).longitude(-1.5197)
                        .adresseTrouvee("Avenue Kwame Nkrumah, Ouagadougou, Burkina Faso").build());

        EntrepriseResponseDTO result = entrepriseService.creer(requete);

        assertThat(result.getLatitude()).isEqualTo(12.3714);
        assertThat(result.getLongitude()).isEqualTo(-1.5197);
        verify(geocodingService, never()).reverse(any(), any());
    }

    @Test
    void creer_shouldThrow_whenNeitherCoordinatesNorAdresseProvided() {
        EntrepriseCreateDTO requete = new EntrepriseCreateDTO();
        requete.setRaisonSociale("Recyclage Faso SARL");
        requete.setTelephone("71000003");
        requete.setMotDePasse("motdepasse123");
        requete.setTypesDechetGeres(Set.of(TypeDechet.PLASTIQUE));

        when(utilisateurRepository.existsByTelephoneHash(telephoneHasher.hacher("71000003"))).thenReturn(false);

        assertThatThrownBy(() -> entrepriseService.creer(requete))
                .isInstanceOf(LocalisationManquanteException.class);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void listerParType_shouldDelegateToRepository() {
        Utilisateur u = Utilisateur.builder().id(5L).telephone("71000000").role(Role.ENTREPRISE).build();
        ProfilEntreprise profil = ProfilEntreprise.builder()
                .utilisateurId(5L).utilisateur(u).raisonSociale("Recyclage Faso")
                .latitude(12.35).longitude(-1.53).typesDechetGeres(Set.of(TypeDechet.PLASTIQUE)).build();
        when(profilEntrepriseRepository.findByTypeDechetGere(TypeDechet.PLASTIQUE)).thenReturn(List.of(profil));

        List<EntrepriseResponseDTO> result = entrepriseService.listerParType(TypeDechet.PLASTIQUE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRaisonSociale()).isEqualTo("Recyclage Faso");
    }
}
