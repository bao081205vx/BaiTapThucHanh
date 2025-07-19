package vn.poly.tuan2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.poly.tuan2.dto.TaskDto;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.exception.ResourceNotFoundException;
import vn.poly.tuan2.repository.TaskRepository;
import vn.poly.tuan2.repository.UserRepository;

import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found with username: " + username));
            return currentUser.getId();
        }
        throw new RuntimeException("User not authenticated or user ID not found.");
    }
    public TaskDto convertToDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToUsername(task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : null)
                .build();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task createTask(Task task) {
        Long currentUserId = getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found with ID: " + currentUserId));
        task.setAssignedTo(currentUser);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        return taskRepository.save(task);
    }
    public Page<TaskDto> getTasksForCurrentUser(TaskStatus status, Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        Page<Task> tasksPage;
        if (status != null) {
            tasksPage = taskRepository.findByAssignedToIdAndStatus(currentUserId, status, pageable);
        } else {
            tasksPage = taskRepository.findByAssignedToId(currentUserId, pageable);
        }
        return tasksPage.map(this::convertToDto);
    }

    public Optional<TaskDto> getTaskByIdForCurrentUser(Long id) {
        Long currentUserId = getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        if (!task.getAssignedTo().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Access Denied: You do not own this task.");
        }
        return Optional.of(convertToDto(task));
    }

    public Task updateTaskForCurrentUser(Long id, Task taskDetails) {
        Long currentUserId = getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        if (!task.getAssignedTo().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Access Denied: You do not own this task.");
        }

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());

        if (taskDetails.getAssignedTo() != null && taskDetails.getAssignedTo().getId() != null) {
            if (!taskDetails.getAssignedTo().getId().equals(currentUserId)) {
                throw new ResourceNotFoundException("You can only assign tasks to yourself or task not found.");
            }
            User assignedUser = userRepository.findById(taskDetails.getAssignedTo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found with ID: " + taskDetails.getAssignedTo().getId()));
            task.setAssignedTo(assignedUser);
        }
        return taskRepository.save(task);
    }

    public void deleteTaskForCurrentUser(Long id) {
        Long currentUserId = getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        if (!task.getAssignedTo().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Access Denied: You do not own this task.");
        }
        taskRepository.deleteById(id);
    }
    public Page<TaskDto> getAllTasksForAdmin(TaskStatus status, Pageable pageable) {
        Page<Task> tasksPage;
        if (status != null) {
            tasksPage = taskRepository.findByStatus(status, pageable);
        } else {
            tasksPage = taskRepository.findAll(pageable);
        }
        return tasksPage.map(this::convertToDto);
    }
    public Page<TaskDto> getTasksByUserIdForAdmin(Long userId, TaskStatus status, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Page<Task> tasksPage;
        if (status != null) {
            tasksPage = taskRepository.findByAssignedToIdAndStatus(userId, status, pageable);
        } else {
            tasksPage = taskRepository.findByAssignedToId(userId, pageable);
        }
        return tasksPage.map(this::convertToDto);
    }

    public Task updateTaskByAdmin(Long id, Task taskDetails) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        existingTask.setTitle(taskDetails.getTitle());
        existingTask.setDescription(taskDetails.getDescription());
        existingTask.setDueDate(taskDetails.getDueDate());
        existingTask.setStatus(taskDetails.getStatus());

        if (taskDetails.getAssignedTo() != null && taskDetails.getAssignedTo().getId() != null) {
            User assignedUser = userRepository.findById(taskDetails.getAssignedTo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found with ID: " + taskDetails.getAssignedTo().getId()));
            existingTask.setAssignedTo(assignedUser);
        } else {
            existingTask.setAssignedTo(null);
        }
        return taskRepository.save(existingTask);
    }

    public void deleteTaskByAdmin(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }
        taskRepository.deleteById(id);
    }
}