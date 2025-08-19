package todoapp.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import todoapp.application.domain.Goal;
import todoapp.application.domain.Task;
import todoapp.application.repository.GoalRepository;
import todoapp.application.repository.TaskRepository;
import todoapp.application.web.dto.TaskRequest;

@Service
public class TaskService {
    
    private static final long UNASSIGNED_GOAL_ID = 1L;

    private final TaskRepository tasks;
    private final GoalRepository goals;

    public TaskService(TaskRepository tasks, GoalRepository goals) {
        this.tasks = tasks;
        this.goals = goals;
    }

    @Transactional(readOnly = true)
    public Page<Task> listByGoal(Long goalId, Pageable pageable) {
        return tasks.findByGoalId(goalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> listUnassigned(Pageable pageable) {
        return tasks.findByGoalId(UNASSIGNED_GOAL_ID, pageable);
    }

    @Transactional(readOnly = true)
    public Task get(Long taskId) {
        return tasks.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("task not found: " + taskId));
    }

    @Transactional
    public Task createdUnderGoal(Long goalIdFromPath, TaskRequest req) {
        Long goalId = goalIdFromPath != null ? goalIdFromPath :
            (req.getGoalId() != null ? req.getGoalId() : UNASSIGNED_GOAL_ID);
        Goal goal = goals.findById(goalId)
            .orElseThrow(() -> new IllegalArgumentException("goal not found: " + goalId));

        Task t = new Task();
        t.setGoal(goal);
        t.setName(req.getName());
        t.setCompleted(Boolean.TRUE.equals(req.getCompleted()));
        return tasks.save(t);
    }

    @Transactional
    public Task update(Long taskId, TaskRequest req) {
        Task t = get(taskId);
        if (req.getName() != null) t.setName(req.getName());
        if (req.getCompleted() != null) t.setCompleted(req.getCompleted());
        if (req.getGoalId() != null) {
            Goal g = goals.findById(req.getGoalId())
                .orElseThrow(() -> new IllegalArgumentException("goal not found: " + req.getGoalId()));
        }
        return tasks.save(t);
    }

    @Transactional
    public void delete(Long taskId) {
        Task t = get(taskId);
        tasks.delete(t);
    }

}
