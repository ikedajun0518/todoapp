package todoapp.application.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import todoapp.application.domain.Task;


public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByGoalId(Long goalId, Pageable pageable);

    List<Task> findByGoalIdOrderByIdAsc(Long goalId);
}
