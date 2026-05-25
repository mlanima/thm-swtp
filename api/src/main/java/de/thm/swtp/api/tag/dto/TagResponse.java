package de.thm.swtp.api.tag.dto;

import de.thm.swtp.api.tag.domain.Tag;


import java.util.UUID;

/** Response body representing a Tag with all its details.*/
public record TagResponse(UUID id, String name){

    /** Converts a Tag-domain model into a valid response dto.*/
    public static TagResponse from(Tag tag){
        return new TagResponse(tag.getId(), tag.getName());
    }
}


