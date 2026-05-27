package de.thm.swtp.api.project;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectEntityTest {

    @Test
    void shouldBuildProject() {
        UserProfile owner = UserProfile.builder()
                .keycloakId(UUID.randomUUID())
                .username("testowner")
                .email("testowner@mni.thm.de")
                .build();

        ProjectEntity project = ProjectEntity.builder()
                .name("Testprojekt")
                .description("Eine Beschreibung")
                .projectUrl("testprojekt")
                .isPrivateProject(false)
                .owner(owner)
                .build();

        assertThat(project.getName()).isEqualTo("Testprojekt");
        assertThat(project.getOwner()).isEqualTo(owner);
        assertThat(project.getMembers()).isNotNull();
        assertThat(project.isPrivateProject()).isFalse();
    }
}