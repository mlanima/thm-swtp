package de.thm.swtp.api.projectInvitation.entity;

import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="project_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInviteEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    // TODO: Relation between project<->invitation and user<->invitation missing, needs to be implemented here when both entities are created.
    // manyToOne?

    @Column(length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sendDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectInviteStatus status;


}
