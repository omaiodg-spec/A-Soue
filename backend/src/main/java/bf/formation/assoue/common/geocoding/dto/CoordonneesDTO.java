package bf.formation.assoue.common.geocoding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordonneesDTO {
    private Double latitude;
    private Double longitude;
    /** Adresse normalisee telle que renvoyee par Nominatim (utile pour confirmation a l'ecran). */
    private String adresseTrouvee;
}
