package todoapp.application.service;



import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

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
    private final EntityManager em;

    public GoalService(GoalRepository goals, TaskRepository tasks, EntityManager em) {
        this.goals = goals;
        this.tasks = tasks;
        this.em = em;
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
        List<TaskRequest> incoming = req.getTasks() != null ? req.getTasks() : Collections.emptyList();
        if (incoming.isEmpty()) {
            throw new IllegalStateException("タスクは1件以上必要です。");
        }
        Predicate<String> blank = s -> (s == null || s.trim().isEmpty());
        boolean nameBlank = blank.test(req.getName());
        boolean descBlank = blank.test(req.getDescription());

        if (nameBlank && descBlank) {
            final Long UNASSIGNED_GOAL_ID = 1L;
            Goal unassigned = goals.findById(UNASSIGNED_GOAL_ID)
                .orElseThrow(() -> new IllegalStateException("目標未設定(id = 1)が存在しません。"));
            
            for (TaskRequest t : incoming) {
                Task e = new Task();
                e.setGoal(unassigned);
                e.setName(t.getName().trim());
                e.setCompleted(Boolean.TRUE.equals(t.getCompleted()));
                tasks.save(e);
            }

            goals.touch(unassigned.getId());
            em.flush();
            em.clear();
            return goals.findById(unassigned.getId()).orElseThrow(() -> new IllegalStateException("初期目標が見つかりません。"));
        }

        if (!descBlank && nameBlank) {
            throw new IllegalArgumentException("説明を入力する場合は目標名が必要です。");
        }

        Goal g = new Goal();
        g.setName(req.getName().trim());
        g.setDescription(descBlank ? null : req.getDescription().trim());
        Goal saved = goals.save(g);
        for (TaskRequest t : incoming) {
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
        Goal g = goals.findById(id).orElseThrow(() -> new IllegalArgumentException("goal not found: " + id));
        if (req.getName() != null) g.setName(req.getName());
        if (req.getDescription() != null) g.setDescription(req.getDescription());
        g.setDescription(req.getDescription());

        Map<Long, Task> existing = tasks.findByGoalId(id, Pageable.unpaged())
            .getContent().stream().collect(Collectors.toMap(Task::getId, x -> x));
        Set<Long> seen = new HashSet<>();

        List<TaskRequest> incoming = req.getTasks() != null ? req.getTasks() : Collections.emptyList();
        for (TaskRequest t: incoming) {
            if (t.getId() != null && existing.containsKey(t.getId())) {
                Task e = existing.get(t.getId());
                e.setName(t.getName().trim());
                e.setCompleted(Boolean.TRUE.equals(t.getCompleted()));
                tasks.save(e);
                seen.add(t.getId());
            } else {
                Task e = new Task();
                e.setGoal(g);
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
        goals.touch(id);
        em.flush();
        em.clear();
        return goals.findById(id).orElseThrow(() -> new IllegalArgumentException("goal not found: " + id));
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
