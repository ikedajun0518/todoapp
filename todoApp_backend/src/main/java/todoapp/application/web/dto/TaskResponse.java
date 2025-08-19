package todoapp.application.web.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskResponse {
    private Long id;
    private Long goalId;
    private String name;
    private boolean completed;
    private Instant createdAt;
    private Instant updatedAt;
}
