package bf.formation.assoue.auth.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Convertisseur JPA transparent : chiffre en base (AES-256-GCM), dechiffre a
 * la lecture. Applique aux colonnes contenant des donnees personnelles
 * (nom, telephone -- CDC section 3B, exigence CIL Burkina Faso).
 *
 * Un AttributeConverter est instancie par Hibernate, PAS par Spring, donc
 * @Value ne peut pas etre injecte directement dessus. CleHolder (un
 * @Component Spring classique) recupere la cle au demarrage et la pousse
 * dans un champ static -- pattern standard pour ce cas precis.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile String cle;

    @Component
    static class CleHolder {
        CleHolder(@Value("${data.encryption-key}") String cleConfiguree) {
            EncryptedStringConverter.cle = cleConfiguree;
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        verifierCleChargee();
        return AesGcmUtil.chiffrer(attribute, cle);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        verifierCleChargee();
        return AesGcmUtil.dechiffrer(dbData, cle);
    }

    private void verifierCleChargee() {
        if (cle == null) {
            throw new IllegalStateException(
                    "data.encryption-key n'a pas ete chargee -- CleHolder doit etre initialise avant tout acces JPA");
        }
    }
}
