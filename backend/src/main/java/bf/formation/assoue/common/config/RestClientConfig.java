package bf.formation.assoue.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Utilise pour appeler des services externes (Nominatim/OpenStreetMap pour le
     * geocodage). Timeouts courts volontaires : un service externe lent ne doit
     * jamais faire attendre longtemps une requete utilisateur (cf. GeocodingService,
     * qui encapsule de toute facon chaque appel dans un try/catch "best effort").
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
