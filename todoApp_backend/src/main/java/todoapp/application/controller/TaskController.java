package todoapp.application.controller;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import todoapp.application.domain.Task;
import todoapp.application.service.TaskService;
import todoapp.application.web.dto.TaskRequest;
import todoapp.application.web.dto.TaskResponse;

@RestController
@RequestMapping("/api")
@Validated
public class TaskController {
    
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/goals/{goalId}/tasks")
    public Page<TaskResponse> listByGoal(
        @PathVariable Long goalId,
        @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable ) {
            return  service.listByGoal(goalId, pageable).map(this::toResponse);
    }

    @GetMapping("/tasks/unassigned")
    public Page<TaskResponse> listUnassigned(
        @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable ) {
            return service.listUnassigned(pageable).map(this::toResponse);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@PathVariable Long taskId) {
        return toResponse(service.get(taskId));
    }

    @PostMapping("/goals/{goalId}/tasks")
    public ResponseEntity<TaskResponse> createUnderGoal(
        @PathVariable Long goalId, @Valid @RequestBody TaskRequest req ) {
            Task saved = service.createdUnderGoal(goalId, req);
            return makeLocation(saved);
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest req) {
        Task saved = service.createdUnderGoal(null, req);
        return makeLocation(saved);
    }

    private ResponseEntity<TaskResponse> makeLocation(Task saved) {
         URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody TaskRequest req) {
        return toResponse(service.update(taskId, req));
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        service.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    private TaskResponse toResponse(Task t) {
        TaskResponse r = new TaskResponse();
        r.setId(t.getId());
        r.setGoalId(t.getGoal().getId());
        r.setName(t.getName());
        r.setCompleted(t.isCompleted());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }
}
