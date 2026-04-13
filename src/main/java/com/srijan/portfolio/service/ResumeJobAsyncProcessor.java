package com.srijan.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ResumeParseRequestDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeScoreDto;
import com.srijan.portfolio.entity.ResumeJob;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.repository.ResumeJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeJobAsyncProcessor {

    private final ResumeJobRepository resumeJobRepository;
    private final ResumeParseService resumeParseService;
    private final ResumeScoringService resumeScoringService;
    private final ObjectMapper objectMapper;

    @Async("resumeTaskExecutor")
    public void processJobAsync(Long jobId, ResumeParseRequestDto request) {
        ResumeJob job = resumeJobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "RESUME_JOB_NOT_FOUND", "Resume job not found"));

        try {
            job.setStatus("PROCESSING");
            resumeJobRepository.save(job);

            ResumeParseResponseDto result = resumeParseService.parseResume(
                    request.getFileBase64(),
                    request.getFileType(),
                    request.getFileName()
            );
            ResumeScoreDto score = resumeScoringService.score(result);

            job.setStatus("COMPLETED");
            job.setResult(objectMapper.writeValueAsString(result));
            job.setScore(objectMapper.writeValueAsString(score));
            job.setError(null);
            resumeJobRepository.save(job);
        } catch (Exception exception) {
            log.error("Resume Job {} failed with exception", jobId, exception);
            job.setStatus("FAILED");
            job.setError(sanitizeFailureMessage(exception));
            resumeJobRepository.save(job);
        }
    }

    private String sanitizeFailureMessage(Exception exception) {
        if (exception instanceof ApiException apiException && apiException.getMessage() != null && !apiException.getMessage().isBlank()) {
            return apiException.getMessage();
        }
        return "Resume processing failed. Please try again.";
    }
}
