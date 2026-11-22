package com.team.tasktracker.project;

import com.team.tasktracker.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "project_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_member", columnNames = {"project_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole = "MEMBER";

    @CreationTimestamp
    @Column(updatable = false)
    private Instant joinedAt;
}
