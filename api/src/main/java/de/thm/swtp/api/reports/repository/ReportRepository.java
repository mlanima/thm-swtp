package de.thm.swtp.api.reports.repository;

import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportStatus;
import de.thm.swtp.api.reports.domain.ReportTarget;
import de.thm.swtp.api.reports.entity.ReportEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.UUID;

/** Repository for report entities.*/
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {


    /** Searches reports using filters and a query.*/
    @Query("""
        SELECT r
        FROM ReportEntity r
        JOIN r.reporter reporter
            WHERE (:status IS NULL OR r.status = :status)
              AND (:target IS NULL OR r.target = :target)
              AND (:reason IS NULL OR r.reason = :reason)
              AND (
                    :query IS NULL
                    OR LOWER(r.message) LIKE :query
                    OR LOWER(reporter.username) LIKE :query
                    OR LOWER(r.reviewerUsername) LIKE :query
                    OR LOWER(r.moderatorMessage) LIKE :query
          )
        """)
    Page<ReportEntity> searchReports(
            @Param("status") ReportStatus status,
            @Param("target") ReportTarget target,
            @Param("reason") ReportReason reason,
            @Param("query") String query,
            Pageable pageable
    );

    long countByTargetAndTargetIdAndStatusIn(ReportTarget target, UUID reportId, Collection<ReportStatus> statuses);
}
