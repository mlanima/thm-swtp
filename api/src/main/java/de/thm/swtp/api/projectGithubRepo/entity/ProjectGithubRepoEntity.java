package de.thm.swtp.api.projectGithubRepo.entity;

import de.thm.swtp.api.project.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** The GitHub repository linked to a project (one repo per project). */
@Entity
@Table(name = "project_github_repos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectGithubRepoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private ProjectEntity project;

    @Column(nullable = false, length = 100)
    private String repoOwner;

    @Column(nullable = false, length = 150)
    private String repoName;

    @Column(nullable = false)
    private UUID linkedByKeycloakId;

    @Column(length = 255)
    private String defaultBranch;

    @Column(nullable = false)
    @Builder.Default
    private boolean showReadme = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
