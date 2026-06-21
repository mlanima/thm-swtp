package de.thm.swtp.api.userAccount.repository;

import de.thm.swtp.api.userAccount.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {
}
