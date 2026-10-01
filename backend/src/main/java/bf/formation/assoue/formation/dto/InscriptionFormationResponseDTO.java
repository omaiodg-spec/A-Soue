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
public class InscriptionFormationResponseDTO {
    private Long id;
    private Long formationId;
    private String titreFormation;
    private LocalDateTime dateFormation;
    private LocalDateTime dateInscription;
}
