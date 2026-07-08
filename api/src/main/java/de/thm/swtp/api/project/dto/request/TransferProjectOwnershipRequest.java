package de.thm.swtp.api.project.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferProjectOwnershipRequest(@NotNull UUID newOwnerId) {}
