package vn.poly.tuan2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.tuan2.entity.Role;
import vn.poly.tuan2.entity.User;
import vn.poly.tuan2.exception.UserAlreadyExistsException;
import vn.poly.tuan2.repository.UserRepository;
import vn.poly.tuan2.request.LoginRequest;
import vn.poly.tuan2.request.RegisterRequest;
import vn.poly.tuan2.response.ApiResponse;
import vn.poly.tuan2.response.JwtResponse;
import vn.poly.tuan2.security.JwtUtils;
import vn.poly.tuan2.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Attempting to register user: {}", registerRequest.getUsername());

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' is already taken.", registerRequest.getUsername());
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            logger.warn("Registration failed: Email '{}' is already in use.", registerRequest.getEmail());
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(Collections.singleton(Role.USER))
                .build();

        userRepository.save(user);
        logger.info("User '{}' registered successfully.", registerRequest.getUsername());

        return ResponseEntity.ok(new ApiResponse(true, "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Attempting to authenticate user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails springUserDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> userOptional = userRepository.findByUsername(springUserDetails.getUsername());

        if (!userOptional.isPresent()) {
            logger.error("Authentication successful but user details not found in DB for username: {}", springUserDetails.getUsername()); // Ghi log lỗi
            throw new ResourceNotFoundException("User details not found after authentication.");
        }

        User userEntity = userOptional.get();

        List<String> roles = springUserDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        logger.info("User '{}' logged in successfully with roles: {}", loginRequest.getUsername(), roles);

        return ResponseEntity.ok(new JwtResponse(jwt, userEntity.getId(), userEntity.getUsername(), userEntity.getEmail(), roles));
    }
}