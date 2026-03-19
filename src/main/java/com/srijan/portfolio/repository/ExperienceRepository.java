package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findByUserId(Long userId);

    List<Experience> findByUserIdAndDeletedFalse(Long userId);

    List<Experience> findByUserUsernameAndDeletedFalse(String username);

    @Query("select count(e) from Experience e where e.user.id = :userId and e.deleted = false and e.isAcademic = false")
    long countProfessionalByUserId(@Param("userId") Long userId);

    java.util.Optional<Experience> findByIdAndUserUsername(Long id, String username);
}
