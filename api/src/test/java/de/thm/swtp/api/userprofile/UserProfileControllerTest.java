package de.thm.swtp.api.userprofile;

import de.thm.swtp.api.config.GlobalExceptionHandler;
import de.thm.swtp.api.config.KeycloakJwtConverter;
import de.thm.swtp.api.config.SecurityConfig;
import de.thm.swtp.api.userprofile.controller.UserProfileController;
import de.thm.swtp.api.userprofile.dto.UserProfileResponse;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.mapper.UserProfileMapper;
import de.thm.swtp.api.userprofile.service.UserProfileService;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.users.entity.User;
import de.thm.swtp.api.users.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, KeycloakJwtConverter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com"
})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserProfileMapper userProfileMapper;

    private static final String USER_ID = "kc-uuid-123";
    private static final String BASE_URL = "/api/users/" + USER_ID + "/profile";

    private UserProfile sampleProfile() {
        User user = User.builder()
                .keycloakId(USER_ID)
                .username("testuser")
                .email("test@example.com")
                .build();
        return UserProfile.builder()
                .user(user)
                .about("I love coding")
                .experience("3 years Java")
                .build();
    }

    @Test
    void getProfile_authenticated_returnsOk() throws Exception {
        UserProfile profile = sampleProfile();
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(USER_ID).about("I love coding").experience("3 years Java").build();

        when(userProfileService.getProfile(USER_ID)).thenReturn(profile);
        when(userProfileMapper.toResponse(profile)).thenReturn(response);

        mockMvc.perform(get(BASE_URL).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.about").value("I love coding"))
                .andExpect(jsonPath("$.experience").value("3 years Java"));
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_unknownUser_returns404() throws Exception {
        when(userProfileService.getProfile(USER_ID))
                .thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(get(BASE_URL).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_authenticated_returnsOk() throws Exception {
        UserProfile updated = UserProfile.builder()
                .user(User.builder().keycloakId(USER_ID).username("testuser").email("test@example.com").build())
                .about("Updated bio")
                .experience("5 years Java")
                .build();
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(USER_ID).about("Updated bio").experience("5 years Java").build();

        when(userProfileService.updateProfile(eq(USER_ID), any(), any())).thenReturn(updated);
        when(userProfileMapper.toResponse(updated)).thenReturn(response);

        mockMvc.perform(put(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"about\":\"Updated bio\",\"experience\":\"5 years Java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.about").value("Updated bio"))
                .andExpect(jsonPath("$.experience").value("5 years Java"));
    }

    @Test
    void updateProfile_unknownUser_returns404() throws Exception {
        when(userProfileService.updateProfile(eq(USER_ID), any(), any()))
                .thenThrow(new UserNotFoundException(USER_ID));

        mockMvc.perform(put(BASE_URL)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"about\":\"bio\",\"experience\":\"exp\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProfile_authenticated_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL).with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProfile_unknownUser_returns404() throws Exception {
        doThrow(new UserProfileNotFoundException(USER_ID))
                .when(userProfileService).deleteProfile(USER_ID);

        mockMvc.perform(delete(BASE_URL).with(jwt()))
                .andExpect(status().isNotFound());
    }
}
