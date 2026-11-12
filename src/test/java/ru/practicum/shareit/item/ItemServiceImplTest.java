package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserService userService;
    @Mock
    private ItemRequestRepository itemRequestRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
        booker = User.builder().id(2L).name("Booker").email("booker@mail.ru").build();
        item = Item.builder().id(10L).name("Drill").description("Power drill")
                .available(true).owner(owner).build();
    }

    @Test
    void createShouldSaveItemForExistingOwner() {
        ItemDto dto = ItemDto.builder().name("Drill").description("Power drill").available(true).build();
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ItemDto result = itemService.create(1L, dto);

        assertEquals(10L, result.getId());
        assertEquals("Drill", result.getName());
        assertEquals("Power drill", result.getDescription());
        assertTrue(result.getAvailable());
    }

    @Test
    void createShouldThrowWhenReferencedRequestDoesNotExist() {
        ItemDto dto = ItemDto.builder().name("Drill").description("Tool")
                .available(true).requestId(404L).build();
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(1L, dto));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ItemDto patch = ItemDto.builder().name("New name").available(false).build();

        ItemDto result = itemService.update(1L, 10L, patch);

        assertEquals("New name", result.getName());
        assertEquals("Power drill", result.getDescription());
        assertFalse(result.getAvailable());
    }

    @Test
    void updateShouldRejectNonOwner() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class, () -> itemService.update(2L, 10L,
                ItemDto.builder().name("Changed").build()));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateShouldRejectBlankName() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> itemService.update(1L, 10L,
                ItemDto.builder().name("  ").build()));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateShouldRejectBlankDescription() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> itemService.update(1L, 10L,
                ItemDto.builder().description(" ").build()));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowWhenItemDoesNotExist() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.update(1L, 404L,
                ItemDto.builder().name("New").build()));
    }

    @Test
    void getByIdShouldReturnComments() {
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItem_IdOrderByCreatedDesc(10L)).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getById(10L);

        assertEquals(10L, result.getId());
        assertNotNull(result.getComments());
        assertTrue(result.getComments().isEmpty());
    }

    @Test
    void getByIdShouldThrowWhenItemDoesNotExist() {
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getById(404L));
    }

    @Test
    void getByOwnerShouldReturnItemsAndLoadBookingMetadataAndComments() {
        Booking previous = booking(11L, LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2));
        Booking next = booking(12L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3));
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findAllByOwnerIdOrderById(1L)).thenReturn(List.of(item));
        when(bookingRepository.findByItem_IdAndStatusAndEndIsBefore(eq(10L), eq(Booking.Status.APPROVED),
                any(LocalDateTime.class), any())).thenReturn(List.of(previous));
        when(bookingRepository.findByItem_IdAndStatusAndStartIsAfter(eq(10L), eq(Booking.Status.APPROVED),
                any(LocalDateTime.class), any())).thenReturn(List.of(next));
        when(commentRepository.findByItem_IdOrderByCreatedDesc(10L)).thenReturn(List.of());

        List<ItemWithBookingsDto> result = itemService.getByOwner(1L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getLastBooking().getId());
        assertEquals(12L, result.get(0).getNextBooking().getId());
        assertNotNull(result.get(0).getComments());
    }

    @Test
    void searchShouldReturnEmptyForBlankTextWithoutQueryingRepository() {
        assertTrue(itemService.search("  ").isEmpty());
        verifyNoInteractions(itemRepository);
    }

    @Test
    void searchShouldTrimTextBeforeRepositoryCall() {
        when(itemRepository.search("drill")).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search("  drill  ");

        assertEquals(1, result.size());
        assertEquals("Drill", result.get(0).getName());
        verify(itemRepository).search("drill");
    }

    @Test
    void addCommentShouldSaveCommentAfterCompletedApprovedBooking() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L), eq(2L), eq(Booking.Status.APPROVED), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(50L);
            return comment;
        });

        var result = itemService.addComment(2L, 10L, "Good drill");

        assertEquals(50L, result.getId());
        assertEquals("Good drill", result.getText());
        assertEquals("Booker", result.getAuthorName());
        assertNotNull(result.getCreated());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addCommentShouldRejectWhenThereIsNoCompletedApprovedBooking() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                eq(10L), eq(2L), eq(Booking.Status.APPROVED), any(LocalDateTime.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> itemService.addComment(2L, 10L, "Review"));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentShouldThrowWhenItemDoesNotExist() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.addComment(2L, 404L, "Review"));
        verifyNoInteractions(commentRepository);
    }

    private Booking booking(Long id, LocalDateTime start, LocalDateTime end) {
        return Booking.builder().id(id).start(start).end(end).item(item).booker(booker)
                .status(Booking.Status.APPROVED).build();
    }
}
