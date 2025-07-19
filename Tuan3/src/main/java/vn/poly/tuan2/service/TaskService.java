package vn.poly.tuan2.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    public Task createTask(Task task) {
        if (task.getAssignedTo() != null && task.getAssignedTo().getId() != null) {
            User assignedUser = userRepository.findById(task.getAssignedTo().getId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + task.getAssignedTo().getId()));
            task.setAssignedTo(assignedUser);
        }
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public Task updateTask(Long id, Task taskDetails) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found with id " + id));
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setDueDate(taskDetails.getDueDate());
        task.setStatus(taskDetails.getStatus());

        if (taskDetails.getAssignedTo() != null && taskDetails.getAssignedTo().getId() != null) {
            User assignedUser = userRepository.findById(taskDetails.getAssignedTo().getId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found with ID: " + taskDetails.getAssignedTo().getId()));
            task.setAssignedTo(assignedUser);
        } else {
            task.setAssignedTo(null);
        }
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findByAssignedToId(userId);
    }
}
