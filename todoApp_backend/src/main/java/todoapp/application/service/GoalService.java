package todoapp.application.service;



import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import todoapp.application.domain.Goal;
import todoapp.application.domain.Task;
import todoapp.application.repository.GoalRepository;
import todoapp.application.repository.TaskRepository;
import todoapp.application.web.dto.GoalRequest;
import todoapp.application.web.dto.TaskRequest;

@Service
public class GoalService {
    private final GoalRepository goals;
    private final TaskRepository tasks;

    public GoalService(GoalRepository goals, TaskRepository tasks) {
        this.goals = goals;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public Page<Goal> list(Pageable pageable) {
        return goals.findAll(pageable); 
    }

    @Transactional(readOnly = true)
    public Goal get(Long id) {
        return goals.findById(id).orElseThrow(() -> new IllegalArgumentException("goal not found: " + id));
    }

    @Transactional
    public Goal create(GoalRequest req) {
        Goal g = new Goal();
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        Goal saved = goals.save(g);

        for (TaskRequest t : req.getTasks()) {
            Task e = new Task();
            e.setGoal(saved);
            e.setName(t.getName().trim());
            e.setCompleted(Boolean.TRUE.equals(t.getCompleted()));
            tasks.save(e);
        }

        return saved;
    }

    @Transactional
    public Goal update(Long id, GoalRequest req) {
        Goal g = new Goal();
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        Goal saved = goals.save(g);

        Map<Long, Task> existing = tasks.findByGoalId(id, Pageable.unpaged())
            .getContent().stream().collect(Collectors.toMap(Task::getId, x -> x));
        Set<Long> seen = new HashSet<>();

        for (TaskRequest t: req.getTasks()) {
            if (t.getId() != null && existing.containsKey(t.getId())) {
                Task e = existing.get(t.getId());
                e.setName(t.getName().trim());
                e.setCompleted(Boolean.TRUE.equals(t.getCompleted()));
                tasks.save(e);
                seen.add(t.getId());
            } else {
                Task e = new Task();
                e.setGoal(saved);
                e.setName(t.getName().trim());
                e.setCompleted(Boolean.TRUE.equals(t.getCompleted()));
                tasks.save(e);
            }
        }

        for (Task e : existing.values()) {
            if (!seen.contains(e.getId())) tasks.delete(e);
        }

        long count = tasks.findByGoalId(id, Pageable.unpaged()).getTotalElements();
        if ( count < 1 ) {
            throw new IllegalStateException("タスクは1件以上必要です。");
        }
        return saved;
    }

    @Transactional
    public List<Task> listTasks(Long goalId) {
        return tasks.findByGoalId(goalId, Pageable.unpaged()).getContent();
    }

    @Transactional
    public void delete(Long id) {
        Goal g = get(id);
        if (g.isDeletionProtected()) {
            throw new IllegalStateException("この目標は削除できません。");
        }
        goals.delete(g);
    }
}
