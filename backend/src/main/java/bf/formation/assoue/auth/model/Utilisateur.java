package bf.formation.assoue.auth.model;

import bf.formation.assoue.auth.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BF-AUTH-01 : compte utilisateur (nom, telephone, mot de passe).
 * Le telephone sert d'identifiant de connexion (pas d'email dans le CDC).
 *
 * nom / telephone sont chiffres au repos (AES-256-GCM, cf. EncryptedStringConverter)
 * pour respecter l'exigence CIL Burkina Faso du CDC (section 3B). Le chiffrement
 * etant non deterministe, telephoneHash (HMAC-SHA256, deterministe) porte desormais
 * la contrainte d'unicite et sert a toutes les recherches par numero.
 */
@Entity
@Table(name = "utilisateurs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String nom;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String telephone;

    @Column(name = "telephone_hash", nullable = false, unique = true)
    private String telephoneHash;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CITOYEN;

    @Builder.Default
    private boolean telephoneVerifie = false;

    @Builder.Default
    private boolean deuxFacteursActif = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
