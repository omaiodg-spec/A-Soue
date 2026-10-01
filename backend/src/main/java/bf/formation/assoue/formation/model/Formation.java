package bf.formation.assoue.formation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Formation proposee par As'Soue a la population (tri des dechets, recyclage,
 * entrepreneuriat vert, etc.). Cree et geree par l'ADMIN ; les utilisateurs
 * (tous roles confondus) peuvent s'y inscrire via InscriptionFormation.
 */
@Entity
@Table(name = "formations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String lieu;

    @Column(name = "date_formation", nullable = false)
    private LocalDateTime dateFormation;

    /** Null = pas de limite de places. */
    @Column(name = "places_disponibles")
    private Integer placesDisponibles;

    @Builder.Default
    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();
}
