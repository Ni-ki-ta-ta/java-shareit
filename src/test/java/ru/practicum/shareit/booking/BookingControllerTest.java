package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ErrorHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {
    @Mock
    private BookingService bookingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(bookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void createShouldReturn201AndDelegateToService() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(1);
        BookingCreateDto request = new BookingCreateDto(start, end, 10L);
        BookingDto response = BookingDto.builder().id(100L).start(start).end(end).status("WAITING").build();
        when(bookingService.create(eq(2L), any(BookingCreateDto.class))).thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .header(BookingController.USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService).create(eq(2L), any(BookingCreateDto.class));
    }

    @Test
    void createShouldReturn400WhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(BookingController.USER_HEADER, 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void getByBookerShouldUseAllAsDefaultState() throws Exception {
        when(bookingService.getByBooker(2L, BookingState.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/bookings").header(BookingController.USER_HEADER, 2L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(bookingService).getByBooker(2L, BookingState.ALL);
    }

    @Test
    void getByOwnerShouldPassRequestedState() throws Exception {
        when(bookingService.getByOwner(1L, BookingState.WAITING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings/owner")
                        .header(BookingController.USER_HEADER, 1L)
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(bookingService).getByOwner(1L, BookingState.WAITING);
    }

    @Test
    void getByBookerShouldReturn400ForUnknownState() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(BookingController.USER_HEADER, 2L)
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }
}
