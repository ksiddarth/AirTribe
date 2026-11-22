package com.team.tasktracker.task;

import com.team.tasktracker.extensions.NotificationStreamService;
import com.team.tasktracker.project.Project;
import com.team.tasktracker.project.ProjectMember;
import com.team.tasktracker.project.ProjectMemberRepository;
import com.team.tasktracker.project.ProjectRepository;
import com.team.tasktracker.user.User;
import com.team.tasktracker.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationStreamService notificationService;

    public record CreateTaskRequest(
            @NotBlank @Size(max = 180) String title,
            String description,
            LocalDate dueDate,
            TaskPriority priority,
            Long assigneeId
    ) {}

    public record UpdateTaskRequest(
            @Size(max = 180) String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Long assigneeId
    ) {}

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<Map<String, Object>> createTask(
            Authentication auth,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest req
    ) {
        User creator = getUser(auth);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        ensureMember(projectId, creator.getId());

        User assignee = null;
        if (req.assigneeId() != null) {
            ensureMember(projectId, req.assigneeId());
            assignee = userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
        }

        Task task = taskRepository.save(Task.builder()
                .project(project)
                .creator(creator)
                .assignee(assignee)
                .title(req.title().trim())
                .description(req.description())
                .dueDate(req.dueDate())
                .priority(req.priority() != null ? req.priority() : TaskPriority.MEDIUM)
                .status(TaskStatus.OPEN)
                .build());

        if (assignee != null) {
            notificationService.notifyUser(assignee.getId(), task.getId(), "TASK_ASSIGNED",
                    "Assigned task: " + task.getTitle());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(task));
    }

    @GetMapping("/tasks")
    public Page<Map<String, Object>> listTasks(
            Authentication auth,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        User user = getUser(auth);
        List<Long> accessibleIds = projectMemberRepository.findByUserId(user.getId()).stream()
                .map(pm -> pm.getProject().getId())
                .toList();

        var spec = TaskSpecification.filterTasks(accessibleIds, projectId, assigneeId, status, q);
        return taskRepository.findAll(spec, pageable).map(this::toDto);
    }

    @GetMapping("/tasks/{taskId}")
    public Map<String, Object> getTask(Authentication auth, @PathVariable Long taskId) {
        User user = getUser(auth);
        Task task = getAuthorizedTask(taskId, user.getId());
        return toDto(task);
    }

    @PatchMapping("/tasks/{taskId}")
    public Map<String, Object> updateTask(
            Authentication auth,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest req
    ) {
        User user = getUser(auth);
        Task task = getAuthorizedTask(taskId, user.getId());
        Long previousAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        if (req.title() != null && !req.title().isBlank()) {
            task.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            task.setDescription(req.description());
        }
        if (req.status() != null) {
            task.setStatus(req.status());
        }
        if (req.priority() != null) {
            task.setPriority(req.priority());
        }
        if (req.dueDate() != null) {
            task.setDueDate(req.dueDate());
        }
        if (req.assigneeId() != null) {
            ensureMember(task.getProject().getId(), req.assigneeId());
            User newAssignee = userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee not found"));
            task.setAssignee(newAssignee);
        }

        Task saved = taskRepository.save(task);
        if (saved.getAssignee() != null && !saved.getAssignee().getId().equals(previousAssigneeId)) {
            notificationService.notifyUser(saved.getAssignee().getId(), saved.getId(), "TASK_ASSIGNED",
                    "Assigned task: " + saved.getTitle());
        } else if (saved.getAssignee() != null) {
            notificationService.notifyUser(saved.getAssignee().getId(), saved.getId(), "TASK_UPDATED",
                    "Updated task: " + saved.getTitle());
        }

        return toDto(saved);
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(Authentication auth, @PathVariable Long taskId) {
        User user = getUser(auth);
        Task task = getAuthorizedTask(taskId, user.getId());
        taskRepository.delete(task);
        return ResponseEntity.noContent().build();
    }

    private Task getAuthorizedTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        ensureMember(task.getProject().getId(), userId);
        return task;
    }

    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("User is not a member of project " + projectId);
        }
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Map<String, Object> toDto(Task t) {
        return Map.of(
                "id", t.getId(),
                "projectId", t.getProject().getId(),
                "creatorId", t.getCreator().getId(),
                "assigneeId", t.getAssignee() != null ? t.getAssignee().getId() : "",
                "title", t.getTitle(),
                "description", t.getDescription() != null ? t.getDescription() : "",
                "status", t.getStatus().name(),
                "priority", t.getPriority().name(),
                "dueDate", t.getDueDate() != null ? t.getDueDate().toString() : ""
        );
    }
}
