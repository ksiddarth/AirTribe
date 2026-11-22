package com.team.tasktracker.user;

import com.team.tasktracker.config.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 120) String name,
            @NotBlank(message = "Email is required") @Email @Size(max = 180) String email,
            @NotBlank(message = "Password is required") @Size(min = 8, max = 100) String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = User.builder()
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "token", token,
                "user", Map.of("id", saved.getId(), "name", saved.getName(), "email", saved.getEmail())
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                if (claims.getId() != null && claims.getExpiration() != null && !revokedTokenRepository.existsByJti(claims.getId())) {
                    revokedTokenRepository.save(RevokedToken.builder()
                            .jti(claims.getId())
                            .expiresAt(claims.getExpiration().toInstant())
                            .build());
                }
            } catch (Exception ignored) {
                // Token already invalid/expired
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
