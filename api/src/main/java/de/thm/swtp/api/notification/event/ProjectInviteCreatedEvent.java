package de.thm.swtp.api.notification.event;

import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;

public record ProjectInviteCreatedEvent(ProjectInvite invite, String invitedUserEmail) {}
