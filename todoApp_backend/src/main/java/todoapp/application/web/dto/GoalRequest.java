package todoapp.application.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRequest {
    @NotBlank
    @Size(max = 200)
    private String goal;

    @Size(max = 10_000)
    private String description;
}
