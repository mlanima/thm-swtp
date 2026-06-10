package de.thm.swtp.api.projectJoinRequest.dto;

import jakarta.validation.constraints.Size;

/** Request DTO for creating a new project join-request. */
public record CreateProjectJoinRequestRequest(@Size(max = 500) String message) {

}
