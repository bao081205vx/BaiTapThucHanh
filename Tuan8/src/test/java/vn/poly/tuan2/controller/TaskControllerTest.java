package vn.poly.tuan2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import vn.poly.tuan2.AppConfig.SecurityConfig;
import vn.poly.tuan2.dto.TaskDto;
import vn.poly.tuan2.entity.Task;
import vn.poly.tuan2.entity.TaskStatus;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.exception.ResourceNotFoundException;
import vn.poly.tuan2.security.JwtUtils;
import vn.poly.tuan2.service.TaskService;
import vn.poly.tuan2.service.UserDetailsServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class})
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;


    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Task testTask;
    private TaskDto testTaskDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
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
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testCreateTask() throws Exception {
        when(taskService.createTask(any(Task.class))).thenReturn(testTask);
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTask)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task created successfully!"))
                .andExpect(jsonPath("$.data.id").value(testTask.getId()))
                .andExpect(jsonPath("$.data.title").value(testTask.getTitle()));
        verify(taskService, times(1)).createTask(any(Task.class));
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTasksForCurrentUser_NoStatus() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskDto> taskDtoPage = new PageImpl<>(Arrays.asList(testTaskDto), pageable, 1);
        when(taskService.getTasksForCurrentUser(eq(null), any(Pageable.class))).thenReturn(taskDtoPage);
        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testTaskDto.getId()))
                .andExpect(jsonPath("$.content[0].title").value(testTaskDto.getTitle()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        verify(taskService, times(1)).getTasksForCurrentUser(eq(null), any(Pageable.class));
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTasksForCurrentUser_WithStatus() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskDto> taskDtoPage = new PageImpl<>(Arrays.asList(testTaskDto), pageable, 1);
        when(taskService.getTasksForCurrentUser(eq(TaskStatus.PENDING), any(Pageable.class))).thenReturn(taskDtoPage);
        mockMvc.perform(get("/api/tasks")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testTaskDto.getId()))
                .andExpect(jsonPath("$.content[0].status").value(testTaskDto.getStatus().name()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        verify(taskService, times(1)).getTasksForCurrentUser(eq(TaskStatus.PENDING), any(Pageable.class));
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTaskById() throws Exception {
        when(taskService.getTaskByIdForCurrentUser(1L)).thenReturn(Optional.of(testTaskDto));
        mockMvc.perform(get("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testTaskDto.getId()))
                .andExpect(jsonPath("$.data.title").value(testTaskDto.getTitle()));
        verify(taskService, times(1)).getTaskByIdForCurrentUser(1L);
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTaskById_NotFound() throws Exception {
        when(taskService.getTaskByIdForCurrentUser(anyLong())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/tasks/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expect 404 Not Found if service returns empty
        verify(taskService, times(1)).getTaskByIdForCurrentUser(99L);
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetTaskById_AccessDenied() throws Exception {
        when(taskService.getTaskByIdForCurrentUser(anyLong())).thenThrow(new ResourceNotFoundException("Access Denied"));
        mockMvc.perform(get("/api/tasks/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // ResourceNotFoundException maps to 404
        verify(taskService, times(1)).getTaskByIdForCurrentUser(2L);
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdateTask() throws Exception {
        Task updatedTaskDetails = Task.builder()
                .title("Updated Title")
                .description("Updated Desc")
                .status(TaskStatus.COMPLETED)
                .dueDate(LocalDate.now().plusDays(10))
                .assignedTo(testUser)
                .build();
        Task returnedTask = Task.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated Desc")
                .status(TaskStatus.COMPLETED)
                .dueDate(LocalDate.now().plusDays(10))
                .createdAt(testTask.getCreatedAt())
                .assignedTo(testUser)
                .build();
        when(taskService.updateTaskForCurrentUser(eq(1L), any(Task.class))).thenReturn(returnedTask);
        mockMvc.perform(put("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTaskDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(returnedTask.getId()))
                .andExpect(jsonPath("$.data.title").value(returnedTask.getTitle()))
                .andExpect(jsonPath("$.data.status").value(returnedTask.getStatus().name())); // Check enum name
        verify(taskService, times(1)).updateTaskForCurrentUser(eq(1L), any(Task.class));
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testUpdateTask_NotFound() throws Exception {
        when(taskService.updateTaskForCurrentUser(eq(99L), any(Task.class)))
                .thenThrow(new ResourceNotFoundException("Task not found with id 99"));
        Task updatedTaskDetails = Task.builder().title("Updated").build();
        mockMvc.perform(put("/api/tasks/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedTaskDetails)))
                .andExpect(status().isNotFound());
        verify(taskService, times(1)).updateTaskForCurrentUser(eq(99L), any(Task.class));
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDeleteTask() throws Exception {
        doNothing().when(taskService).deleteTaskForCurrentUser(1L);
        mockMvc.perform(delete("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task deleted successfully!"));
        verify(taskService, times(1)).deleteTaskForCurrentUser(1L);
    }
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDeleteTask_AccessDenied() throws Exception {
        doThrow(new ResourceNotFoundException("Access Denied")).when(taskService).deleteTaskForCurrentUser(2L);
        mockMvc.perform(delete("/api/tasks/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        verify(taskService, times(1)).deleteTaskForCurrentUser(2L);
    }
}