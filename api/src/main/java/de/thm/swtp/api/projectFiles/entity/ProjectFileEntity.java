package de.thm.swtp.api.projectFiles.entity;

import de.thm.swtp.api.project.ProjectEntity;
import de.thm.swtp.api.projectFiles.domain.FileVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255, unique = true)
    private String storageName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    @Builder.Default
    private FileVisibility visibility = FileVisibility.PUBLIC;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
