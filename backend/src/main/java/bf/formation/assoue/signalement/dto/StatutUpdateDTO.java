package bf.formation.assoue.signalement.dto;

import bf.formation.assoue.signalement.model.StatutSignalement;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatutUpdateDTO {
    @NotNull
    private StatutSignalement statut;
}
