package de.thm.swtp.api.thesis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DeleteThesisResponse {
    private UUID thesisId;
    private String message;
}
