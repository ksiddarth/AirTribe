package com.team.tasktracker.collaboration;

import com.team.tasktracker.task.Task;
import com.team.tasktracker.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "task_comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
