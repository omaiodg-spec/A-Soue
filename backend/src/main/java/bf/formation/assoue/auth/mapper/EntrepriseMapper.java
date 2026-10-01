package bf.formation.assoue.auth.mapper;

import bf.formation.assoue.auth.dto.EntrepriseResponseDTO;
import bf.formation.assoue.auth.model.ProfilEntreprise;
import org.springframework.stereotype.Component;

@Component
public class EntrepriseMapper {
    public EntrepriseResponseDTO toDto(ProfilEntreprise p) {
        return EntrepriseResponseDTO.builder()
                .id(p.getUtilisateurId())
                .raisonSociale(p.getRaisonSociale())
                .telephone(p.getUtilisateur().getTelephone())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .adresse(p.getAdresse())
                .typesDechetGeres(p.getTypesDechetGeres())
                .estAssoue(p.isEstAssoue())
                .build();
    }
}
