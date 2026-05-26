package de.thm.swtp.api.tag.domain;

import lombok.Builder;
import lombok.Value;

/** Domain model representing a tag assigned to a project or a user profile. */
@Builder
@Value
public class Tag {
    String name;
}
