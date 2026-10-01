package bf.formation.assoue.signalement.mapper;

import bf.formation.assoue.signalement.dto.SignalementResponseDTO;
import bf.formation.assoue.signalement.model.Signalement;
import org.springframework.stereotype.Component;

@Component
public class SignalementMapper {
    public SignalementResponseDTO toDto(Signalement s) {
        return SignalementResponseDTO.builder()
                .id(s.getId())
                .utilisateurId(s.getUtilisateurId())
                .typeDechet(s.getTypeDechet())
                .photoUrl(s.getPhotoUrl())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .adresse(s.getAdresse())
                .numeroSuivi(s.getNumeroSuivi())
                .statut(s.getStatut())
                .entrepriseId(s.getEntrepriseId())
                .dateCreation(s.getDateCreation())
                .build();
    }
}
