package de.thm.swtp.api.common;

import java.util.regex.Pattern;

/**
 * Sanitizes client-controlled free text before it reaches a log line, so a
 * CR/LF in user input cannot forge fake log entries (log injection).
 *
 * <p>Use at genuine free-text sources (search queries, upload filenames) and at handler log
 * sites whose exception message embeds raw request fields (project name, project URL,
 * upload Content-Type). Other sites carry UUID/enums/HTTP-parsed values with no CRLF —
 * see {@code GlobalExceptionHandler}. Strip at the source, not scattered across every
 * {@code log.x(ex.getMessage())} call.
 */
public final class LogSafe {

    private LogSafe() {}

    // \p{Cntrl} misses U+2028/U+2029 (line/paragraph separators, category Zl/Zp), which
    // search-query params can carry via %-encoding — include them explicitly.
    private static final Pattern LOG_UNSAFE = Pattern.compile("[\\p{Cntrl}\\u2028\\u2029]");

    /** Replaces control chars + line/paragraph separators with {@code _}; null-safe. */
    public static String clean(String s) {
        return s == null ? null : LOG_UNSAFE.matcher(s).replaceAll("_");
    }
}