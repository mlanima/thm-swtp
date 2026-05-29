package de.thm.swtp.api.projectJoinRequest.dto;

import jakarta.validation.constraints.Size;

public record CreateProjectJoinRequestRequest(@Size(max = 500) String message) {

}
