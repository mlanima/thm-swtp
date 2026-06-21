package de.thm.swtp.api.projectPost.entity;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectPost.domain.PostContentFormat;
import de.thm.swtp.api.projectPost.domain.ProjectPostStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** JPA entity representing a project-post in the database.*/

@Entity
@Table(name="project_posts")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPostEntity {

    /** Unique identifier of the post.*/
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The project where the post belongs to.*/
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    /** Author of the post.*/
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_keycloak_id", nullable = false)
    private UserProfile author;

    /** Title of the post.*/
    @Column(nullable = false)
    private String title;

    /** Content of the post.*/
    @Column(nullable = false)
    private String content;

    /** Status of the Post. See {@link ProjectPostStatus}
     * Default status is set to draft.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectPostStatus status = ProjectPostStatus.DRAFT;

    /** The content-format of the post. See {@link PostContentFormat}.
     * Default format is set to plain-text.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false)
    @Builder.Default
    private PostContentFormat contentFormat = PostContentFormat.PLAIN_TEXT;

    /** Timestamp when the post was published.*/
    @Column
    private LocalDateTime publishedAt;

    /** Timestamp when the post was archived.*/
    @Column
    private LocalDateTime archivedAt;

    /** Timestamp when the post was created.*/
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the post was last updated.*/
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
