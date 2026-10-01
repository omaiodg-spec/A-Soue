package bf.formation.assoue.notification.controller;

import bf.formation.assoue.notification.repository.NotificationLogRepository;
import bf.formation.assoue.common.security.JwtTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationLogRepository notificationLogRepository;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
    }

    @Test
    void listerNotifications_shouldReturn401or403_whenNoToken() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void listerNotifications_shouldReturn403_whenTokenIsCitoyen() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listerNotifications_shouldReturn200_whenTokenIsAdmin() throws Exception {
        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + JwtTestUtils.token(1L, "ADMIN")))
                .andExpect(status().isOk());
    }
}
