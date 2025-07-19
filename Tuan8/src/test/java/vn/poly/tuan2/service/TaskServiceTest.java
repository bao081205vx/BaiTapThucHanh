package vn.poly.tuan2.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import vn.poly.tuan2.dto.TaskDto;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.exception.ResourceNotFoundException;
import vn.poly.tuan2.repository.TaskRepository;
import vn.poly.tuan2.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private TaskDto testTaskDto;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(Collections.emptySet())
                .build();
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Description for test task")
                .status(TaskStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .assignedTo(testUser)
                .build();
        testTaskDto = TaskDto.builder()
                .id(testTask.getId())
                .title(testTask.getTitle())
                .description(testTask.getDescription())
                .status(testTask.getStatus())
                .dueDate(testTask.getDueDate())
                .createdAt(testTask.getCreatedAt())
                .assignedToId(testUser.getId())
                .assignedToUsername(testUser.getUsername())
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }
    private void setupSecurityContext(Long userId, String username) {
        User currentUser = User.builder().id(userId).username(username).build();
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn(mock(UserDetails.class));
        when(((UserDetails) authentication.getPrincipal()).getUsername()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
    }
    @Test
    void testCreateTask() {
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        Task createdTask = taskService.createTask(testTask);
        assertNotNull(createdTask);
        assertEquals(testTask.getTitle(), createdTask.getTitle());
        assertEquals(testUser.getId(), createdTask.getAssignedTo().getId());
        assertEquals(TaskStatus.PENDING, createdTask.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testGetTasksForCurrentUser_NoStatus() {
        setupSecurityContext(testUser.getId(), testUser.getUsername());
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findByAssignedToId(testUser.getId(), pageable)).thenReturn(taskPage);
        Page<TaskDto> result = taskService.getTasksForCurrentUser(null, pageable);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        verify(userRepository, times(1)).findByUsername(testUser.getUsername());
        verify(taskRepository, times(1)).findByAssignedToId(testUser.getId(), pageable);
    }
    @Test
    void testGetTasksForCurrentUser_WithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findByAssignedToIdAndStatus(eq(testUser.getId()), eq(TaskStatus.PENDING), eq(pageable)))
                .thenReturn(taskPage);
        Page<TaskDto> result = taskService.getTasksForCurrentUser(TaskStatus.PENDING, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        // Verify gọi phương thức cụ thể của repository
        verify(taskRepository, times(1)).findByAssignedToIdAndStatus(eq(testUser.getId()), eq(TaskStatus.PENDING), eq(pageable));
    }
    @Test
    void testGetTasksForCurrentUser_InvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(IllegalArgumentException.class, () -> taskService.getTasksForCurrentUser(TaskStatus.valueOf("INVALID_STATUS"), pageable));
        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        verify(taskRepository, never()).findByAssignedToIdAndStatus(anyLong(), any(TaskStatus.class), any(Pageable.class));
    }


    @Test
    void testGetTaskByIdForCurrentUser() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        Optional<TaskDto> result = taskService.getTaskByIdForCurrentUser(1L);
        assertTrue(result.isPresent());
        assertEquals(testTaskDto.getTitle(), result.get().getTitle());
        assertEquals(testTaskDto.getId(), result.get().getId());
        assertEquals(testTaskDto.getAssignedToId(), result.get().getAssignedToId());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void testGetTaskByIdForCurrentUser_NotFound() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskByIdForCurrentUser(99L));
        verify(taskRepository, times(1)).findById(99L);
    }

    @Test
    void testGetTaskByIdForCurrentUser_AccessDenied() {
        User otherUser = User.builder().id(2L).username("otheruser").build();
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other User Task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .assignedTo(otherUser)
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskByIdForCurrentUser(2L));
        verify(taskRepository, times(1)).findById(2L);
    }

    @Test
    void testUpdateTaskForCurrentUser() {
        Task updatedDetails = Task.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(TaskStatus.COMPLETED)
                .dueDate(LocalDate.now().plusDays(10))
                .assignedTo(testUser)
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedDetails);
        Task result = taskService.updateTaskForCurrentUser(1L, updatedDetails);
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    @Test
    void testUpdateTaskForCurrentUser_AccessDenied() {
        User otherUser = User.builder().id(2L).username("otheruser").build();
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other User Task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .assignedTo(otherUser)
                .build();
        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));
        Task updatedDetails = Task.builder().title("Updated Title").build();
        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTaskForCurrentUser(2L, updatedDetails));
        verify(taskRepository, times(1)).findById(2L);
        verify(taskRepository, never()).save(any(Task.class));
    }
    @Test
    void testDeleteTaskForCurrentUser() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).deleteById(1L);
        taskService.deleteTaskForCurrentUser(1L);
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }
    @Test
    void testDeleteTaskForCurrentUser_AccessDenied() {
        User otherUser = User.builder().id(2L).username("otheruser").build();
        Task otherUserTask = Task.builder()
                .id(2L)
                .title("Other User Task")
                .description("Description")
                .status(TaskStatus.PENDING)
                .assignedTo(otherUser)
                .build();
        when(taskRepository.findById(2L)).thenReturn(Optional.of(otherUserTask));
        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTaskForCurrentUser(2L));
        verify(taskRepository, times(1)).findById(2L);
        verify(taskRepository, never()).deleteById(anyLong());
    }
    @Test
    void testGetAllTasksForAdmin_NoStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
        Page<TaskDto> result = taskService.getAllTasksForAdmin(null, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }
    @Test
    void testGetAllTasksForAdmin_WithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);
        Page<TaskDto> result = taskService.getAllTasksForAdmin(TaskStatus.PENDING, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        verify(taskRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }
    @Test
    void testGetTasksByUserIdForAdmin_NoStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.findByAssignedToId(eq(1L), eq(pageable))).thenReturn(taskPage);
        Page<TaskDto> result = taskService.getTasksByUserIdForAdmin(1L, null, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).findByAssignedToId(eq(1L), eq(pageable));
    }
    @Test
    void testGetTasksByUserIdForAdmin_WithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = Arrays.asList(testTask);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.findByAssignedToIdAndStatus(eq(1L), eq(TaskStatus.PENDING), eq(pageable))).thenReturn(taskPage);
        Page<TaskDto> result = taskService.getTasksByUserIdForAdmin(1L, TaskStatus.PENDING, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTaskDto.getTitle(), result.getContent().get(0).getTitle());
        verify(userRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).findByAssignedToIdAndStatus(eq(1L), eq(TaskStatus.PENDING), eq(pageable));
    }
    @Test
    void testGetTaskByIdForAdmin() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        Optional<TaskDto> result = taskService.getTaskByIdForAdmin(1L);
        assertTrue(result.isPresent());
        assertEquals(testTaskDto.getTitle(), result.get().getTitle());
        verify(taskRepository, times(1)).findById(1L);
    }
    @Test
    void testGetTaskByIdForAdmin_NotFound() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskByIdForAdmin(99L));
        verify(taskRepository, times(1)).findById(99L);
    }
    @Test
    void testUpdateTaskByAdmin() {
        Task updatedDetails = Task.builder()
                .title("Admin Updated Title")
                .description("Admin Updated Description")
                .status(TaskStatus.COMPLETED)
                .dueDate(LocalDate.now().plusDays(10))
                .assignedTo(testUser)
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedDetails);
        Task result = taskService.updateTaskByAdmin(1L, updatedDetails);
        assertNotNull(result);
        assertEquals("Admin Updated Title", result.getTitle());
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(userRepository, times(1)).findById(testUser.getId());
    }
    @Test
    void testDeleteTaskByAdmin() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(1L);
        taskService.deleteTaskByAdmin(1L);
        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }
    @Test
    void testDeleteTaskByAdmin_NotFound() {
        when(taskRepository.existsById(anyLong())).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTaskByAdmin(99L));
        verify(taskRepository, times(1)).existsById(99L);
        verify(taskRepository, never()).deleteById(anyLong());
    }
}
