package bf.formation.assoue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** BF-AUTH-02 : validation du numero de telephone via code OTP. */
@Data
public class OtpVerifyRequestDTO {
    @NotBlank
    private String telephone;
    @NotBlank
    private String code;
}
