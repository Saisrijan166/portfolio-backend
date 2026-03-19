package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);

    List<Project> findByUserIdAndDeletedFalse(Long userId);

    List<Project> findByUserUsernameAndDeletedFalse(String username);

    long countByUserIdAndDeletedFalse(Long userId);

    java.util.Optional<Project> findByIdAndUserUsername(Long id, String username);
}
