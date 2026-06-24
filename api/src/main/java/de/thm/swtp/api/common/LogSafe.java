package de.thm.swtp.api.common;

/**
 * Sanitizes client-controlled free text before it reaches a log line, so a
 * CR/LF in user input cannot forge fake log entries (log injection).
 *
 * <p>Use only at genuine free-text sources (search query params, upload
 * filenames). Most log sites carry UUID-validated path params, @Pattern-secured
 * strings, or HTTP-parsed values (e.g. multipart Content-Type), where raw CRLF
 * cannot survive upstream validation/parsing — see the policy comment in
 * {@code GlobalExceptionHandler}. Strip at the source, not scattered across
 * every {@code log.x(ex.getMessage())} site.
 */
public final class LogSafe {

    private LogSafe() {}

    /** Replaces control chars (CR/LF/...) with {@code _}; null-safe. */
    public static String clean(String s) {
        return s == null ? null : s.replaceAll("\\p{Cntrl}", "_");
    }
}