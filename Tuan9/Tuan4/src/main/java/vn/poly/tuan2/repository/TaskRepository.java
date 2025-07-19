package vn.poly.tuan2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.poly.tuan2.entity.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToId(Long userId);
}
