package de.thm.swtp.api.project;


import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectMemberNotFoundException;
import de.thm.swtp.api.project.dto.request.*;
import de.thm.swtp.api.project.dto.response.*;
import de.thm.swtp.api.project.exception.*;
import de.thm.swtp.api.projectInvitation.service.ProjectInviteService;
import de.thm.swtp.api.projectFavorite.repository.ProjectFavoriteRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.projectView.repository.ProjectViewRepository;

import jakarta.transaction.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.*;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProjectInviteService projectInviteService;
    private static final String PROJECT_CREATION_INVITE_MESSAGE = "You have been invited to join this project.";
    private final ProjectFavoriteRepository projectFavoriteRepository;
    private final ProjectViewRepository projectViewRepository;

    private ProjectResponse toResponse(ProjectEntity project) {
        Set<UUID> memberIds = project.getMembers().stream()
                .map(UserProfile::getKeycloakId)
                .collect(java.util.stream.Collectors.toSet());

        int contributors = memberIds.size();

        if (project.getOwner() != null) {
            contributors++;
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .shortDescription(project.getShortDescription())
                .projectUrl(project.getProjectUrl())
                .isPrivateProject(project.isPrivateProject())
                .allowJoinRequests(project.isAllowJoinRequests())
                .ownerId(project.getOwner().getKeycloakId())
                .ownerUsername(project.getOwner().getUsername())
                .memberIds(project.getMembers().stream()
                        .map(UserProfile::getKeycloakId)
                        .collect(java.util.stream.Collectors.toSet()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .stats(ProjectStatsResponse.builder()
                        .contributors(contributors)
                        .views((int) projectViewRepository.countByProjectId(project.getId()))
                        .likes((int) projectFavoriteRepository.countByProjectId(project.getId()))
                        .openPositions(project.getOpenPositionsCount())
                        .build())
                .favoriteCount(projectFavoriteRepository.countByProjectId(project.getId()))

                .build();
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID currentUserId) {

        if (projectRepository.existsByName(request.name())) {
            throw new ExceptionProjectResponse(request.name());
        }

        UserProfile owner = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        String projectUrl;
        if (request.projectUrl() == null || request.projectUrl().isBlank()) {
            projectUrl = resolveUniqueUrl(ProjectUrlUtils.generateProjectUrl(request.name()));
        } else {
            if (!ProjectUrlUtils.isValidUrl(request.projectUrl())) {
                throw new ExceptionInvalidProjectUrl(request.projectUrl());
            }
            if (projectRepository.existsByProjectUrl(request.projectUrl())) {
                throw new ExceptionProjectUrlAlreadyExists(request.projectUrl());
            }
            projectUrl = request.projectUrl();
        }

        ProjectEntity project = ProjectEntity.builder()
                .name(request.name())
                .description(request.description())
                .shortDescription(request.shortDescription())
                .projectUrl(projectUrl)
                .isPrivateProject(request.isPrivateProject())
                .owner(owner)
                .build();

        ProjectEntity saved = projectRepository.save(project);

        createProjectInvites(saved, owner, request.memberIds());

        TxLogger.afterCommit(log, "Project created: project={}, owner={}", saved.getId(), currentUserId);
        return toResponse(saved);
    }

    private String resolveUniqueUrl(String baseSlug) {
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "projekt";
        }
        if (baseSlug.length() < 3) {
            baseSlug = baseSlug + "-projekt";
        }

        if (!projectRepository.existsByProjectUrl(baseSlug)) {
            return baseSlug;
        }

        int counter = 1;
        String candidate;
        do {
            String suffix = "-" + counter;
            int baseLength = Math.min(baseSlug.length(), 30 - suffix.length());
            candidate = baseSlug.substring(0, baseLength) + suffix;
            counter++;
        } while (projectRepository.existsByProjectUrl(candidate) && counter <= 99);

        if (projectRepository.existsByProjectUrl(candidate)) {
            throw new ExceptionProjectUrlGenerationFailed(baseSlug);
        }

        return candidate;
    }

    @Transactional
    public DeleteProjectResponse deleteProject(UUID projectId) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        projectFavoriteRepository.deleteByProjectId(projectId);
        projectViewRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);

        TxLogger.afterCommit(log, "Project deleted: project={}", projectId);
        return DeleteProjectResponse.builder()
                .projectId(projectId)
                .message("Projekt erfolgreich gelöscht.")
                .build();
    }
    @Transactional
    public ProjectResponse getProject(UUID projectId) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        projectViewRepository.save(
                ProjectViewEntity.builder()
                        .project(project)
                        .build()
        );

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse getProjectByUrl(String projectUrl) {

        ProjectEntity project = projectRepository.findByProjectUrl(projectUrl)
                .orElseThrow(() -> new ProjectNotFoundByUrlException(projectUrl));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(project.getId());
        }

        projectViewRepository.save(
                ProjectViewEntity.builder()
                        .project(project)
                        .build()
        );

        return toResponse(project);
    }

    @Transactional
    public boolean projectUrlExists(String projectUrl){
        return projectRepository.existsByProjectUrl(projectUrl);
    }

    @Transactional
    public ProjectResponse editProject(UUID projectId, UpdateProjectRequest request) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        if (request.getName() != null &&
                !request.getName().equals(project.getName()) &&
                projectRepository.existsByNameAndIdNot(request.getName(), projectId)) {
            throw new ExceptionProjectNameAlreadyExists(request.getName());
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getShortDescription() != null) {
            project.setShortDescription(request.getShortDescription());
        }
        if (request.getProjectUrl() != null) {
            if (!ProjectUrlUtils.isValidUrl(request.getProjectUrl())) {
                throw new ExceptionInvalidProjectUrl(request.getProjectUrl());
            }
            if (!request.getProjectUrl().equals(project.getProjectUrl()) &&
                    projectRepository.existsByProjectUrl(request.getProjectUrl())) {
                throw new ExceptionProjectUrlAlreadyExists(request.getProjectUrl());
            }
            project.setProjectUrl(request.getProjectUrl());
        }
        project.setPrivateProject(request.isPrivateProject());

        ProjectEntity saved = projectRepository.save(project);

        TxLogger.afterCommit(log, "Project updated: project={}", projectId);
        return toResponse(saved);
    }

    @Transactional
    public List<ProjectResponse> getProjectsByUsername(String username) {
        return projectRepository.findAllByOwnerUsernameAndDeletedAtIsNullOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<ProjectResponse> getAllProjectsByUsername(String username) {
        return projectRepository.findAllByOwnerOrMemberUsernameAndDeletedAtIsNull(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public ProjectResponse updateAllowJoinRequests(UUID projectId, boolean allow) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        project.setAllowJoinRequests(allow);
        ProjectResponse saved = toResponse(projectRepository.save(project));
        TxLogger.afterCommit(log, "AllowJoinRequests updated: project={}, allow={}", projectId, allow);
        return saved;
    }

    @Transactional
    public List<UserProfile> getProjectMembers(UUID projectId) {
        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        return projectEntity.getMembers()
                .stream()
                .toList();
    }

    @Transactional
    public void deleteProjectMember(UUID projectId, UUID memberId){
        ProjectEntity projectEntity =  projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        UserProfile member = userProfileRepository.findById(memberId)
                .orElseThrow(() -> new UserProfileNotFoundException(memberId.toString()));

        boolean memberExists = projectEntity.getMembers()
                .stream()
                .anyMatch(existingMember -> existingMember.getKeycloakId().equals(memberId));

        if (!memberExists) {
            throw new ProjectMemberNotFoundException(memberId, projectId);
        }

        projectEntity.getMembers().remove(member);
        projectRepository.save(projectEntity);
        TxLogger.afterCommit(log, "Project member removed: project={}, member={}", projectId, memberId);
    }


    private void createProjectInvites(ProjectEntity project, UserProfile owner, Set <UUID> invitedUserIds) {
        if (invitedUserIds == null || invitedUserIds.isEmpty()) {
            return;
        }

        invitedUserIds.stream()
                .filter(userId -> !userId.equals(owner.getKeycloakId()))
                .distinct()
                .forEach(userId -> projectInviteService.createProjectInvite(
                        project.getId(),
                        userId,
                        PROJECT_CREATION_INVITE_MESSAGE
                ));
    }
}
