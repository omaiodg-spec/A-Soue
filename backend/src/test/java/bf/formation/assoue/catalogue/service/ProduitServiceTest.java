package bf.formation.assoue.catalogue.service;

import bf.formation.assoue.catalogue.dto.ProduitCreateDTO;
import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.exception.ProduitNotFoundException;
import bf.formation.assoue.catalogue.mapper.ProduitMapper;
import bf.formation.assoue.catalogue.model.Produit;
import bf.formation.assoue.catalogue.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock private ProduitRepository produitRepository;
    private final ProduitMapper produitMapper = new ProduitMapper();

    private ProduitService produitService;

    @BeforeEach
    void setUp() {
        produitService = new ProduitService(produitRepository, produitMapper);
    }

    @Test
    void creer_shouldSaveProduit() {
        ProduitCreateDTO dto = new ProduitCreateDTO();
        dto.setTitre("Pagne recycle");
        dto.setDescription("Tisse a partir de sachets plastiques collectes");
        dto.setPrixFcfa(BigDecimal.valueOf(5000));
        dto.setDisponible(true);

        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> {
            Produit p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProduitResponseDTO result = produitService.creer(dto);

        assertThat(result.getTitre()).isEqualTo("Pagne recycle");
        assertThat(result.getPrixFcfa()).isEqualByComparingTo("5000");
        assertThat(result.isDisponible()).isTrue();
    }

    @Test
    void obtenir_shouldThrow_whenNotFound() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produitService.obtenir(99L))
                .isInstanceOf(ProduitNotFoundException.class);
    }

    @Test
    void lister_shouldReturnAllProduits() {
        Produit p = Produit.builder().id(1L).titre("Pagne").prixFcfa(BigDecimal.TEN).disponible(true).build();
        when(produitRepository.findAll()).thenReturn(List.of(p));

        List<ProduitResponseDTO> result = produitService.lister();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).isEqualTo("Pagne");
    }

    @Test
    void supprimer_shouldThrow_whenNotFound() {
        when(produitRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> produitService.supprimer(99L))
                .isInstanceOf(ProduitNotFoundException.class);
        verify(produitRepository, never()).deleteById(any());
    }

    @Test
    void modifier_shouldUpdateFields_whenFound() {
        Produit existant = Produit.builder().id(1L).titre("Ancien").prixFcfa(BigDecimal.TEN).disponible(true).build();
        when(produitRepository.findById(1L)).thenReturn(Optional.of(existant));
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        ProduitCreateDTO dto = new ProduitCreateDTO();
        dto.setTitre("Nouveau titre");
        dto.setPrixFcfa(BigDecimal.valueOf(2000));
        dto.setDisponible(false);

        ProduitResponseDTO result = produitService.modifier(1L, dto);

        assertThat(result.getTitre()).isEqualTo("Nouveau titre");
        assertThat(result.isDisponible()).isFalse();
    }
}
