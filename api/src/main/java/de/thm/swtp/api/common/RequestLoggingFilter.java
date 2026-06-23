package de.thm.swtp.api.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every HTTP request at debug level: {@code METHOD uri -> status in Xms}.
 *
 * <p>Dev visibility: set {@code LOGGING_LEVEL_DE_THM_SWTP_API=DEBUG} (or
 * {@code logging.level.de.thm.swtp.api=debug}) to see every click. At the production
 * default (INFO) this filter is silent, so read paths stay quiet in prod — see
 * {@code api/CLAUDE.md}. Lives at the HTTP boundary so it sees every request,
 * including reads, 401s and framework routes, without per-service instrumentation.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            log.debug("{} {} -> {} in {}ms", req.getMethod(), req.getRequestURI(), res.getStatus(),
                    System.currentTimeMillis() - start);
        }
    }
}
