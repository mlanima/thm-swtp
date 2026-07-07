package de.thm.swtp.api.search.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Optional filter criteria for project search.
 * <p>
 * Every field is nullable; a {@code null} value means "no constraint on this field".
 * Filters are applied in addition to the keyword match, not as a replacement for it.
 *
 * @param hasOpenPositions restrict to projects with (or without) open positions
 * @param allowJoinRequests restrict to projects accepting (or not accepting) join requests
 * @param tags restrict to projects having at least one of the given tag names
 * @param createdAfter restrict to projects created at or after this timestamp
 * @param createdBefore restrict to projects created at or before this timestamp
 */
public record ProjectSearchFilter(
        Boolean hasOpenPositions,
        Boolean allowJoinRequests,
        List<String> tags,
        LocalDateTime createdAfter,
        LocalDateTime createdBefore
) {

    public static final ProjectSearchFilter EMPTY = new ProjectSearchFilter(null, null, null, null, null);
}
