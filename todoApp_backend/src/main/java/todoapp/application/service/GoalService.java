package todoapp.application.service;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import todoapp.application.domain.Goal;
import todoapp.application.repository.GoalRepository;
import todoapp.application.web.dto.GoalRequest;

@Service
public class GoalService {
    private final GoalRepository goals;

    public GoalService(GoalRepository goals) {
        this.goals = goals;
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
        g.setName(req.getGoal());
        g.setDescription(req.getDescription());
        return goals.save(g);
    }

    @Transactional
    public Goal update(Long id, GoalRequest req) {
        Goal g = new Goal();
        g.setName(req.getGoal());
        g.setDescription(req.getDescription());
        return goals.save(g);
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
