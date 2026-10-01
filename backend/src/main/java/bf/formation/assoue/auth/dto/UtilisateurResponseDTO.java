package bf.formation.assoue.auth.dto;

import bf.formation.assoue.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponseDTO {
    private Long id;
    private String nom;
    private String telephone;
    private Role role;
    private boolean telephoneVerifie;
}
