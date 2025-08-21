package todoapp.application.web.dto;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompletedUpdateRequest {
    @NotNull
    private Boolean completed;
}
