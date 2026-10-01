package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.dto.*;
import bf.formation.assoue.auth.exception.IdentifiantsInvalidesException;
import bf.formation.assoue.auth.exception.TelephoneDejaUtiliseException;
import bf.formation.assoue.auth.mapper.UtilisateurMapper;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.model.TypeOtp;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import bf.formation.assoue.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;
    private final UtilisateurMapper utilisateurMapper = new UtilisateurMapper();
    // Hachage deterministe reel (pas un mock) : les tests verifient le meme comportement
    // qu'en production, ou telephone est chiffre et telephoneHash sert aux recherches.
    private final TelephoneHasher telephoneHasher = new TelephoneHasher();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        setField(telephoneHasher, "cle", "cle_de_test_hachage_telephone_2026");
        authService = new AuthService(utilisateurRepository, passwordEncoder, jwtService, otpService,
                utilisateurMapper, telephoneHasher);
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

    private String hash(String telephone) {
        return telephoneHasher.hacher(telephone);
    }

    @Test
    void inscrire_shouldHashPasswordAndSendOtp_whenTelephoneFree() {
        RegisterRequestDTO requete = new RegisterRequestDTO();
        requete.setNom("Awa OUEDRAOGO");
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");

        when(utilisateurRepository.existsByTelephoneHash(hash("70000000"))).thenReturn(false);
        when(passwordEncoder.encode("motdepasse123")).thenReturn("hash");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UtilisateurResponseDTO result = authService.inscrire(requete);

        assertThat(result.getTelephone()).isEqualTo("70000000");
        assertThat(result.getRole()).isEqualTo(Role.CITOYEN);
        verify(otpService).genererEtEnvoyer("70000000", TypeOtp.VALIDATION_INSCRIPTION);
    }

    @Test
    void inscrire_shouldThrow_whenTelephoneAlreadyUsed() {
        RegisterRequestDTO requete = new RegisterRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");
        requete.setNom("Awa");

        when(utilisateurRepository.existsByTelephoneHash(hash("70000000"))).thenReturn(true);

        assertThatThrownBy(() -> authService.inscrire(requete))
                .isInstanceOf(TelephoneDejaUtiliseException.class);
        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(otpService);
    }

    @Test
    void connecter_shouldReturnJwt_whenCredentialsValidAndPhoneVerified() {
        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L).nom("Awa").telephone("70000000").telephoneHash(hash("70000000")).motDePasseHash("hash")
                .role(Role.CITOYEN).telephoneVerifie(true).build();

        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("motdepasse123", "hash")).thenReturn(true);
        when(jwtService.generateToken(1L, "70000000", "CITOYEN")).thenReturn("un.jwt.token");

        JwtResponseDTO result = authService.connecter(requete);

        assertThat(result.getToken()).isEqualTo("un.jwt.token");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo("CITOYEN");
        assertThat(result.isOtpRequis()).isFalse();
    }

    @Test
    void connecter_shouldSendOtpAndWithholdToken_whenRoleIsAdmin() {
        // BNF Securite : le mot de passe seul ne suffit pas pour un compte ADMIN.
        Utilisateur admin = Utilisateur.builder()
                .id(1L).nom("Admin").telephone("70000000").telephoneHash(hash("70000000")).motDePasseHash("hash")
                .role(Role.ADMIN).telephoneVerifie(true).build();

        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("motdepasse123", "hash")).thenReturn(true);

        JwtResponseDTO result = authService.connecter(requete);

        assertThat(result.isOtpRequis()).isTrue();
        assertThat(result.getToken()).isNull();
        verify(otpService).genererEtEnvoyer("70000000", TypeOtp.DEUX_FACTEURS_ADMIN);
        verifyNoInteractions(jwtService);
    }

    @Test
    void verifierDeuxFacteurs_shouldReturnJwt_whenOtpValidAndRoleAdmin() {
        Utilisateur admin = Utilisateur.builder()
                .id(1L).telephone("70000000").telephoneHash(hash("70000000")).role(Role.ADMIN).build();

        OtpVerifyRequestDTO requete = new OtpVerifyRequestDTO();
        requete.setTelephone("70000000");
        requete.setCode("123456");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(1L, "70000000", "ADMIN")).thenReturn("un.jwt.token");

        JwtResponseDTO result = authService.verifierDeuxFacteurs(requete);

        verify(otpService).verifier("70000000", "123456", TypeOtp.DEUX_FACTEURS_ADMIN);
        assertThat(result.getToken()).isEqualTo("un.jwt.token");
        assertThat(result.isOtpRequis()).isFalse();
    }

    @Test
    void connecter_shouldThrow_whenPasswordWrong() {
        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L).telephone("70000000").telephoneHash(hash("70000000")).motDePasseHash("hash")
                .role(Role.CITOYEN).telephoneVerifie(true).build();

        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("mauvais");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.connecter(requete))
                .isInstanceOf(IdentifiantsInvalidesException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void connecter_shouldThrow_whenTelephoneNotVerified() {
        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L).telephone("70000000").telephoneHash(hash("70000000")).motDePasseHash("hash")
                .role(Role.CITOYEN).telephoneVerifie(false).build();

        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("motdepasse123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.connecter(requete))
                .isInstanceOf(IdentifiantsInvalidesException.class)
                .hasMessageContaining("valide");
    }

    @Test
    void connecter_shouldThrow_whenTelephoneUnknown() {
        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000000");
        requete.setMotDePasse("motdepasse123");

        when(utilisateurRepository.findByTelephoneHash(hash("70000000"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.connecter(requete))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }
}
