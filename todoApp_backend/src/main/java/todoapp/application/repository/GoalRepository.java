package todoapp.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import todoapp.application.domain.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    @Modifying
    @Query("update Goal g set g.updatedAt = CURRENT_TIMESTAMP where g.id = :id")
    int touch(@Param("id") Long id);
}
