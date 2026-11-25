package com.braille.braille_learning.service;

import com.braille.braille_learning.dto.LoginRequest;
import com.braille.braille_learning.dto.LoginResponse;
import com.braille.braille_learning.dto.RegisterRequest;
import com.braille.braille_learning.dto.UserResponse;
import com.braille.braille_learning.dto.UserSummary;
import com.braille.braille_learning.entity.User;
import com.braille.braille_learning.exception.DuplicateEmailException;
import com.braille.braille_learning.exception.InvalidCredentialsException;
import com.braille.braille_learning.repository.UserRepository;
import com.braille.braille_learning.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered: " + email);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getFullName().trim(), email, encodedPassword);
        User savedUser = userRepository.save(user);

        return UserResponse.fromUser(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", UserSummary.fromUser(user));
    }
}
