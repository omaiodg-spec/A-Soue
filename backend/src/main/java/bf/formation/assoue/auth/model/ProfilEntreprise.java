package bf.formation.assoue.auth.model;

import bf.formation.assoue.common.model.TypeDechet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Profil complementaire d'un compte de role ENTREPRISE : coordonnees
 * geographiques (pour le matching de proximite) et types de dechets geres.
 * Separe de Utilisateur pour ne pas polluer les comptes CITOYEN de colonnes
 * qui ne les concernent pas.
 */
@Entity
@Table(name = "profils_entreprise")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilEntreprise {

    @Id
    private Long utilisateurId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /**
     * Adresse lisible : soit fournie directement par l'ADMIN a la creation, soit
     * obtenue automatiquement par geocodage inverse si seules les coordonnees ont
     * ete fournies (cf. GeocodingService, EntrepriseService.creer).
     */
    private String adresse;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "entreprise_types_dechet", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "type_dechet")
    @Builder.Default
    private Set<TypeDechet> typesDechetGeres = new HashSet<>();

    /**
     * Marque le compte entreprise interne d'As'Soue elle-meme. Un signalement
     * de type PNEU est automatiquement rattache a CETTE entreprise (pas de
     * mise en concurrence marketplace pour les pneus), cf. SignalementService.
     * Un seul compte doit porter ce marqueur (verifie dans EntrepriseService).
     */
    @Column(name = "est_assoue", nullable = false)
    @Builder.Default
    private boolean estAssoue = false;
}
