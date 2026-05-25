package de.thm.swtp.api.userprofile.dto;

public record UserProfileRequest(
        String title,
        String location,
        String about,
        String experience
) {}
