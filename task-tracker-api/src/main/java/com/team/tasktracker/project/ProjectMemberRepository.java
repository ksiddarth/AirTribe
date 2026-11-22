package com.team.tasktracker.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByUserId(Long userId);
    List<ProjectMember> findByProjectId(Long projectId);
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
