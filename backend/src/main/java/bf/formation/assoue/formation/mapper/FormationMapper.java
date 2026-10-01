package bf.formation.assoue.formation.mapper;

import bf.formation.assoue.formation.dto.FormationResponseDTO;
import bf.formation.assoue.formation.model.Formation;
import org.springframework.stereotype.Component;

@Component
public class FormationMapper {
    public FormationResponseDTO toDto(Formation f, long nombreInscrits) {
        Integer placesRestantes = f.getPlacesDisponibles() == null
                ? null
                : Math.max(0, f.getPlacesDisponibles() - (int) nombreInscrits);

        return FormationResponseDTO.builder()
                .id(f.getId())
                .titre(f.getTitre())
                .description(f.getDescription())
                .lieu(f.getLieu())
                .dateFormation(f.getDateFormation())
                .placesDisponibles(f.getPlacesDisponibles())
                .placesRestantes(placesRestantes)
                .dateCreation(f.getDateCreation())
                .build();
    }
}
