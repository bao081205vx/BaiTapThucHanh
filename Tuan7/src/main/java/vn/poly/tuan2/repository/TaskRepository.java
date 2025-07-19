package vn.poly.tuan2.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus; // Import TaskStatus

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Page<Task> findByAssignedToId(Long assignedToId, Pageable pageable);
    Page<Task> findByAssignedToIdAndStatus(Long assignedToId, TaskStatus status, Pageable pageable);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}