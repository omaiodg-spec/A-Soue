package bf.formation.assoue.auth.dto;

import bf.formation.assoue.common.model.TypeDechet;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

/** BF-BO : creation d'un compte entreprise partenaire, reservee a l'ADMIN. */
@Data
public class EntrepriseCreateDTO {

    @NotBlank(message = "La raison sociale est obligatoire")
    private String raisonSociale;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numero de telephone invalide")
    private String telephone;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe initial doit contenir au moins 8 caracteres")
    private String motDePasse;

    /**
     * Fournir latitude+longitude OU adresse (au moins l'un des deux) : si seule
     * l'adresse est donnee, elle est convertie en coordonnees via geocodage
     * (OpenStreetMap/Nominatim, cf. GeocodingService). Verifie dans EntrepriseService.
     */
    private Double latitude;
    private Double longitude;
    private String adresse;

    @NotEmpty(message = "Au moins un type de dechet gere est requis")
    private Set<TypeDechet> typesDechetGeres;

    /**
     * A cocher une seule fois, pour le compte entreprise interne d'As'Soue elle-meme.
     * Tout signalement PNEU lui sera automatiquement rattache (cf. SignalementService).
     */
    private boolean estAssoue;
}
