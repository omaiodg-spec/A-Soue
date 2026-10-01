package bf.formation.assoue.notification.service;

import bf.formation.assoue.notification.dto.NotificationResponseDTO;
import bf.formation.assoue.notification.dto.SmsRequestDTO;
import bf.formation.assoue.notification.model.NotificationLog;
import bf.formation.assoue.notification.model.TypeNotification;
import bf.formation.assoue.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simule l'envoi de SMS. A remplacer par un vrai appel API (passerelle SMS
 * locale Burkina Faso ou fournisseur type Twilio) quand les identifiants
 * seront fournis par As'Soue (cf. CDC section 3A).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsGatewayService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationResponseDTO envoyer(SmsRequestDTO requete) {
        log.info("[SMS SIMULE] a {} ({}) : {}", requete.getTelephone(), requete.getType(), requete.getContenu());

        NotificationLog notificationLog = NotificationLog.builder()
                .telephone(requete.getTelephone())
                .type(TypeNotification.valueOf(requete.getType()))
                .contenu(requete.getContenu())
                .envoyeAvecSucces(true)
                .build();
        notificationLog = notificationLogRepository.save(notificationLog);

        return NotificationResponseDTO.builder()
                .id(notificationLog.getId())
                .telephone(notificationLog.getTelephone())
                .type(notificationLog.getType().name())
                .envoyeAvecSucces(notificationLog.isEnvoyeAvecSucces())
                .dateEnvoi(notificationLog.getDateEnvoi())
                .build();
    }
}
