package de.thm.swtp.api.project;


import de.thm.swtp.api.project.dto.request.CreateProjectRequest;
import de.thm.swtp.api.project.dto.response.ProjectResponse;
import de.thm.swtp.api.project.exception.ExceptionOwnerNotFound;
import de.thm.swtp.api.project.exception.ExceptionProjectResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;

import lombok.*;
import java.util.*;


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
}


