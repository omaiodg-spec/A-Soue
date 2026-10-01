package bf.formation.assoue.paiement.repository;

import bf.formation.assoue.paiement.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
