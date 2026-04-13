package com.srijan.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ResumeJobResponseDto;
import com.srijan.portfolio.dto.ResumeParseRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeScoreDto;
import com.srijan.portfolio.entity.ResumeJob;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.repository.ResumeJobRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeJobService {

    private final ResumeJobRepository resumeJobRepository;
    private final UserRepository userRepository;
    private final ResumeJobAsyncProcessor resumeJobAsyncProcessor;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeJobResponseDto createJob(String username, ResumeParseRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        ResumeJob job = resumeJobRepository.save(ResumeJob.builder()
                .user(user)
                .status("PENDING")
                .cached(false)
                .build());

        resumeJobAsyncProcessor.processJobAsync(job.getId(), request);

        return ResumeJobResponseDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .cached(false)
                .build();
    }

    @Transactional(readOnly = true)
    public ResumeJobResponseDto getJob(String username, Long id) {
        ResumeJob job = resumeJobRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESUME_JOB_NOT_FOUND", "Resume job not found"));

        return ResumeJobResponseDto.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .error(job.getError())
                .cached(job.isCached())
                .result(read(job.getResult(), ResumeParseResponseDto.class))
                .score(read(job.getScore(), ResumeScoreDto.class))
                .build();
    }

    private <T> T read(String value, Class<T> targetType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, targetType);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
