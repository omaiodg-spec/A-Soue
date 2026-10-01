package bf.formation.assoue.signalement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expose le dossier de stockage des photos de signalement (cf. PhotoStorageService)
 * sous /photos-signalements/** -- c'est cette URL, servie directement par Spring
 * (pas de base de donnees impliquee), qui est renvoyee comme Signalement.photoUrl.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${photos.storage-path}")
    private String cheminStockage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = cheminStockage.endsWith("/") ? cheminStockage : cheminStockage + "/";
        registry.addResourceHandler("/photos-signalements/**")
                .addResourceLocations("file:" + location);
    }
}
