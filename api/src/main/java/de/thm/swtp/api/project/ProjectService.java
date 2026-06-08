package de.thm.swtp.api.project;


import de.thm.swtp.api.exceptionhandling.exceptions.ProjectMemberNotFoundException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectOwnerCannotBeRemovedException;
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
import java.util.*;
import java.time.*;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
    public ProjectResponse createProject(CreateProjectRequest request, String username) {

        if (projectRepository.existsByName(request.name())) {
            throw new ExceptionProjectResponse(request.name());
        }

        UserProfile owner = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for username: " + username));

        ProjectEntity project = ProjectEntity.builder()
                .name(request.name())
                .description(request.description())
                .shortDescription(request.shortDescription())
                .projectUrl(request.projectUrl())
                .isPrivateProject(request.isPrivateProject())
                .owner(owner)
                .build();

        ProjectEntity saved = projectRepository.save(project);

        createProjectInvites(saved, owner, request.memberIds());

        return toResponse(saved);
    }

    @Transactional
    public DeleteProjectResponse deleteProject(UUID projectId, String username) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for username: " + username));



        projectFavoriteRepository.deleteByProjectId(projectId);
        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);

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
                .orElseThrow(() -> new RuntimeException("Kein Projekt mit der URL " + projectUrl + " gefunden."));

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
    public ProjectResponse editProject(UUID projectId, UpdateProjectRequest request, String username) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for username: " + username));



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
            project.setProjectUrl(request.getProjectUrl());
        }
        project.setPrivateProject(request.isPrivateProject());

        ProjectEntity saved = projectRepository.save(project);

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
                        PROJECT_CREATION_INVITE_MESSAGE,
                        owner.getKeycloakId()
                ));
    }

    @Transactional
    public ProjectResponse updateAllowJoinRequests(UUID projectId, boolean allow, UUID currentUserId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        if (!project.getOwner().getKeycloakId().equals(currentUserId)) {
            throw new ExceptionProjectEditNotAllowed(currentUserId, projectId);
        }

        project.setAllowJoinRequests(allow);
        return toResponse(projectRepository.save(project));
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
    public void deleteProjectMember(UUID projectId, UUID currentUserId, UUID memberId){
        ProjectEntity projectEntity =  projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        checkProjectOwner(projectEntity, currentUserId);
        if (currentUserId.equals(memberId)) {
            throw new ProjectOwnerCannotBeRemovedException(currentUserId, projectId);
        }

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
    }



    private void checkProjectOwner(ProjectEntity projectEntity, UUID currentUserId){
        UUID ownerId = projectEntity.getOwner().getKeycloakId();

        if (!ownerId.equals(currentUserId)) {
            throw new ExceptionProjectEditNotAllowed(currentUserId, projectEntity.getId());
        }
    }
}
