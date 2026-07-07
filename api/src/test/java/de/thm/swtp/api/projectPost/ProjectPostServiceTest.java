package de.thm.swtp.api.projectPost;

import de.thm.swtp.api.discord.service.DiscordPostSyncService;
import de.thm.swtp.api.discord.stream.DiscordEventPublisher;
import de.thm.swtp.api.exceptionhandling.exceptions.InvalidProjectPostException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectPostNotFoundException;
import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectPost.domain.PostContentFormat;
import de.thm.swtp.api.projectPost.domain.ProjectPost;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.projectPost.entity.ProjectPostEntity;
import de.thm.swtp.api.projectPost.repository.ProjectPostRepository;
import de.thm.swtp.api.projectPost.service.ProjectPostService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectPostServiceTest {

    @Mock
    private ProjectPostRepository projectPostRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DiscordEventPublisher discordEventPublisher;

    @Mock
    private DiscordPostSyncService discordPostSyncService;

    private ProjectPostService service;

    private UUID projectId;
    private UUID authorId;
    private ProjectEntity project;
    private UserProfile author;

    @BeforeEach
    void setUp() {
        service = new ProjectPostService(projectPostRepository, projectRepository,
                userProfileRepository, discordEventPublisher, discordPostSyncService);

        lenient().when(discordPostSyncService.getDiscordMessageId(any())).thenReturn(Optional.empty());

        projectId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        project = ProjectEntity.builder()
                .id(projectId)
                .name("test project")
                .build();

        author = UserProfile.builder()
                .keycloakId(authorId)
                .username("Alice")
                .build();
    }

    // ── createProjectPost ──

    @Test
    void shouldCreatePostSuccessfully() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(authorId)).thenReturn(Optional.of(author));

        var saved = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("post title")
                .content("post content")
                .status(ProjectPostStatus.DRAFT)
                .contentFormat(PostContentFormat.MARKDOWN)
                .build();
        when(projectPostRepository.saveAndFlush(any())).thenReturn(saved);

        ProjectPost result = service.createProjectPost(projectId, authorId,
                "post title", "post content",
                PostContentFormat.MARKDOWN, ProjectPostStatus.DRAFT);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldThrowWhenStatusNull() {
        assertThatThrownBy(() -> service.createProjectPost(projectId, authorId,
                "title", "content",
                PostContentFormat.PLAIN_TEXT, null))
                .isInstanceOf(InvalidProjectPostException.class)
                .hasMessage("Post status must not be null.");
        verifyNoInteractions(projectRepository);
    }

    @Test
    void shouldThrowWhenStatusArchived() {
        assertThatThrownBy(() -> service.createProjectPost(projectId, authorId,
                "title", "content",
                PostContentFormat.PLAIN_TEXT, ProjectPostStatus.ARCHIVED))
                .isInstanceOf(InvalidProjectPostException.class)
                .hasMessage("Post cannot be created as archived.");
    }

    @Test
    void shouldThrowWhenContentFormatNull() {
        assertThatThrownBy(() -> service.createProjectPost(projectId, authorId,
                "title", "content",
                null, ProjectPostStatus.DRAFT))
                .isInstanceOf(InvalidProjectPostException.class)
                .hasMessage("Post content format must not be null.");
    }

    @Test
    void shouldThrowWhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProjectPost(projectId, authorId,
                "title", "content",
                PostContentFormat.PLAIN_TEXT, ProjectPostStatus.DRAFT))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAuthorNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userProfileRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProjectPost(projectId, authorId,
                "title", "content",
                PostContentFormat.PLAIN_TEXT, ProjectPostStatus.DRAFT))
                .isInstanceOf(UserProfileNotFoundException.class);
    }

    // ── publishProjectPost ──

    @Test
    void shouldPublishDraftPost() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.DRAFT)
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));
        when(projectPostRepository.save(postEntity)).thenReturn(postEntity);

        var result = service.publishProjectPost(projectId, postEntity.getId());

        assertThat(result.getStatus()).isEqualTo(ProjectPostStatus.PUBLISHED);
    }

    @Test
    void shouldReturnPublishedPostWhenAlreadyPublished() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.PUBLISHED)
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));

        service.publishProjectPost(projectId, postEntity.getId());

        verify(projectPostRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPublishPostNotFound() {
        var postId = UUID.randomUUID();
        when(projectPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishProjectPost(projectId, postId))
                .isInstanceOf(ProjectPostNotFoundException.class);
    }

    @Test
    void shouldThrowWhenPublishPostNotBelongToProject() {
        var otherProject = ProjectEntity.builder().id(UUID.randomUUID()).build();
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(otherProject)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.DRAFT)
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));

        assertThatThrownBy(() -> service.publishProjectPost(projectId, postEntity.getId()))
                .isInstanceOf(ProjectPostNotFoundException.class);
    }

    // ── archiveProjectPost ──

    @Test
    void shouldArchivePublishedPost() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.PUBLISHED)
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));
        when(projectPostRepository.save(postEntity)).thenReturn(postEntity);

        var result = service.archiveProjectPost(projectId, postEntity.getId());

        assertThat(result.getStatus()).isEqualTo(ProjectPostStatus.ARCHIVED);
    }

    @Test
    void shouldReturnArchivedPostWhenAlreadyArchived() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.ARCHIVED)
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));

        service.archiveProjectPost(projectId, postEntity.getId());

        verify(projectPostRepository, never()).save(any());
    }

    // ── deleteProjectPost ──

    @Test
    void shouldDeletePostWhenBelongsToProject() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));

        service.deleteProjectPost(projectId, postEntity.getId());

        verify(projectPostRepository).delete(postEntity);
    }

    @Test
    void shouldThrowWhenDeletePostNotBelongToProject() {
        var otherProject = ProjectEntity.builder().id(UUID.randomUUID()).build();
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(otherProject)
                .author(author)
                .title("title")
                .content("content")
                .build();

        when(projectPostRepository.findById(postEntity.getId())).thenReturn(Optional.of(postEntity));

        assertThatThrownBy(() -> service.deleteProjectPost(projectId, postEntity.getId()))
                .isInstanceOf(ProjectPostNotFoundException.class);
    }

    // ── getPublishedPostsForProject ──

    @Test
    void shouldReturnPublishedPosts() {
        var postEntity = ProjectPostEntity.builder()
                .id(UUID.randomUUID())
                .project(project)
                .author(author)
                .title("title")
                .content("content")
                .status(ProjectPostStatus.PUBLISHED)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectPostRepository.findAllByProjectIdAndStatusOrderByPublishedAtDesc(
                projectId, ProjectPostStatus.PUBLISHED))
                .thenReturn(List.of(postEntity));

        var results = service.getPublishedPostsForProject(projectId);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(postEntity.getId());
    }

    @Test
    void shouldThrowWhenGetPostsForNonexistentProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedPostsForProject(projectId))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
