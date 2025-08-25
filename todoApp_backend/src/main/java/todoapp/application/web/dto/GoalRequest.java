package todoapp.application.web.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRequest {
    @Size(max = 200, message = "{name.max200}")
    private String name;

    @Size(max = 1000, message = "{desc.max1000}")
    private String description;

    @NotEmpty(message = "{task.min1}")
    private List<@Valid TaskRequest> tasks;
}
