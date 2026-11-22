package com.team.tasktracker.collaboration;

import com.team.tasktracker.project.ProjectMemberRepository;
import com.team.tasktracker.task.Task;
import com.team.tasktracker.task.TaskRepository;
import com.team.tasktracker.user.User;
import com.team.tasktracker.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}")
@RequiredArgsConstructor
public class CollaborationController {

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskAttachmentRepository attachmentRepository;

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    public record CommentRequest(@NotBlank String body) {}

    @PostMapping("/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            Authentication auth,
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest req
    ) {
        User author = getUser(auth);
        Task task = authorizedTask(taskId, author.getId());
        TaskComment saved = commentRepository.save(TaskComment.builder()
                .task(task)
                .author(author)
                .body(req.body().trim())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "taskId", taskId,
                "authorId", author.getId(),
                "body", saved.getBody()
        ));
    }

    @GetMapping("/comments")
    public List<Map<String, Object>> listComments(Authentication auth, @PathVariable Long taskId) {
        User user = getUser(auth);
        authorizedTask(taskId, user.getId());
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "authorId", c.getAuthor().getId(),
                        "authorName", c.getAuthor().getName(),
                        "body", c.getBody(),
                        "createdAt", c.getCreatedAt().toString()
                ))
                .toList();
    }

    @PostMapping("/attachments")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            Authentication auth,
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }
        User uploader = getUser(auth);
        Task task = authorizedTask(taskId, uploader.getId());

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        TaskAttachment saved = attachmentRepository.save(TaskAttachment.builder()
                .task(task)
                .uploadedBy(uploader)
                .fileName(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storageKey(storedName)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "fileName", saved.getFileName(),
                "fileSize", saved.getFileSize(),
                "contentType", saved.getContentType() != null ? saved.getContentType() : "application/octet-stream"
        ));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            Authentication auth,
            @PathVariable Long taskId,
            @PathVariable Long attachmentId
    ) throws IOException {
        User user = getUser(auth);
        authorizedTask(taskId, user.getId());

        TaskAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!att.getTask().getId().equals(taskId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment does not belong to task");
        }

        Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(att.getStorageKey());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file missing");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getContentType() != null ? att.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="" + att.getFileName() + """)
                .body(resource);
    }

    private Task authorizedTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!projectMemberRepository.existsByProjectIdAndUserId(task.getProject().getId(), userId)) {
            throw new AccessDeniedException("Not a member of this task's project");
        }
        return task;
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
