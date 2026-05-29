package de.thm.swtp.api.projectJoinRequest.entity;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectJoinRequest.domain.ProjectJoinRequestStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** JPA entity representing a join-request to a project. */

@Entity
@Table(name="project_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectJoinRequestEntity {

    /** Unique Identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The project where the invitation was sent to. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    /** The User profile that sent the invitation. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_user_id", nullable = false)
    private UserProfile requestingUser;

    /** Optional message from the user to the project owner. */
    @Column(length = 500)
    private String message;

    /** Timestamp of when the invite was sent. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp of when the invite was last updated. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Current status of the join-request. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectJoinRequestStatus status;


    /** Automatically sets the creation date and the status to pending when creating the entity. */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if(this.status == null) {
            this.status = ProjectJoinRequestStatus.PENDING;
        }
    }

    /** Automatically updates the updatedAt field on entity updates. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
