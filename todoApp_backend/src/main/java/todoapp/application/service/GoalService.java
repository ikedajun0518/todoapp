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
import todoapp.application.service.search.SolrIndexService;
import todoapp.application.web.dto.GoalRequest;
import todoapp.application.web.dto.TaskRequest;

@Service
public class GoalService {
    private final GoalRepository goals;
    private final TaskRepository tasks;
    private final EntityManager em;
    private final SolrIndexService solr;

    public GoalService(GoalRepository goals, TaskRepository tasks, EntityManager em, SolrIndexService solr) { 
        this.goals = goals;
        this.tasks = tasks;
        this.em = em;
        this.solr = solr;
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
            throw new IllegalStateException("At least one task is required.");
        }
        Predicate<String> blank = s -> (s == null || s.trim().isEmpty());
        boolean nameBlank = blank.test(req.getName());
        boolean descBlank = blank.test(req.getDescription());

        if (nameBlank && descBlank) {
            final Long UNASSIGNED_GOAL_ID = 1L;
            Goal unassigned = goals.findById(UNASSIGNED_GOAL_ID)
                .orElseThrow(() -> new IllegalStateException("Unassigned goal (id = 1) does not exists."));
            
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
            
            Goal reloaded = goals.findById(unassigned.getId())
                .orElseThrow(() -> new IllegalStateException("Initial goal not found."));
            solr.indexGoalWithTasks(reloaded, tasks.findByGoalIdOrderByIdAsc(unassigned.getId()));
            return reloaded;
        }

        if (!descBlank && nameBlank) {
            throw new IllegalArgumentException("Goal name is required when description is provided.");
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

        solr.indexGoalWithTasks(saved, tasks.findByGoalIdOrderByIdAsc(saved.getId()));
        return saved;
    }

    @Transactional
    public Goal update(Long id, GoalRequest req) {
        Goal g = goals.findById(id).orElseThrow(() -> new IllegalArgumentException("goal not found: " + id));
        if (req.getName() != null) g.setName(req.getName());
        if (req.getDescription() != null) g.setDescription(req.getDescription());

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
            throw new IllegalStateException("At least one task is required.");
        }
        goals.touch(id);
        em.flush();
        em.clear();
        Goal reloaded = goals.findById(id).orElseThrow(() -> new IllegalArgumentException("goal not found: " + id));
        solr.indexGoalWithTasks(reloaded, tasks.findByGoalIdOrderByIdAsc(id));
        return reloaded;
    }

    @Transactional
    public List<Task> listTasks(Long goalId) {
        return tasks.findByGoalIdOrderByIdAsc(goalId);
    }

    @Transactional
    public void delete(Long id) {
        Goal g = get(id);
        if (g.isDeletionProtected()) {
            throw new IllegalStateException("This goal cannot be deleted.");
        }
        solr.deleteByGoalId(id);
        goals.delete(g);
    }
}
