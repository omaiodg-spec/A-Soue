package bf.formation.assoue.auth.dto;

import bf.formation.assoue.common.model.TypeDechet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntrepriseResponseDTO {
    private Long id;
    private String raisonSociale;
    private String telephone;
    private Double latitude;
    private Double longitude;
    private String adresse;
    private Set<TypeDechet> typesDechetGeres;
    private boolean estAssoue;
}
