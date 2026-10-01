package bf.formation.assoue.auth.service;

import bf.formation.assoue.auth.exception.OtpInvalideException;
import bf.formation.assoue.auth.model.OtpCode;
import bf.formation.assoue.auth.model.TypeOtp;
import bf.formation.assoue.auth.repository.OtpCodeRepository;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.service.SmsGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/** BF-AUTH-02 / BF-AUTH-04 : generation et verification des codes OTP par SMS. */
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_VALIDITE_MINUTES = 5;
    private static final int MAX_TENTATIVES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final SmsGatewayService smsGatewayService;

    public void genererEtEnvoyer(String telephone, TypeOtp type) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode otp = OtpCode.builder()
                .telephone(telephone)
                .code(code)
                .type(type)
                .dateExpiration(LocalDateTime.now().plusMinutes(OTP_VALIDITE_MINUTES))
                .build();
        otpCodeRepository.save(otp);

        SmsRequestDTO sms = new SmsRequestDTO();
        sms.setTelephone(telephone);
        sms.setType("OTP");
        sms.setContenu("Votre code As'Soue Store : " + code + " (valable " + OTP_VALIDITE_MINUTES + " min)");
        smsGatewayService.envoyer(sms);
    }

    public void verifier(String telephone, String code, TypeOtp type) {
        OtpCode otp = otpCodeRepository
                .findTopByTelephoneAndTypeAndUtiliseFalseOrderByIdDesc(telephone, type)
                .orElseThrow(() -> new OtpInvalideException("Aucun code OTP en attente pour ce numero"));

        if (otp.isUtilise() || otp.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new OtpInvalideException("Le code OTP a expire, veuillez en redemander un");
        }
        // Anti brute-force : sans cette limite, un code a 6 chiffres est jouable
        // en quelques milliers de requetes pendant sa fenetre de validite de 5 min.
        if (otp.getTentatives() >= MAX_TENTATIVES) {
            otp.setUtilise(true);
            otpCodeRepository.save(otp);
            throw new OtpInvalideException("Nombre maximal de tentatives atteint, veuillez redemander un code");
        }
        if (!otp.getCode().equals(code)) {
            otp.setTentatives(otp.getTentatives() + 1);
            otpCodeRepository.save(otp);
            throw new OtpInvalideException("Code OTP incorrect");
        }
        otp.setUtilise(true);
        otpCodeRepository.save(otp);
    }
}
