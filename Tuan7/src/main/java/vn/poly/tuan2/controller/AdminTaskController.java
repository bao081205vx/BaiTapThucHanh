package vn.poly.tuan2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.poly.tuan2.dto.TaskDto;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus;
import vn.poly.tuan2.response.ApiResponse;
import vn.poly.tuan2.service.TaskService;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/all")
    public ResponseEntity<Page<TaskDto>> getAllTasksForAdmin(
                                                              @RequestParam(required = false) TaskStatus status,
                                                              @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getAllTasksForAdmin(status, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TaskDto>> getTasksByUserIdForAdmin(
                                                                   @PathVariable Long userId,
                                                                   @RequestParam(required = false) TaskStatus status,
                                                                   @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getTasksByUserIdForAdmin(userId, status, pageable);
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateTaskByAdmin(@PathVariable Long id, @RequestBody Task taskDetails) {
        Task updatedTask = taskService.updateTaskByAdmin(id, taskDetails);
        return ResponseEntity.ok(new ApiResponse(true, "Task updated by admin successfully!", updatedTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTaskByAdmin(@PathVariable Long id) {
        taskService.deleteTaskByAdmin(id);
        return ResponseEntity.ok(new ApiResponse(true, "Task deleted by admin successfully!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getTaskByIdForAdmin(@PathVariable Long id) {
        Optional<Task> task = taskService.getTaskById(id);
        if (task.isPresent()) {
            return ResponseEntity.ok(new ApiResponse(true, "Task found!", taskService.convertToDto(task.get()))); // Chuyển đổi sang TaskDto
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}