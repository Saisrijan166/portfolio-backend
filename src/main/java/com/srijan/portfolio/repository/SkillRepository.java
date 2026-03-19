package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByUserId(Long userId);

    List<Skill> findByUserUsername(String username);

    java.util.Optional<Skill> findByIdAndUserUsername(Long id, String username);
}
