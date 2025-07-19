package vn.poly.tuan2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.repository.TaskRepository;
import vn.poly.tuan2.repository.UserRepository;

import java.util.List;
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
            User currentUser = userRepository.findByUsername(username);
            if (currentUser != null) {
                return currentUser.getId();
            }
        }
        throw new RuntimeException("User not authenticated or user ID not found.");
    }

    public Task createTask(Task task) {
        Long currentUserId = getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found with ID: " + currentUserId));
        task.setAssignedTo(currentUser);
        return taskRepository.save(task);
    }

    public List<Task> getAllTasksForCurrentUser() {
        Long currentUserId = getCurrentUserId();
        return taskRepository.findByAssignedToId(currentUserId);
    }

    public Optional<Task> getTaskByIdForCurrentUser(Long id) {
        Long currentUserId = getCurrentUserId();
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent() && !task.get().getAssignedTo().getId().equals(currentUserId)) {
            throw new RuntimeException("Access Denied: You do not own this task.");
        }
        return task;
    }

    public Task updateTaskForCurrentUser(Long id, Task taskDetails) {
        Long currentUserId = getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id " + id));

        if (!task.getAssignedTo().getId().equals(currentUserId)) {
            throw new RuntimeException("Access Denied: You do not own this task.");
        }

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());

        if (taskDetails.getAssignedTo() != null && taskDetails.getAssignedTo().getId() != null) {
            if (!taskDetails.getAssignedTo().getId().equals(currentUserId)) {
                throw new RuntimeException("You can only assign tasks to yourself.");
            }
            User assignedUser = userRepository.findById(taskDetails.getAssignedTo().getId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + taskDetails.getAssignedTo().getId()));
            task.setAssignedTo(assignedUser);
        } else {
            task.setAssignedTo(null);
        }
        return taskRepository.save(task);
    }

    public void deleteTaskForCurrentUser(Long id) {
        Long currentUserId = getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id " + id));

        if (!task.getAssignedTo().getId().equals(currentUserId)) {
            throw new RuntimeException("Truy cập bị từ chối");
        }
        taskRepository.deleteById(id);
    }
    // public List<Task> getTasksByUserId(Long userId) {
    //     return taskRepository.findByAssignedToId(userId);
    // }
}