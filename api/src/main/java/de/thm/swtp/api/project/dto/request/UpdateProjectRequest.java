package de.thm.swtp.api.project.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
public class UpdateProjectRequest {
    private String name;
    private String description;
    private String shortDescription;
    private String projectUrl;
    @JsonProperty("isPrivateProject")
    private boolean isPrivateProject;
}
