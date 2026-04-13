package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.ResumeJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeJobRepository extends JpaRepository<ResumeJob, Long> {
    Optional<ResumeJob> findByIdAndUserUsername(Long id, String username);
}
