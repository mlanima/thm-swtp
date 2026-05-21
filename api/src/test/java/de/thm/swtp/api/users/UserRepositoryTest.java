package de.thm.swtp.api.users;

import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.users.entity.User;
import de.thm.swtp.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private User buildUser() {
        return User.builder()
                .keycloakId("kc-uuid-123")
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void savingUser_autoCreatesProfile() {
        User saved = userRepository.save(buildUser());

        assertThat(saved.getId()).isNotNull();
        assertThat(userProfileRepository.findByUser(saved)).isPresent();
    }

    @Test
    void findByKeycloakId_returnsUser() {
        userRepository.save(buildUser());

        assertThat(userRepository.findByKeycloakId("kc-uuid-123")).isPresent();
    }

    @Test
    void findByUsername_returnsUser() {
        userRepository.save(buildUser());

        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }

    @Test
    void findByEmail_returnsUser() {
        userRepository.save(buildUser());

        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }

    @Test
    void findByKeycloakId_unknownId_returnsEmpty() {
        assertThat(userRepository.findByKeycloakId("does-not-exist")).isEmpty();
    }

    @Test
    void deletingUser_alsoDeletesProfile() {
        User saved = userRepository.save(buildUser());
        Long userId = saved.getId();

        userRepository.delete(saved);

        assertThat(userProfileRepository.findById(userId)).isEmpty();
    }
}
