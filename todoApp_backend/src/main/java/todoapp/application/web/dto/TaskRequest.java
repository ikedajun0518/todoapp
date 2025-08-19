package todoapp.application.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    private Boolean completed;

    private Long goalId;
}
