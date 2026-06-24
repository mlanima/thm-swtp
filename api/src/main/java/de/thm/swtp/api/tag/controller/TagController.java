package de.thm.swtp.api.tag.controller;

import de.thm.swtp.api.tag.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final de.thm.swtp.api.tag.service.TagService tagService;

    @GetMapping
    public List<TagResponse> searchTags(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "32") int limit
    ) {
        return tagService.searchTags(q, limit)
                .stream()
                .map(TagResponse::toResponse)
                .toList();
    }
}
