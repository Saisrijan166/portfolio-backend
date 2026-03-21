package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByUserId(Long userId);

    List<Education> findByUserUsername(String username);

    java.util.Optional<Education> findByIdAndUserUsername(Long id, String username);
}
