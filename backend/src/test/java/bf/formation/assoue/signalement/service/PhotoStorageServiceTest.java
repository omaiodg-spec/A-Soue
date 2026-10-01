package bf.formation.assoue.signalement.service;

import bf.formation.assoue.signalement.exception.PhotoInvalideException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoStorageServiceTest {

    @TempDir
    Path dossierTemporaire;

    private PhotoStorageService photoStorageService;

    @BeforeEach
    void setUp() {
        photoStorageService = new PhotoStorageService();
        ReflectionTestUtils.setField(photoStorageService, "cheminStockage", dossierTemporaire.toString());
    }

    @AfterEach
    void tearDown() {
        // Rien a faire : @TempDir nettoie automatiquement apres chaque test.
    }

    private byte[] genererImageJpeg(int largeur, int hauteur) throws IOException {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    void stocker_shouldResizeToMax800px_whenLarger() throws IOException {
        byte[] contenu = genererImageJpeg(1600, 1200);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", contenu);

        String photoUrl = photoStorageService.stocker(fichier);

        assertThat(photoUrl).startsWith("/photos-signalements/").endsWith(".jpg");
        String nomFichier = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        BufferedImage resultat = ImageIO.read(dossierTemporaire.resolve(nomFichier).toFile());

        assertThat(Math.max(resultat.getWidth(), resultat.getHeight())).isEqualTo(800);
        // Proportions conservees (1600x1200 = ratio 4:3 -> 800x600).
        assertThat(resultat.getWidth()).isEqualTo(800);
        assertThat(resultat.getHeight()).isEqualTo(600);
    }

    @Test
    void stocker_shouldKeepOriginalSize_whenAlreadySmall() throws IOException {
        byte[] contenu = genererImageJpeg(400, 300);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", contenu);

        String photoUrl = photoStorageService.stocker(fichier);

        String nomFichier = photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        BufferedImage resultat = ImageIO.read(dossierTemporaire.resolve(nomFichier).toFile());

        assertThat(resultat.getWidth()).isEqualTo(400);
        assertThat(resultat.getHeight()).isEqualTo(300);
    }

    @Test
    void stocker_shouldThrow_whenFileEmpty() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> photoStorageService.stocker(fichier))
                .isInstanceOf(PhotoInvalideException.class);
    }

    @Test
    void stocker_shouldThrow_whenContentTypeNotAnImage() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> photoStorageService.stocker(fichier))
                .isInstanceOf(PhotoInvalideException.class);
    }

    @Test
    void stocker_shouldThrow_whenFileTooLarge() {
        byte[] contenu = new byte[11 * 1024 * 1024]; // 11 Mo > limite de 10 Mo
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", contenu);

        assertThatThrownBy(() -> photoStorageService.stocker(fichier))
                .isInstanceOf(PhotoInvalideException.class);
    }
}
