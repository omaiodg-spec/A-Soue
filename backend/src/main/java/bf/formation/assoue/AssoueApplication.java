package bf.formation.assoue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree unique du backend As'Soue depuis la fusion des anciens
 * microservices (auth, catalogue, commande, notification, paiement,
 * signalement) en une seule application Spring Boot. Le frontend reste un
 * projet a part, consommant cette API (architecture client-serveur classique).
 */
@SpringBootApplication
public class AssoueApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssoueApplication.class, args);
    }
}
