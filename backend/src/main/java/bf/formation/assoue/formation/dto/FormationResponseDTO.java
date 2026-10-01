package bf.formation.assoue.formation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormationResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private String lieu;
    private LocalDateTime dateFormation;
    private Integer placesDisponibles;
    /** Null si placesDisponibles est illimite ; sinon placesDisponibles - nombre d'inscrits. */
    private Integer placesRestantes;
    private LocalDateTime dateCreation;
}
