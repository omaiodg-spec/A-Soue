package bf.formation.assoue.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Mise a jour du compte par son titulaire, une fois connecte. Le mot de
 * passe actuel est obligatoire pour confirmer l'identite avant tout
 * changement (nom, telephone ou mot de passe) -- meme si un JWT valide est
 * deja presente, c'est une securite standard contre un JWT vole/laisse
 * ouvert sur un poste partage.
 *
 * nom / telephone / nouveauMotDePasse sont tous optionnels : seuls les
 * champs non vides envoyes par le client sont mis a jour (cf.
 * AuthService.mettreAJourCompte).
 */
@Data
public class MettreAJourCompteDTO {

    @NotBlank(message = "Le mot de passe actuel est obligatoire pour confirmer la modification")
    private String motDePasseActuel;

    private String nom;

    private String telephone;

    @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caracteres")
    private String nouveauMotDePasse;
}
