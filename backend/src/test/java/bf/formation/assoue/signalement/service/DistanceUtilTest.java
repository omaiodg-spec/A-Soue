package bf.formation.assoue.signalement.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class DistanceUtilTest {

    @Test
    void distanceKm_shouldBeZero_forSamePoint() {
        double distance = DistanceUtil.distanceKm(12.3714, -1.5197, 12.3714, -1.5197);
        assertThat(distance).isEqualTo(0.0, offset(0.001));
    }

    @Test
    void distanceKm_shouldMatchKnownDistance_ouagadougouToBoboDioulasso() {
        // Ouagadougou (12.3714, -1.5197) -> Bobo-Dioulasso (11.1771, -4.2979) : ~330.29 km a vol d'oiseau.
        double distance = DistanceUtil.distanceKm(12.3714, -1.5197, 11.1771, -4.2979);
        assertThat(distance).isCloseTo(330.29, offset(0.5));
    }

    @Test
    void distanceKm_shouldBeSymmetric() {
        double d1 = DistanceUtil.distanceKm(12.37, -1.52, 11.17, -4.30);
        double d2 = DistanceUtil.distanceKm(11.17, -4.30, 12.37, -1.52);
        assertThat(d1).isEqualTo(d2, offset(0.0001));
    }
}
