package de.thm.swtp.api.project.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectStatsResponse {
    private int contributors;
    private int views;
    private int likes;
    private int openPositions;
}