package de.thm.swtp.api.project;


import de.thm.swtp.api.project.dto.request.*;
import de.thm.swtp.api.project.dto.response.*;
import de.thm.swtp.api.project.exception.*;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;

import lombok.*;
import java.util.*;
import java.time.*;


import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProfileRepository userProfileRepository;

    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {


        if (projectRepository.existsByName(request.name())) {
            throw new ExceptionProjectResponse(request.name());
        }


        UserProfile owner = userProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ExceptionOwnerNotFound(ownerId));


        Set<UserProfile> members = new HashSet<>(
                userProfileRepository.findAllById(request.memberIds())
        );


        ProjectEntity project = ProjectEntity.builder()
                .name(request.name())
                .description(request.description())
                .projectUrl(request.projectUrl())
                .isPrivateProject(request.isPrivateProject())
                .owner(owner)
                .members(members)
                .build();

        ProjectEntity saved = projectRepository.save(project);


        return ProjectResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .projectUrl(saved.getProjectUrl())
                .isPrivateProject(saved.isPrivateProject())
                .ownerId(saved.getOwner().getKeycloakId())
                .memberIds(request.memberIds())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    public DeleteProjectResponse deleteProject(UUID projectId, UUID requestingUserId) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        if (!project.getOwner().getKeycloakId().equals(requestingUserId)) {
            throw new ExceptionProjectDeleteNotAllowed(requestingUserId, projectId);
        }

        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);

        return DeleteProjectResponse.builder()
                .projectId(projectId)
                .message("Projekt erfolgreich gelöscht.")
                .build();
    }

    public ProjectResponse getProject(UUID projectId) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .projectUrl(project.getProjectUrl())
                .isPrivateProject(project.isPrivateProject())
                .ownerId(project.getOwner().getKeycloakId())
                .memberIds(project.getMembers().stream()
                        .map(m -> m.getKeycloakId())
                        .collect(java.util.stream.Collectors.toSet()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public ProjectResponse editProject(UUID projectId, UpdateProjectRequest request, UUID requestingUserId) {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ExceptionProjectNotFound(projectId));

        if (project.getDeletedAt() != null) {
            throw new ExceptionProjectAlreadyDeleted(projectId);
        }

        if (!project.getOwner().getKeycloakId().equals(requestingUserId)) {
            throw new ExceptionProjectEditNotAllowed(requestingUserId, projectId);
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
        if (request.getProjectUrl() != null) {
            project.setProjectUrl(request.getProjectUrl());
        }
        project.setPrivateProject(request.isPrivateProject());

        ProjectEntity saved = projectRepository.save(project);

        return ProjectResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .projectUrl(saved.getProjectUrl())
                .isPrivateProject(saved.isPrivateProject())
                .ownerId(saved.getOwner().getKeycloakId())
                .memberIds(saved.getMembers().stream()
                        .map(m -> m.getKeycloakId())
                        .collect(java.util.stream.Collectors.toSet()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}


