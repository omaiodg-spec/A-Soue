package bf.formation.assoue.signalement.model;

import bf.formation.assoue.common.model.TypeDechet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** BF-SIG-01 a 05 : declaration de dechets geolocalisee avec photo. */
@Entity
@Table(name = "signalements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant recupere depuis le JWT (userId), pas de relation JPA
    // inter-service : chaque microservice possede sa propre base (cf. schema).
    @Column(name = "utilisateur_id", nullable = false)
    private Long utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDechet typeDechet;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /**
     * Adresse lisible obtenue par geocodage inverse (OpenStreetMap/Nominatim) a partir
     * de latitude/longitude, cf. GeocodingService. Toujours "best effort" : peut rester
     * null si le service de geocodage etait indisponible au moment de la creation.
     */
    private String adresse;

    // BF-SIG-03 : numero de suivi unique communique a l'utilisateur.
    @Column(name = "numero_suivi", nullable = false, unique = true)
    private String numeroSuivi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutSignalement statut = StatutSignalement.EN_ATTENTE;

    // Renseigne uniquement au moment ou une entreprise prend en charge le signalement
    // (marketplace : premiere entreprise du bon type qui clique le recupere).
    @Column(name = "entreprise_id")
    private Long entrepriseId;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
