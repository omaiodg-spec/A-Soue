package bf.formation.assoue.catalogue.service;

import bf.formation.assoue.catalogue.dto.ProduitCreateDTO;
import bf.formation.assoue.catalogue.dto.ProduitResponseDTO;
import bf.formation.assoue.catalogue.exception.ProduitNotFoundException;
import bf.formation.assoue.catalogue.mapper.ProduitMapper;
import bf.formation.assoue.catalogue.model.Produit;
import bf.formation.assoue.catalogue.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;

    /** BF-SHOP-01 : catalogue complet. */
    public List<ProduitResponseDTO> lister() {
        return produitRepository.findAll().stream().map(produitMapper::toDto).toList();
    }

    public ProduitResponseDTO obtenir(Long id) {
        return produitMapper.toDto(trouver(id));
    }

    /** BF-BO-01. */
    public ProduitResponseDTO creer(ProduitCreateDTO requete) {
        Produit produit = Produit.builder()
                .titre(requete.getTitre())
                .description(requete.getDescription())
                .photoUrl(requete.getPhotoUrl())
                .prixFcfa(requete.getPrixFcfa())
                .disponible(requete.isDisponible())
                .build();
        return produitMapper.toDto(produitRepository.save(produit));
    }

    /** BF-BO-01. */
    public ProduitResponseDTO modifier(Long id, ProduitCreateDTO requete) {
        Produit produit = trouver(id);
        produit.setTitre(requete.getTitre());
        produit.setDescription(requete.getDescription());
        produit.setPhotoUrl(requete.getPhotoUrl());
        produit.setPrixFcfa(requete.getPrixFcfa());
        produit.setDisponible(requete.isDisponible());
        return produitMapper.toDto(produitRepository.save(produit));
    }

    /** BF-BO-01. */
    public void supprimer(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new ProduitNotFoundException(id);
        }
        produitRepository.deleteById(id);
    }

    private Produit trouver(Long id) {
        return produitRepository.findById(id).orElseThrow(() -> new ProduitNotFoundException(id));
    }
}
