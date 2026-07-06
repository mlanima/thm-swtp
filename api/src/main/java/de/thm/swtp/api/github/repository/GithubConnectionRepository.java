package de.thm.swtp.api.github.repository;

import de.thm.swtp.api.github.entity.GithubConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GithubConnectionRepository extends JpaRepository<GithubConnectionEntity, UUID> {
}
