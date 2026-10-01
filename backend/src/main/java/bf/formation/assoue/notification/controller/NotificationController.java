package bf.formation.assoue.notification.controller;

import bf.formation.assoue.notification.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * L'ancienne route POST /notifications/sms (appelee via Feign par les autres
 * microservices) a disparu avec la fusion : auth, commande et signalement
 * appellent desormais SmsGatewayService directement (injection Spring),
 * plus besoin d'exposer l'envoi de SMS en HTTP.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    /** BF-BO : suivi/audit des notifications envoyees, reserve a l'equipe As'Soue. */
    @GetMapping
    public List<bf.formation.assoue.notification.model.NotificationLog> lister() {
        return notificationLogRepository.findAll();
    }
}
