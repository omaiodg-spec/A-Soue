package bf.formation.assoue.formation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Inscription d'un utilisateur (tous roles) a une Formation. Un seul enregistrement par paire. */
@Entity
@Table(name = "inscriptions_formation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"formation_id", "utilisateur_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "formation_id", nullable = false)
    private Long formationId;

    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Builder.Default
    @Column(name = "date_inscription")
    private LocalDateTime dateInscription = LocalDateTime.now();
}
