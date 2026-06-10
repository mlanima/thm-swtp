package de.thm.swtp.api.projectView.service;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectView.entity.ProjectViewEntity;
import de.thm.swtp.api.projectView.repository.ProjectViewRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectViewServiceTest {

    @Mock
    private ProjectViewRepository projectViewRepository;

    @InjectMocks
    private ProjectViewService projectViewService;

    @Test
    void shouldAddView() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("owner")
                .email("owner@mni.thm.de")
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .id(UUID.randomUUID())
                .name("Testprojekt")
                .projectUrl("testprojekt")
                .owner(owner)
                .build();

        projectViewService.addView(project);

        verify(projectViewRepository).save(any(ProjectViewEntity.class));
    }

    @Test
    void shouldCountViews() {
        UUID projectId = UUID.randomUUID();

        when(projectViewRepository.countByProjectId(projectId)).thenReturn(15L);

        long views = projectViewService.countViews(projectId);

        assertThat(views).isEqualTo(15L);
        verify(projectViewRepository).countByProjectId(projectId);
    }
}