package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByUserIdAndDeletedFalse(Long userId);

    List<Education> findByUserId(Long userId);
}
