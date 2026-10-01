package bf.formation.assoue.notification.service;

import bf.formation.assoue.notification.dto.NotificationResponseDTO;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.model.NotificationLog;
import bf.formation.assoue.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsGatewayServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    private SmsGatewayService smsGatewayService;

    @BeforeEach
    void setUp() {
        smsGatewayService = new SmsGatewayService(notificationLogRepository);
    }

    @Test
    void envoyer_shouldPersistLogAndReturnResponse() {
        SmsRequestDTO requete = new SmsRequestDTO();
        requete.setTelephone("70000000");
        requete.setType("OTP");
        requete.setContenu("Votre code : 123456");

        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(inv -> {
            NotificationLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        NotificationResponseDTO result = smsGatewayService.envoyer(requete);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTelephone()).isEqualTo("70000000");
        assertThat(result.getType()).isEqualTo("OTP");
        assertThat(result.isEnvoyeAvecSucces()).isTrue();
    }

    @Test
    void envoyer_shouldThrow_whenTypeUnknown() {
        SmsRequestDTO requete = new SmsRequestDTO();
        requete.setTelephone("70000000");
        requete.setType("TYPE_INEXISTANT");
        requete.setContenu("test");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> smsGatewayService.envoyer(requete));
    }
}
