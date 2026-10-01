package bf.formation.assoue.signalement.dto;

import bf.formation.assoue.common.model.TypeDechet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** BF-SIG-01 + BF-SIG-02 : photo obligatoire + geoloc automatique + type de dechet. */
@Data
public class SignalementCreateDTO {
    @NotNull
    private TypeDechet typeDechet;

    @NotBlank(message = "La photo est obligatoire")
    private String photoUrl;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
