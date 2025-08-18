package todoapp.application.web.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalResponse {
    private Long id;
    private String goal;
    private String description;
    private boolean deletionProtected;
    private Instant createdAt;
    private Instant updatedAt;
}
