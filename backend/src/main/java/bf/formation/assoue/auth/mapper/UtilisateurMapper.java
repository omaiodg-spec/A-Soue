package bf.formation.assoue.auth.mapper;

import bf.formation.assoue.auth.dto.UtilisateurResponseDTO;
import bf.formation.assoue.auth.model.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurMapper {

    public UtilisateurResponseDTO toDto(Utilisateur u) {
        return UtilisateurResponseDTO.builder()
                .id(u.getId())
                .nom(u.getNom())
                .telephone(u.getTelephone())
                .role(u.getRole())
                .telephoneVerifie(u.isTelephoneVerifie())
                .build();
    }
}
