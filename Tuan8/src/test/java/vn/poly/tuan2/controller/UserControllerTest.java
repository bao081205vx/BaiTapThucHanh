package vn.poly.tuan2.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.poly.tuan2.entity.Role;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.repository.UserRepository;
import vn.poly.tuan2.security.JwtUtils; // Import JwtUtils

import java.util.Arrays;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtils jwtUtils;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .password("password")
                .email("admin@example.com")
                .roles(Set.of(Role.ADMIN))
                .build();

        normalUser = User.builder()
                .id(2L)
                .username("user")
                .password("password")
                .email("user@example.com")
                .roles(Set.of(Role.USER))
                .build();
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_shouldReturnUsersWithoutPasswords_whenAdmin() throws Exception {
        when(userRepository.findAll()).thenReturn(Arrays.asList(adminUser, normalUser));

        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[1].username").value("user"))
                .andExpect(jsonPath("$[1].password").doesNotExist());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_shouldReturnForbidden_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verify(userRepository, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        mockMvc.perform(delete("/api/admin/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/admin/users/{id}", 99L))
                .andExpect(status().isNotFound());

        verify(userRepository, times(1)).existsById(99L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_shouldReturnForbidden_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", 1L))
                .andExpect(status().isForbidden());
        verify(userRepository, never()).existsById(anyLong());
        verify(userRepository, never()).deleteById(anyLong());
    }
}
