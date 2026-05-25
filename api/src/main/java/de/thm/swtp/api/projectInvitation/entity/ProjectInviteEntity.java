package de.thm.swtp.api.projectInvitation.entity;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** JPA entity representing an invitation to a project. */
@Entity
@Table(name="project_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInviteEntity {

    /** Unique Identifier. */
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    /** The project where the invitation came from. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="project_id", nullable=false)
    private ProjectEntity project;

    // TODO: Relation between user<->invitation missing, needs to be implemented here when UserEntity is created.
    // manyToOne?

    /** Optional message from the project-owner to the invited user. */
    @Column(length = 500)
    private String message;

    /** Timestamp of when the invite was sent.*/
    @Column(nullable = false, updatable = false)
    private LocalDateTime sendDate;

    /** Current status of the invite.*/
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectInviteStatus status;


}
