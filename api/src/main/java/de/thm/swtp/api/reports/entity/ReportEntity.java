package de.thm.swtp.api.reports.entity;

import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** JPA entity representing a report submitted by a user.*/
@Entity
@Table(name = "reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {

    /** Unique identifier of the report.*/
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User who submitted the report.*/
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserProfile reporter;

    /** Type of the entity that was reported.*/
    @Enumerated(EnumType.STRING)
    @Column(name = "report_target", nullable = false, length = 64)
    private ReportTarget target;

    /** Unique identifier of the reported entity.*/
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** Report reason selected by the reporting user. */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false, length = 64)
    private ReportReason reason;

    /** Optional free-text message from the reporting user.*/
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Current moderation status of the report.*/
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "report_status", nullable = false, length = 32)
    private ReportStatus status = ReportStatus.OPEN;

    /** Keycloak identifier of the reviewing moderator.*/
    @Column(name = "reviewer_keycloak_id")
    private UUID reviewerKeycloakId;

    /** Username of the reviewing moderator.*/
    @Column(name = "reviewer_username")
    private String reviewerUsername;

    /** Timestamp when the report was reviewed*/
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Optional internal moderation message.*/
    @Column(name = "moderator_message", columnDefinition = "TEXT")
    private String moderatorMessage;

    /** Timestamp when the report was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the report was last updated. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = ReportStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
