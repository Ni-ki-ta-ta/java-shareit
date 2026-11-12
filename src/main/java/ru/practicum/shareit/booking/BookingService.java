package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto dto);

    BookingDto approve(Long userId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getByBooker(Long userId, BookingState state);

    List<BookingDto> getByOwner(Long userId, BookingState state);
}
