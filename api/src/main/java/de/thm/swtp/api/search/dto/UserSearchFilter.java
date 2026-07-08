package de.thm.swtp.api.search.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Optional filter criteria for user search.
 * <p>
 * Every field is nullable; a {@code null} value means "no constraint on this field".
 * Filters are applied in addition to the keyword match, not as a replacement for it.
 *
 * @param isProfessor restrict to users with (or without) verified professor status
 * @param tags restrict to users having at least one of the given tag names
 * @param location restrict to users whose location contains this text (case-insensitive)
 * @param createdAfter restrict to users created at or after this timestamp
 * @param createdBefore restrict to users created at or before this timestamp
 */
public record UserSearchFilter(
        Boolean isProfessor,
        List<String> tags,
        String location,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {

    public static final UserSearchFilter EMPTY = new UserSearchFilter(null, null, null, null, null);
}
