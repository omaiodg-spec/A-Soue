package bf.formation.assoue.auth.controller;

import bf.formation.assoue.auth.crypto.TelephoneHasher;
import bf.formation.assoue.auth.dto.*;
import bf.formation.assoue.auth.model.OtpCode;
import bf.formation.assoue.auth.model.Role;
import bf.formation.assoue.auth.model.TypeOtp;
import bf.formation.assoue.auth.model.Utilisateur;
import bf.formation.assoue.auth.repository.OtpCodeRepository;
import bf.formation.assoue.auth.repository.UtilisateurRepository;
import bf.formation.assoue.notification.service.SmsGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration bout-en-bout (controleur + service + repository + H2, profil "test").
 * SmsGatewayService est mocke : on ne veut pas reellement "envoyer" de SMS pendant les tests
 * (avant la fusion, c'etait NotificationClient qui etait mocke, en Feign).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private OtpCodeRepository otpCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TelephoneHasher telephoneHasher;

    @MockBean
    private SmsGatewayService smsGatewayService;

    @BeforeEach
    void setUp() {
        otpCodeRepository.deleteAll();
        utilisateurRepository.deleteAll();
    }

    @Test
    void inscription_shouldReturn201AndTriggerOtpSms() throws Exception {
        RegisterRequestDTO requete = new RegisterRequestDTO();
        requete.setNom("Awa OUEDRAOGO");
        requete.setTelephone("70000001");
        requete.setMotDePasse("motdepasse123");

        mockMvc.perform(post("/auth/inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telephone").value("70000001"))
                .andExpect(jsonPath("$.telephoneVerifie").value(false));

        org.mockito.Mockito.verify(smsGatewayService).envoyer(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inscription_shouldReturn409_whenTelephoneAlreadyUsed() throws Exception {
        utilisateurRepository.save(Utilisateur.builder()
                .nom("Existant").telephone("70000002").telephoneHash(telephoneHasher.hacher("70000002"))
                .motDePasseHash(passwordEncoder.encode("x"))
                .role(Role.CITOYEN).telephoneVerifie(true).build());

        RegisterRequestDTO requete = new RegisterRequestDTO();
        requete.setNom("Nouveau");
        requete.setTelephone("70000002");
        requete.setMotDePasse("motdepasse123");

        mockMvc.perform(post("/auth/inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isConflict());
    }

    @Test
    void inscription_shouldReturn400_whenPayloadInvalid() throws Exception {
        RegisterRequestDTO requete = new RegisterRequestDTO();
        requete.setNom("");
        requete.setTelephone("pas-un-numero");
        requete.setMotDePasse("court");

        mockMvc.perform(post("/auth/inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void connexion_shouldReturn401_beforePhoneValidated() throws Exception {
        utilisateurRepository.save(Utilisateur.builder()
                .nom("Awa").telephone("70000003").telephoneHash(telephoneHasher.hacher("70000003"))
                .motDePasseHash(passwordEncoder.encode("motdepasse123"))
                .role(Role.CITOYEN).telephoneVerifie(false).build());

        LoginRequestDTO requete = new LoginRequestDTO();
        requete.setTelephone("70000003");
        requete.setMotDePasse("motdepasse123");

        mockMvc.perform(post("/auth/connexion")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void parcoursComplet_inscriptionValidationConnexion_shouldReturnJwt() throws Exception {
        RegisterRequestDTO inscription = new RegisterRequestDTO();
        inscription.setNom("Awa OUEDRAOGO");
        inscription.setTelephone("70000004");
        inscription.setMotDePasse("motdepasse123");

        mockMvc.perform(post("/auth/inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isCreated());

        // Recupere le code OTP directement en base (dans la vraie vie il arrive par SMS).
        OtpCode otp = otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc(
                        "70000004", TypeOtp.VALIDATION_INSCRIPTION)
                .orElseThrow();

        OtpVerifyRequestDTO validation = new OtpVerifyRequestDTO();
        validation.setTelephone("70000004");
        validation.setCode(otp.getCode());

        mockMvc.perform(post("/auth/valider-inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validation)))
                .andExpect(status().isOk());

        LoginRequestDTO connexion = new LoginRequestDTO();
        connexion.setTelephone("70000004");
        connexion.setMotDePasse("motdepasse123");

        mockMvc.perform(post("/auth/connexion")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(connexion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CITOYEN"));
    }

    @Test
    void validerInscription_shouldReturn400_whenCodeIncorrect() throws Exception {
        utilisateurRepository.save(Utilisateur.builder()
                .nom("Awa").telephone("70000005").telephoneHash(telephoneHasher.hacher("70000005"))
                .motDePasseHash(passwordEncoder.encode("motdepasse123"))
                .role(Role.CITOYEN).telephoneVerifie(false).build());
        otpCodeRepository.save(OtpCode.builder()
                .telephone("70000005").code("111111").type(TypeOtp.VALIDATION_INSCRIPTION)
                .dateExpiration(LocalDateTime.now().plusMinutes(5)).utilise(false).build());

        OtpVerifyRequestDTO validation = new OtpVerifyRequestDTO();
        validation.setTelephone("70000005");
        validation.setCode("999999");

        mockMvc.perform(post("/auth/valider-inscription")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validation)))
                .andExpect(status().isBadRequest());
    }
}
