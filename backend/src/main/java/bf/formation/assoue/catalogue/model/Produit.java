package bf.formation.assoue.catalogue.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** BF-SHOP-01 + BF-BO-01 : produit recycle du catalogue As'Soue. */
@Entity
@Table(name = "produits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "prix_fcfa", nullable = false)
    private BigDecimal prixFcfa;

    @Builder.Default
    private boolean disponible = true;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
