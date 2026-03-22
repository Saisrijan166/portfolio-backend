package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.About;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AboutRepository extends JpaRepository<About, Long> {
    Optional<About> findByUserId(Long userId);

    List<About> findAllByUserIdOrderByIdAsc(Long userId);
}
