package de.thm.swtp.api.project;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name="project_url", nullable = false, length = 30)
    private String projectUrl;

    @Column(name="is_private", nullable = false)
    private boolean isPrivateProject;

    // Misses Join to UserEntity for the project-owner. ManyToOne should work.

    // Misses Join to UserEntity for project-members. ManyToMany should work.

    // Misses Join to TagEntity for project-tags. ManyToMany should work.

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
