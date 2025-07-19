package vn.poly.tuan2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.repository.UserRepository;
import vn.poly.tuan2.exception.ResourceNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword1")
                .build();

        testUser2 = User.builder()
                .id(2L)
                .username("testuser2")
                .email("test2@example.com")
                .password("encodedPassword2")
                .build();
    }

    @Test
    void testCreateUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User createdUser = userService.createUser(testUser);

        assertNotNull(createdUser);
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetAllUsers() {
        List<User> users = Arrays.asList(testUser, testUser2);
        when(userRepository.findAll()).thenReturn(users);

        List<User> foundUsers = userService.getAllUsers();

        assertNotNull(foundUsers);
        assertEquals(2, foundUsers.size());
        assertEquals("testuser", foundUsers.get(0).getUsername());
        assertEquals("testuser2", foundUsers.get(1).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetUserById_Found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> foundUser = userService.getUserById(1L);

        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getUsername(), foundUser.get().getUsername());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        Optional<User> foundUser = userService.getUserById(99L);

        assertFalse(foundUser.isPresent());
        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void testUpdateUser_Success() {
        User updatedDetails = User.builder()
                .username("updatedUser")
                .email("updated@example.com")
                .password("newEncodedPassword")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedDetails);

        User result = userService.updateUser(1L, updatedDetails);

        assertNotNull(result);
        assertEquals("updatedUser", result.getUsername());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("newEncodedPassword", result.getPassword());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdateUser_NotFound() {
        User updatedDetails = User.builder().username("updatedUser").build();
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(99L, updatedDetails);
        });
        assertEquals("User not found with id 99", exception.getMessage());
        verify(userRepository, times(1)).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteUser() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }
}
