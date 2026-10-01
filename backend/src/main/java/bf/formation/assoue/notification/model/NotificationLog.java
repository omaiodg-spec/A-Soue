package bf.formation.assoue.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trace de chaque SMS/alerte envoye. Le CDC (section 3A) prevoit une
 * "passerelle SMS locale ou API" a integrer plus tard : cette entite
 * permet de brancher un vrai fournisseur (Orange/Twilio/etc.) sans
 * changer le contrat expose aux autres microservices.
 */
@Entity
@Table(name = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Column(nullable = false, length = 500)
    private String contenu;

    @Builder.Default
    private boolean envoyeAvecSucces = true;

    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();
}
