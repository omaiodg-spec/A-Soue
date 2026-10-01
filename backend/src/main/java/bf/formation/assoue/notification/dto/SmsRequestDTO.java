package bf.formation.assoue.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsRequestDTO {
    @NotBlank
    private String telephone;
    @NotBlank
    private String type;
    @NotBlank
    private String contenu;
}
