package com.srijan.portfolio.controller;

import com.srijan.portfolio.dto.AboutDto;
import com.srijan.portfolio.dto.ApiResponse;
import com.srijan.portfolio.service.AboutService;
import com.srijan.portfolio.service.PortfolioService;
import com.srijan.portfolio.util.ApiResponses;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class PublicAboutController {

    private static final String CACHE_PUBLIC = "public, max-age=300, s-maxage=300";
    private static final String USERNAME_PATTERN = "^[a-z0-9][a-z0-9-]{2,30}$";

    private final AboutService aboutService;
    private final PortfolioService portfolioService;

    @GetMapping({"/api/public/about/{username}", "/api/public/portfolio/{username}/about"})
    public ResponseEntity<ApiResponse<AboutDto>> getPublicAbout(
            @PathVariable
            @Pattern(regexp = USERNAME_PATTERN, message = "Username format is invalid")
            String username) {
        AboutDto data = aboutService.getPublicAbout(username);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_PUBLIC)
                .body(ApiResponses.success(
                        data,
                        data == null && !portfolioService.publicUserExists(username) ? "User not found" : "About loaded"
                ));
    }
}
