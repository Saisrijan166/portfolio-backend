package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeRegenerateRequestDto {

    @NotNull
    @Valid
    private ResumeParseResponseDto existingResume;

    @NotEmpty
    @Size(max = 5)
    private List<String> sections;
}
