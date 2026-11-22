package bf.formation.assoue.signalement.dto;

import bf.formation.assoue.common.model.TypeDechet;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** BF-SIG-01 + BF-SIG-02 : photo obligatoire + geoloc automatique + type de dechet. */
@Data
public class SignalementCreateDTO {
    @NotNull
    private TypeDechet typeDechet;

    @NotBlank(message = "La photo est obligatoire")
    @Pattern(regexp = "^/photos-signalements/[0-9a-fA-F-]{36}\\.jpg$",
             message = "photoUrl invalide : utilisez l'URL renvoyee par POST /signalements/photos")
    private String photoUrl;

    @NotNull
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitude;
}
