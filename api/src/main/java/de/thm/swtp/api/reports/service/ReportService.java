package de.thm.swtp.api.reports.service;

import de.thm.swtp.api.exceptionhandling.exceptions.InvalidReportSortFieldException;
import de.thm.swtp.api.exceptionhandling.exceptions.ReportTargetNotFoundException;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.reports.domain.Report;
import de.thm.swtp.api.reports.domain.ReportReason;
import de.thm.swtp.api.reports.domain.ReportTarget;
import de.thm.swtp.api.reports.entity.ReportEntity;
import de.thm.swtp.api.reports.mapper.ReportMapper;
import de.thm.swtp.api.reports.repository.ReportRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPostRepository projectPostRepository;

    private static final Set<String> ALLOWED_REPORT_SORT_FIELDS = Set.of("createdAt", "updatedAt", "reportStatus", "reason", "target");


    @Transactional
    public Report createReport(UUID currentUserId, ReportTarget target, UUID targetId, ReportReason reason, String message) {
        UserProfile reporter = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));


        validateReportTarget(target,targetId);

        ReportEntity reportEntity = ReportEntity.builder()
                .reporter(reporter)
                .target(target)
                .targetId(targetId)
                .reason(reason)
                .message(message)
                .build();

        ReportEntity saved = reportRepository.save(reportEntity);
        return ReportMapper.toDomain(saved);

    }

    @Transactional
    public Page<Report> getReports(Pageable pageable){
        validateReportSort(pageable);
        return reportRepository.findAll(pageable)
                .map(ReportMapper::toDomain);
    }


    private void validateReportTarget(ReportTarget target, UUID targetId){
        boolean exists = switch (target){
            case USER -> userProfileRepository.existsById(targetId);
            case PROJECT -> projectRepository.existsById(targetId);
            case PROJECT_POST -> projectPostRepository.existsById(targetId);
        };

        if (!exists){
            throw new ReportTargetNotFoundException("Reported target not found:" + targetId);
        }
    }

    private void validateReportSort(Pageable pageable){
        pageable.getSort().forEach(sortField -> {
            if (!ALLOWED_REPORT_SORT_FIELDS.contains(sortField.getProperty())) {
                throw new InvalidReportSortFieldException("Unsupported sort field: " + sortField.getProperty());
            }
        });
    }

}


