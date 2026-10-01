package bf.formation.assoue.signalement.dto;

import bf.formation.assoue.common.model.TypeDechet;
import bf.formation.assoue.signalement.model.StatutSignalement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Entree du fil "marketplace" d'une entreprise : signalement + distance a vol d'oiseau. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementMarketplaceDTO {
    private Long id;
    private TypeDechet typeDechet;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String adresse;
    private String numeroSuivi;
    private StatutSignalement statut;
    private double distanceKm;
    private LocalDateTime dateCreation;
}
