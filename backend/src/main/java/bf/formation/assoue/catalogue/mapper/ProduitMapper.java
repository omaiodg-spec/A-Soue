package bf.formation.assoue.catalogue.mapper;

import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.model.Produit;
import org.springframework.stereotype.Component;

@Component
public class ProduitMapper {
    public ProduitResponseDTO toDto(Produit p) {
        return ProduitResponseDTO.builder()
                .id(p.getId())
                .titre(p.getTitre())
                .description(p.getDescription())
                .photoUrl(p.getPhotoUrl())
                .prixFcfa(p.getPrixFcfa())
                .disponible(p.isDisponible())
                .build();
    }
}
