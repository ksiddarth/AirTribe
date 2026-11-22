package com.team.tasktracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByJti(String jti);
}
