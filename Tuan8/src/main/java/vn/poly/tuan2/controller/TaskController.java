package vn.poly.tuan2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.poly.tuan2.dto.TaskDto;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus;
import vn.poly.tuan2.response.ApiResponse;

import jakarta.validation.Valid;
import vn.poly.tuan2.service.TaskService;

import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> createTask(@Valid @RequestBody Task task) {
        Task createdTask = taskService.createTask(task);
        return new ResponseEntity<>(new ApiResponse(true, "Task created successfully!", createdTask), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TaskDto>> getTasksForCurrentUser(
                                                                 @RequestParam(required = false) TaskStatus status,
                                                                 @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getTasksForCurrentUser(status, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> getTaskById(@PathVariable Long id) {
        Optional<TaskDto> task = taskService.getTaskByIdForCurrentUser(id);
        if (task.isPresent()) {
            return ResponseEntity.ok(new ApiResponse(true, "Task found!", task.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> updateTask(@PathVariable Long id, @Valid @RequestBody Task taskDetails) {
        Task updatedTask = taskService.updateTaskForCurrentUser(id, taskDetails);
        return ResponseEntity.ok(new ApiResponse(true, "Task updated successfully!", updatedTask));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse> deleteTask(@PathVariable Long id) {
        taskService.deleteTaskForCurrentUser(id);
        return ResponseEntity.ok(new ApiResponse(true, "Task deleted successfully!"));
    }
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TaskDto>> getAllTasksByAdmin(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getAllTasksByAdmin(status, pageable);
        return ResponseEntity.ok(tasks);
    }
}