package de.thm.swtp.api.project.dto.response;

import lombok.*;
import java.util.*;



@Data
@Builder
public class DeleteProjectResponse {
    private UUID projectId;
    private String message;
}
