package todoapp.application.controller;

import java.net.URI;

import javax.validation.Valid;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import todoapp.application.domain.Goal;
import todoapp.application.service.GoalService;
import todoapp.application.web.dto.GoalRequest;
import todoapp.application.web.dto.GoalResponse;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/api/goals")
@Validated
public class GoalController {
    
    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping
    public Page<GoalResponse> list(
        @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
            return service.list(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable Long id) {
        return toResponse(service.get(id));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest req) {
        Goal saved = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(saved));
    }
    
    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @Valid @RequestBody GoalRequest req) {
        return toResponse(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private GoalResponse toResponse(Goal g) {
        GoalResponse r = new GoalResponse();
        r.setId(g.getId());
        r.setGoal(g.getName());
        r.setDescription(g.getDescription());
        r.setDeletionProtected(g.isDeletionProtected());
        r.setCreatedAt(g.getCreatedAt());
        r.setUpdatedAt(g.getUpdatedAt());
        return r;
    }
}
