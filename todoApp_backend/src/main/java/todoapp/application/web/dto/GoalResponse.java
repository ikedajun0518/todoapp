package todoapp.application.web.dto;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalResponse {
    private Long id;
    private String name;
    private String description;
    private boolean deletionProtected;
    private Instant createdAt;
    private Instant updatedAt;

    public static class TaskSummary {
        public Long id;
        public String name;
        public boolean completed;
        public Instant createdAt;
        public Instant updatedAt;
    }

    private List<TaskSummary> tasks;
}
