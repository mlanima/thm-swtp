package de.thm.swtp.api.userprofile.dto;

import jakarta.validation.constraints.Size;

public record BanUserRequest(@Size(max = 1000) String reason) {
}
