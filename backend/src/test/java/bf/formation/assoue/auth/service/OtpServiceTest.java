package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.exception.OtpInvalideException;
import bf.formation.assoue.auth.model.OtpCode;
import bf.formation.assoue.auth.model.TypeOtp;
import bf.formation.assoue.auth.repository.OtpCodeRepository;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpCodeRepository otpCodeRepository;
    @Mock
    private SmsGatewayService smsGatewayService;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpCodeRepository, smsGatewayService);
    }

    @Test
    void genererEtEnvoyer_shouldSaveOtpAndSendSms() {
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        otpService.genererEtEnvoyer("70000000", TypeOtp.VALIDATION_INSCRIPTION);

        ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(otpCaptor.capture());
        OtpCode saved = otpCaptor.getValue();
        assertThat(saved.getTelephone()).isEqualTo("70000000");
        assertThat(saved.getCode()).matches("\\d{6}");
        assertThat(saved.isUtilise()).isFalse();

        ArgumentCaptor<SmsRequestDTO> smsCaptor = ArgumentCaptor.forClass(SmsRequestDTO.class);
        verify(smsGatewayService).envoyer(smsCaptor.capture());
        assertThat(smsCaptor.getValue().getTelephone()).isEqualTo("70000000");
        assertThat(smsCaptor.getValue().getContenu()).contains(saved.getCode());
    }

    @Test
    void verifier_shouldPass_whenCodeValidAndNotExpired() {
        OtpCode otp = OtpCode.builder()
                .telephone("70000000").code("123456").type(TypeOtp.VALIDATION_INSCRIPTION)
                .dateExpiration(LocalDateTime.now().plusMinutes(3)).utilise(false).build();
        when(otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc("70000000", TypeOtp.VALIDATION_INSCRIPTION))
                .thenReturn(Optional.of(otp));

        otpService.verifier("70000000", "123456", TypeOtp.VALIDATION_INSCRIPTION);

        assertThat(otp.isUtilise()).isTrue();
        verify(otpCodeRepository).save(otp);
    }

    @Test
    void verifier_shouldThrowAndIncrementTentatives_whenCodeIncorrect() {
        OtpCode otp = OtpCode.builder()
                .telephone("70000000").code("123456").type(TypeOtp.VALIDATION_INSCRIPTION)
                .dateExpiration(LocalDateTime.now().plusMinutes(3)).utilise(false).tentatives(0).build();
        when(otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc("70000000", TypeOtp.VALIDATION_INSCRIPTION))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifier("70000000", "000000", TypeOtp.VALIDATION_INSCRIPTION))
                .isInstanceOf(OtpInvalideException.class);

        // Anti brute-force : la tentative echouee est comptabilisee, le code n'est pas encore invalide.
        assertThat(otp.getTentatives()).isEqualTo(1);
        assertThat(otp.isUtilise()).isFalse();
        verify(otpCodeRepository).save(otp);
    }

    @Test
    void verifier_shouldLockCode_afterMaxTentatives() {
        OtpCode otp = OtpCode.builder()
                .telephone("70000000").code("123456").type(TypeOtp.VALIDATION_INSCRIPTION)
                .dateExpiration(LocalDateTime.now().plusMinutes(3)).utilise(false).tentatives(5).build();
        when(otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc("70000000", TypeOtp.VALIDATION_INSCRIPTION))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifier("70000000", "123456", TypeOtp.VALIDATION_INSCRIPTION))
                .isInstanceOf(OtpInvalideException.class)
                .hasMessageContaining("tentatives");

        // Meme avec le bon code : au-dela de la limite, le code est definitivement invalide.
        assertThat(otp.isUtilise()).isTrue();
    }

    @Test
    void verifier_shouldThrow_whenCodeExpired() {
        OtpCode otp = OtpCode.builder()
                .telephone("70000000").code("123456").type(TypeOtp.VALIDATION_INSCRIPTION)
                .dateExpiration(LocalDateTime.now().minusMinutes(1)).utilise(false).build();
        when(otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc("70000000", TypeOtp.VALIDATION_INSCRIPTION))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifier("70000000", "123456", TypeOtp.VALIDATION_INSCRIPTION))
                .isInstanceOf(OtpInvalideException.class)
                .hasMessageContaining("expire");
    }

    @Test
    void verifier_shouldThrow_whenNoOtpPending() {
        when(otpCodeRepository.findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc("70000000", TypeOtp.VALIDATION_INSCRIPTION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifier("70000000", "123456", TypeOtp.VALIDATION_INSCRIPTION))
                .isInstanceOf(OtpInvalideException.class);
    }
}
