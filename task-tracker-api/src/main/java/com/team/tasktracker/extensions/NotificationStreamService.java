package com.team.tasktracker.extensions;

import com.team.tasktracker.user.User;
import com.team.tasktracker.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @GetMapping("/stream")
    public SseEmitter stream(Authentication auth) {
        User user = userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(user.getId(), emitter));
        emitter.onTimeout(() -> removeEmitter(user.getId(), emitter));
        return emitter;
    }

    @GetMapping
    public List<Notification> list(Authentication auth) {
        User user = userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
    }

    public void notifyUser(Long recipientId, Long taskId, String type, String message) {
        Notification saved = notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .taskId(taskId)
                .type(type)
                .message(message)
                .build());

        List<SseEmitter> userEmitters = emitters.get(recipientId);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name(type).data(saved));
                } catch (IOException ex) {
                    removeEmitter(recipientId, emitter);
                }
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
