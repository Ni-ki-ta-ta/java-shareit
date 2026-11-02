package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRequireOwnerHeaderToCreateItem() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Drill", "Powerful drill", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectItemForUnknownOwner() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 999999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Drill", "Powerful drill", true)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectItemWithoutAvailableField() throws Exception {
        long ownerId = createUser("Owner", uniqueEmail());

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Powerful drill\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectItemWithBlankNameOrDescription() throws Exception {
        long ownerId = createUser("Owner", uniqueEmail());

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("", "Description", true)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Name", "", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForUnknownItem() throws Exception {
        mockMvc.perform(get("/items/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateGetAndSearchItemsOnlyWhenAvailable() throws Exception {
        long ownerId = createUser("Owner", uniqueEmail());

        String token = UUID.randomUUID().toString().replace("-", "");
        String searchWord = "drill" + token;

        String availableJson = mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(searchWord, "A tool", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.available").value(true))
                .andReturn().getResponse().getContentAsString();

        long itemId = objectMapper.readTree(availableJson).get("id").asLong();

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("unavailable" + token, "Another tool", false)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(searchWord));

        mockMvc.perform(get("/items")
                        .header(USER_HEADER, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/items/search")
                        .param("text", searchWord.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value(searchWord));

        mockMvc.perform(get("/items/search")
                        .param("text", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldAllowOwnerToPartiallyUpdateItemAndRejectOtherOwner() throws Exception {
        long ownerId = createUser("Owner", uniqueEmail());
        long otherId = createUser("Other", uniqueEmail());

        String response = mockMvc.perform(post("/items")
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Original", "Original description", true)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long itemId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/items/{id}", itemId)
                        .header(USER_HEADER, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Changed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Changed"))
                .andExpect(jsonPath("$.description").value("Original description"))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(patch("/items/{id}", itemId)
                        .header(USER_HEADER, otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Not allowed\"}"))
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

    private String itemJson(String name, String description, boolean available) {
        return "{\"name\":\"" + name + "\",\"description\":\"" + description
                + "\",\"available\":" + available + "}";
    }
}
