package todoapp.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import todoapp.application.domain.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    
}
