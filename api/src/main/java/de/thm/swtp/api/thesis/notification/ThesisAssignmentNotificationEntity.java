package de.thm.swtp.api.thesis.notification;

import de.thm.swtp.api.thesis.ThesisEntity;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** Notifies a student in-app that a professor has assigned them to a thesis. */
@Entity(name = "thesis_assignment_notifications")
@Table(name = "thesis_assignment_notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThesisAssignmentNotificationEntity {

    /** Unique identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The student who was assigned and should be notified. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_keycloak_id", nullable = false)
    private UserProfile student;

    /** The thesis the student was assigned to. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "thesis_id", nullable = false)
    private ThesisEntity thesis;

    /** Whether the student has already seen this notification. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
