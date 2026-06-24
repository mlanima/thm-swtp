package de.thm.swtp.api.common;

/**
 * Sanitizes client-controlled free text before it reaches a log line, so a
 * CR/LF in user input cannot forge fake log entries (log injection).
 *
 * <p>Use at genuine free-text sources (search queries, upload filenames) and at handler log
 * sites whose exception message embeds raw request fields (project name, project URL). Other
 * sites carry UUID/enums/HTTP-parsed values with no CRLF — see {@code GlobalExceptionHandler}.
 * Strip at the source, not scattered across every {@code log.x(ex.getMessage())} call.
 */
public final class LogSafe {

    private LogSafe() {}

    /** Replaces control chars (CR/LF/...) with {@code _}; null-safe. */
    public static String clean(String s) {
        return s == null ? null : s.replaceAll("\\p{Cntrl}", "_");
    }
}