package com.example.notes.service;

import com.example.notes.model.AppUser;
import com.example.notes.repository.UserRepository;
import com.example.notes.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.notes.dto.AuthDtos.AuthResponse;
import static com.example.notes.dto.AuthDtos.LoginRequest;
import static com.example.notes.dto.AuthDtos.RegisterRequest;
import static com.example.notes.dto.AuthDtos.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya esta registrado");
        }

        AppUser user = userRepository.save(new AppUser(
                email,
                request.name().trim(),
                passwordEncoder.encode(request.password())
        ));

        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        return responseFor(user);
    }

    private AuthResponse responseFor(AppUser user) {
        return new AuthResponse(
                "Bearer",
                jwtService.createToken(user),
                jwtService.expirationSeconds(),
                new UserResponse(user.getId(), user.getEmail(), user.getName())
        );
    }
}
