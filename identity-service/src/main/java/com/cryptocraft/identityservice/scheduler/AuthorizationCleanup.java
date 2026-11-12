package com.cryptocraft.identityservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring Authorization Server keeps every sign-in and service token in oauth2_authorization and never
 * deletes them; rows whose last token expired a day ago are of no use.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationCleanup {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "${identity.cleanup-ms:3600000}", initialDelayString = "${identity.cleanup-ms:3600000}")
    public void deleteExpired() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM oauth2_authorization
                WHERE GREATEST(refresh_token_expires_at, access_token_expires_at, authorization_code_expires_at)
                      < now() - INTERVAL '1 day'
                """);
        if (deleted > 0) {
            log.info("Deleted {} expired authorizations", deleted);
        }
    }
}
