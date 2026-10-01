package bf.formation.assoue.paiement.model;

import bf.formation.assoue.common.model.Operateur;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** BF-SHOP-03 : transaction de paiement mobile money liee a une commande. */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commande_id", nullable = false)
    private Long commandeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operateur operateur;

    @Column(nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutTransaction statut = StatutTransaction.INITIEE;

    @Column(name = "reference_externe")
    private String referenceExterne;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
