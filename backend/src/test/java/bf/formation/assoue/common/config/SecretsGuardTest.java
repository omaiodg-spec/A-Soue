package bf.formation.assoue.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretsGuardTest {

    private static final String SECRET_FORT = "k8Vq2mZx0Rj7TnHs4YbLw9PcDa6EuFgX1oIe5Ct3";

    private SecretsGuard guard(String jwt, String cle, String webhook, String motDePasseAdmin, boolean strict) {
        SecretsGuard guard = new SecretsGuard();
        ReflectionTestUtils.setField(guard, "jwtSecret", jwt);
        ReflectionTestUtils.setField(guard, "cleChiffrement", cle);
        ReflectionTestUtils.setField(guard, "secretWebhook", webhook);
        ReflectionTestUtils.setField(guard, "motDePasseAdmin", motDePasseAdmin);
        ReflectionTestUtils.setField(guard, "strict", strict);
        return guard;
    }

    @Test
    void verifier_shouldPass_whenSecretsFortsEnModeStrict() {
        SecretsGuard guard = guard(SECRET_FORT, SECRET_FORT, SECRET_FORT, "Mot-de-passe-Admin-2026", true);

        assertThatCode(guard::verifier).doesNotThrowAnyException();
    }

    @Test
    void verifier_shouldThrow_whenValeurExempleEnModeStrict() {
        SecretsGuard guard = guard("CHANGE_MOI_secret_partage_assoue_store_2026_min_32_car",
                SECRET_FORT, SECRET_FORT, "Mot-de-passe-Admin-2026", true);

        assertThatThrownBy(guard::verifier)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void verifier_shouldThrow_whenMotDePasseAdminParDefautEnModeStrict() {
        SecretsGuard guard = guard(SECRET_FORT, SECRET_FORT, SECRET_FORT, "admin123", true);

        assertThatThrownBy(guard::verifier)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_DEFAULT_PASSWORD");
    }

    @Test
    void verifier_shouldOnlyWarn_whenModeNonStrict() {
        SecretsGuard guard = guard("court", "court", "court", "admin123", false);

        assertThatCode(guard::verifier).doesNotThrowAnyException();
    }
}
