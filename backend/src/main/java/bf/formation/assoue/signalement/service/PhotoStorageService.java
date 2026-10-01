package bf.formation.assoue.signalement.service;

import bf.formation.assoue.signalement.exception.PhotoInvalideException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Repond au point souleve dans le CDC (section 3A) : "les photos de signalement
 * doivent etre compressees automatiquement a 800px max et stockees sur le
 * serveur" -- ce n'etait pas implemente jusqu'ici (POST /signalements ne
 * recevait qu'une URL deja hebergee ailleurs).
 *
 * Stockage sur disque local (volume Docker persistant) pour rester simple et
 * gratuit. Si le volume de photos devient important, remplacer ce service par
 * un client S3-compatible (MinIO auto-heberge, ou un fournisseur cloud) sans
 * changer le contrat expose au controleur (toujours une photoUrl en retour).
 */
@Service
@Slf4j
public class PhotoStorageService {

    private static final int TAILLE_MAX_PX = 800;
    private static final Set<String> TYPES_ACCEPTES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024; // 10 Mo avant compression

    @Value("${photos.storage-path}")
    private String cheminStockage;

    /** @return l'URL relative a stocker dans Signalement.photoUrl (ex. "/photos-signalements/xxxx.jpg"). */
    public String stocker(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new PhotoInvalideException("Aucun fichier recu");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new PhotoInvalideException("Photo trop volumineuse (max 10 Mo avant compression)");
        }
        if (!TYPES_ACCEPTES.contains(fichier.getContentType())) {
            throw new PhotoInvalideException("Format non supporte (jpeg, png ou webp attendu)");
        }

        try {
            BufferedImage original = ImageIO.read(fichier.getInputStream());
            if (original == null) {
                throw new PhotoInvalideException("Fichier illisible en tant qu'image");
            }

            BufferedImage redimensionnee = redimensionnerSiNecessaire(original);

            Path dossier = Path.of(cheminStockage);
            Files.createDirectories(dossier);

            String nomFichier = UUID.randomUUID() + ".jpg";
            Path destination = dossier.resolve(nomFichier);
            ImageIO.write(redimensionnee, "jpg", destination.toFile());

            log.info("Photo signalement enregistree : {} ({}x{} -> {}x{})", nomFichier,
                    original.getWidth(), original.getHeight(), redimensionnee.getWidth(), redimensionnee.getHeight());

            return "/photos-signalements/" + nomFichier;
        } catch (IOException e) {
            throw new PhotoInvalideException("Erreur lors du traitement de la photo : " + e.getMessage());
        }
    }

    /** Redimensionne pour que la plus grande dimension ne depasse jamais 800px, en conservant les proportions. */
    private BufferedImage redimensionnerSiNecessaire(BufferedImage original) {
        int largeur = original.getWidth();
        int hauteur = original.getHeight();

        if (largeur <= TAILLE_MAX_PX && hauteur <= TAILLE_MAX_PX) {
            return convertirEnRgbSiNecessaire(original);
        }

        double ratio = (double) TAILLE_MAX_PX / Math.max(largeur, hauteur);
        int nouvelleLargeur = (int) Math.round(largeur * ratio);
        int nouvelleHauteur = (int) Math.round(hauteur * ratio);

        BufferedImage redimensionnee = new BufferedImage(nouvelleLargeur, nouvelleHauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = redimensionnee.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(original, 0, 0, nouvelleLargeur, nouvelleHauteur, null);
        g2d.dispose();
        return redimensionnee;
    }

    /** Le JPEG ne supporte pas la transparence (PNG/webp) : on aplati sur fond blanc avant d'ecrire. */
    private BufferedImage convertirEnRgbSiNecessaire(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgb.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return rgb;
    }
}
