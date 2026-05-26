package de.thm.swtp.api.tag.dto;

import de.thm.swtp.api.tag.domain.Tag;

public record TagResponse(String name) {
    public static TagResponse toResponse(Tag tag){
        return new TagResponse(tag.getName());
    }
}
