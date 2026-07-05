package de.thm.swtp.api.auditlog.repository;

import de.thm.swtp.api.auditlog.entity.AuditLogEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}