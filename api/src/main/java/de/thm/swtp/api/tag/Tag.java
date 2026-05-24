package de.thm.swtp.api.tag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.thm.swtp.api.profile.Profile;
import de.thm.swtp.api.project.Project;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    @Id
    private String name;

    @JsonIgnore
    @ManyToMany
    private Set<Profile> profiles;

    @JsonIgnore
    @ManyToMany
    private Set<Project> projects;

    public Tag(String name) {
        this.name = name;
    }
}
