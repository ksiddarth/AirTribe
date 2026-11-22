package com.team.tasktracker.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    public record UpdateProfileRequest(
            @Size(min = 1, max = 120) String name,
            @Size(max = 500) String avatarUrl
    ) {}

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User user = currentUser(auth);
        return toProfile(user);
    }

    @PatchMapping("/me")
    public Map<String, Object> updateMe(Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        User user = currentUser(auth);
        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name().trim());
        }
        if (req.avatarUrl() != null) {
            user.setAvatarUrl(req.avatarUrl().trim());
        }
        return toProfile(userRepository.save(user));
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Map<String, Object> toProfile(User u) {
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : ""
        );
    }
}
