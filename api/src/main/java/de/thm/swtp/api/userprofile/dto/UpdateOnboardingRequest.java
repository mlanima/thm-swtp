package de.thm.swtp.api.userprofile.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingRequest(@NotNull Boolean onboardingCompleted) {
}
