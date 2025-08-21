package todoapp.application.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {

    private Long id;

    @NotBlank(message = "{task.required}")
    @Size(max = 200, message = "{name.max200}")
    private String name;

    private Boolean completed;

    private Long goalId;
}
