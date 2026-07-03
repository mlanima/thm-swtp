package de.thm.swtp.api.reports.repository;

import de.thm.swtp.api.reports.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repository for report entities.*/
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

}
