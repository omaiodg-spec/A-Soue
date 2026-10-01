package bf.formation.assoue.formation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FormationCreateDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotBlank(message = "Le lieu est obligatoire")
    private String lieu;

    @NotNull(message = "La date de la formation est obligatoire")
    @Future(message = "La date de la formation doit etre dans le futur")
    private LocalDateTime dateFormation;

    /** Optionnel : laisser vide pour une formation sans limite de places. */
    @Positive(message = "Le nombre de places doit etre positif")
    private Integer placesDisponibles;
}
