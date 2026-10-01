package bf.formation.assoue.signalement.dto;

import bf.formation.assoue.signalement.model.StatutSignalement;
import bf.formation.assoue.common.model.TypeDechet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalementResponseDTO {
    private Long id;
    private Long utilisateurId;
    private TypeDechet typeDechet;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String adresse;
    private String numeroSuivi;
    private StatutSignalement statut;
    private Long entrepriseId;
    private LocalDateTime dateCreation;
}
