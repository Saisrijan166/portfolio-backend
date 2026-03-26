package com.srijan.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    private String accessToken;

    private String refreshToken;

    private String username;

    private Long userId;
}
