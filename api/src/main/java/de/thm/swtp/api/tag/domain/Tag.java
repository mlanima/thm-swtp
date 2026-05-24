package de.thm.swtp.api.tag.domain;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/** Domain model representing a tag which can be assigned to projects or user profiles.
 */

@Builder
@Value
public class Tag {
    UUID id;
    String name;
}
