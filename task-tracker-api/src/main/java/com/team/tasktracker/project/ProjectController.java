package com.team.tasktracker.project;

import com.team.tasktracker.user.User;
import com.team.tasktracker.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public record CreateProjectRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description
    ) {}

    public record AddMemberRequest(
            @NotBlank @Email String email,
            String role
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(Authentication auth, @Valid @RequestBody CreateProjectRequest req) {
        User owner = getUser(auth);
        Project project = projectRepository.save(Project.builder()
                .name(req.name().trim())
                .description(req.description())
                .owner(owner)
                .build());
        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(owner)
                .memberRole("OWNER")
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", project.getId(),
                "name", project.getName(),
                "description", project.getDescription() != null ? project.getDescription() : "",
                "ownerId", owner.getId()
        ));
    }

    @GetMapping
    public List<Map<String, Object>> listProjects(Authentication auth) {
        User user = getUser(auth);
        return projectMemberRepository.findByUserId(user.getId()).stream()
                .map(pm -> Map.<String, Object>of(
                        "id", pm.getProject().getId(),
                        "name", pm.getProject().getName(),
                        "description", pm.getProject().getDescription() != null ? pm.getProject().getDescription() : "",
                        "role", pm.getMemberRole()
                ))
                .toList();
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            Authentication auth,
            @PathVariable Long projectId,
            @Valid @RequestBody AddMemberRequest req
    ) {
        User requester = getUser(auth);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, requester.getId())) {
            throw new AccessDeniedException("Not a member of this project");
        }

        User invitee = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invited user not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this project");
        }

        String role = (req.role() == null || req.role().isBlank()) ? "MEMBER" : req.role().trim().toUpperCase();
        ProjectMember saved = projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(invitee)
                .memberRole(role)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "projectId", projectId,
                "userId", invitee.getId(),
                "email", invitee.getEmail(),
                "role", saved.getMemberRole()
        ));
    }

    @GetMapping("/{projectId}/members")
    public List<Map<String, Object>> listMembers(Authentication auth, @PathVariable Long projectId) {
        User requester = getUser(auth);
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, requester.getId())) {
            throw new AccessDeniedException("Not a member of this project");
        }
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(pm -> Map.<String, Object>of(
                        "userId", pm.getUser().getId(),
                        "name", pm.getUser().getName(),
                        "email", pm.getUser().getEmail(),
                        "role", pm.getMemberRole()
                ))
                .toList();
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
