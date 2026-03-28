package com.srijan.portfolio.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class AuthSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationRunner syncAuthSchema() {
        return args -> {
            jdbcTemplate.execute("alter table public.email_otps drop constraint if exists email_otps_purpose_check");
            jdbcTemplate.execute("""
                    alter table public.email_otps
                    add constraint email_otps_purpose_check
                    check (purpose in ('LOGIN', 'VERIFY', 'RESET_PASSWORD'))
                    """);
        };
    }
}
