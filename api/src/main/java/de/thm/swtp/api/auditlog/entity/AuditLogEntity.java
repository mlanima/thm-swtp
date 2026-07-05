package de.thm.swtp.api.auditlog.entity;

import de.thm.swtp.api.auditlog.domain.AuditLogAction;
import de.thm.swtp.api.auditlog.domain.AuditLogTargetType;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditLogAction action;

    @Column(nullable = false)
    private UUID actorUserId;

    @Column(length = 255)
    private String actorUsername;

    @Column(length = 255)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditLogTargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 255)
    private String targetName;

    @Column(length = 2000)
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}