package bf.formation.assoue.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private String telephone;
    private String type;
    private boolean envoyeAvecSucces;
    private LocalDateTime dateEnvoi;
}
