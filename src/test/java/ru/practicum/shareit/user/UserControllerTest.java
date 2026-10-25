package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndGetUser() throws Exception {
        String email = uniqueEmail();

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Nikita", email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Nikita"))
                .andExpect(jsonPath("$.email").value(email))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldRejectUserWithoutEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nikita\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Nikita", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        createUser("First", email);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("Second", email)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldPartiallyUpdateUserAndPreserveUnchangedFields() throws Exception {
        String email = uniqueEmail();
        long id = createUser("Before", email);

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldUpdateEmailWhenItIsUnique() throws Exception {
        long id = createUser("Email owner", uniqueEmail());
        String newEmail = uniqueEmail();

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    void shouldRejectUpdatingEmailToAnotherUsersEmail() throws Exception {
        String sharedEmail = uniqueEmail();
        long firstId = createUser("First", sharedEmail);
        long secondId = createUser("Second", uniqueEmail());

        mockMvc.perform(patch("/users/{id}", secondId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + sharedEmail + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/users/{id}", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(sharedEmail));
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        mockMvc.perform(get("/users/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldListAndDeleteUser() throws Exception {
        long id = createUser("Delete me", uniqueEmail());

        String usersJson = mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode users = objectMapper.readTree(usersJson);
        boolean found = false;

        for (JsonNode user : users) {
            if (user.get("id").asLong() == id) {
                found = true;
                break;
            }
        }

        assertTrue(found, "Созданный пользователь должен присутствовать в списке");

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    private long createUser(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "") + "@example.org";
    }

    private String userJson(String name, String email) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}";
    }
}
