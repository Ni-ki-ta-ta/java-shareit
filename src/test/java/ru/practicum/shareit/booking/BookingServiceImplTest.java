package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@mail.ru").build();
        booker = User.builder().id(2L).name("Booker").email("booker@mail.ru").build();
        item = Item.builder().id(10L).name("Drill").description("Tool")
                .available(true).owner(owner).build();
    }

    @Test
    void createShouldSaveWaitingBookingAndReturnDto() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(2);
        BookingCreateDto dto = new BookingCreateDto(start, end, 10L);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        var result = bookingService.create(2L, dto);

        assertEquals(100L, result.getId());
        assertEquals("WAITING", result.getStatus());
        assertEquals(start, result.getStart());
        assertEquals(end, result.getEnd());
        assertEquals(10L, result.getItem().getId());
        assertEquals(2L, result.getBooker().getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createShouldRejectBookingByItemOwner() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(1L,
                new BookingCreateDto(LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2), 10L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createShouldRejectUnavailableItem() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        item.setAvailable(false);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(2L,
                new BookingCreateDto(LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2), 10L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createShouldRejectStartInPastOrNow() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        LocalDateTime now = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(2L,
                new BookingCreateDto(now, now.plusDays(1), 10L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createShouldRejectEndBeforeStart() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        LocalDateTime start = LocalDateTime.now().plusDays(2);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(2L,
                new BookingCreateDto(start, start, 10L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createShouldThrowWhenItemDoesNotExist() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(2L,
                new BookingCreateDto(LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2), 404L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveShouldSetApprovedStatusForOwner() {
        Booking booking = booking(5L, Booking.Status.WAITING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var result = bookingService.approve(1L, 5L, true);

        assertEquals("APPROVED", result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void approveShouldSetRejectedStatusForOwner() {
        Booking booking = booking(5L, Booking.Status.WAITING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var result = bookingService.approve(1L, 5L, false);

        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void approveShouldRejectNonOwner() {
        Booking booking = booking(5L, Booking.Status.WAITING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.approve(2L, 5L, true));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveShouldRejectAlreadyProcessedBooking() {
        Booking booking = booking(5L, Booking.Status.APPROVED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, () -> bookingService.approve(1L, 5L, false));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getByIdShouldAllowBookerAndOwner() {
        Booking booking = booking(5L, Booking.Status.WAITING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertEquals(5L, bookingService.getById(2L, 5L).getId());
        assertEquals(5L, bookingService.getById(1L, 5L).getId());
    }

    @Test
    void getByIdShouldRejectUnrelatedUser() {
        Booking booking = booking(5L, Booking.Status.WAITING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class, () -> bookingService.getById(3L, 5L));
    }

    @Test
    void getByIdShouldThrowWhenBookingDoesNotExist() {
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.getById(2L, 404L));
    }

    @Test
    void getByBookerShouldReturnAllAndUseNewestFirstSort() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        Booking first = booking(1L, Booking.Status.WAITING);
        Booking second = booking(2L, Booking.Status.APPROVED);
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class))).thenReturn(List.of(first, second));

        var result = bookingService.getByBooker(2L, BookingState.ALL);

        assertEquals(2, result.size());
        verify(bookingRepository).findByBooker_Id(eq(2L),
                eq(Sort.by(Sort.Direction.DESC, "start")));
    }

    @Test
    void getByOwnerShouldFilterWaitingBookings() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        Booking waiting = booking(1L, Booking.Status.WAITING);
        Booking approved = booking(2L, Booking.Status.APPROVED);
        when(bookingRepository.findByItem_Owner_Id(eq(1L), any(Sort.class)))
                .thenReturn(List.of(waiting, approved));

        var result = bookingService.getByOwner(1L, BookingState.WAITING);

        assertEquals(1, result.size());
        assertEquals("WAITING", result.get(0).getStatus());
    }

    @Test
    void getByBookerShouldFilterRejectedBookings() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        Booking rejected = booking(1L, Booking.Status.REJECTED);
        Booking waiting = booking(2L, Booking.Status.WAITING);
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class)))
                .thenReturn(List.of(rejected, waiting));

        var result = bookingService.getByBooker(2L, BookingState.REJECTED);

        assertEquals(1, result.size());
        assertEquals("REJECTED", result.get(0).getStatus());
    }

    @Test
    void getByBookerShouldThrowForUnknownUser() {
        when(userService.getUserOrThrow(404L)).thenThrow(new NotFoundException("not found"));

        assertThrows(NotFoundException.class, () -> bookingService.getByBooker(404L, BookingState.ALL));
        verifyNoInteractions(bookingRepository);
    }

    private Booking booking(Long id, Booking.Status status) {
        return Booking.builder().id(id).start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2)).item(item).booker(booker).status(status).build();
    }


    @Test
    void getByBookerShouldFilterCurrentBookings() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        LocalDateTime now = LocalDateTime.now();
        Booking past = bookingWithDates(1L, Booking.Status.APPROVED,
                now.minusDays(3), now.minusDays(2));
        Booking current = bookingWithDates(2L, Booking.Status.APPROVED,
                now.minusHours(1), now.plusHours(1));
        Booking future = bookingWithDates(3L, Booking.Status.APPROVED,
                now.plusDays(1), now.plusDays(2));
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class)))
                .thenReturn(List.of(past, current, future));

        var result = bookingService.getByBooker(2L, BookingState.CURRENT);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getByBookerShouldFilterPastBookings() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        LocalDateTime now = LocalDateTime.now();
        Booking past = bookingWithDates(1L, Booking.Status.APPROVED,
                now.minusDays(3), now.minusDays(2));
        Booking current = bookingWithDates(2L, Booking.Status.APPROVED,
                now.minusHours(1), now.plusHours(1));
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class)))
                .thenReturn(List.of(past, current));

        var result = bookingService.getByBooker(2L, BookingState.PAST);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getByBookerShouldFilterFutureBookings() {
        when(userService.getUserOrThrow(2L)).thenReturn(booker);
        LocalDateTime now = LocalDateTime.now();
        Booking current = bookingWithDates(1L, Booking.Status.APPROVED,
                now.minusHours(1), now.plusHours(1));
        Booking future = bookingWithDates(2L, Booking.Status.APPROVED,
                now.plusDays(1), now.plusDays(2));
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class)))
                .thenReturn(List.of(current, future));

        var result = bookingService.getByBooker(2L, BookingState.FUTURE);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void getByOwnerShouldReturnAllBookingsRegardlessOfStatusAndDates() {
        when(userService.getUserOrThrow(1L)).thenReturn(owner);
        LocalDateTime now = LocalDateTime.now();
        Booking past = bookingWithDates(1L, Booking.Status.REJECTED,
                now.minusDays(3), now.minusDays(2));
        Booking current = bookingWithDates(2L, Booking.Status.WAITING,
                now.minusHours(1), now.plusHours(1));
        Booking future = bookingWithDates(3L, Booking.Status.APPROVED,
                now.plusDays(1), now.plusDays(2));
        when(bookingRepository.findByItem_Owner_Id(eq(1L), any(Sort.class)))
                .thenReturn(List.of(past, current, future));

        var result = bookingService.getByOwner(1L, BookingState.ALL);

        assertEquals(3, result.size());
    }

    private Booking bookingWithDates(Long id, Booking.Status status,
                                     LocalDateTime start, LocalDateTime end) {
        return Booking.builder()
                .id(id)
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(status)
                .build();
    }
}
