package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingBookerDto;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "start");
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto dto) {
        User booker = userService.getUserOrThrow(userId);
        Item item = itemRepository.findById(dto.getItemId()).orElseThrow(() -> new NotFoundException("Вещь не найдена"));
        if (item.getOwner().getId().equals(userId))
            throw new IllegalArgumentException("Нельзя бронировать собственную вещь");
        if (!item.isAvailable()) throw new IllegalArgumentException("Вещь недоступна для бронирования");
        LocalDateTime now = LocalDateTime.now();
        if (dto.getStart() == null || dto.getEnd() == null || !dto.getStart().isAfter(now) || !dto.getEnd().isAfter(dto.getStart()))
            throw new IllegalArgumentException("Укажите корректные даты бронирования: начало в будущем, окончание после начала");
        Booking booking = Booking.builder().start(dto.getStart()).end(dto.getEnd()).item(item).booker(booker).status(Booking.Status.WAITING).build();
        return toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = getBooking(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "Подтвердить бронирование может только владелец вещи"
            );
        }

        if (booking.getStatus() != Booking.Status.WAITING) {
            throw new IllegalArgumentException(
                    "Можно изменить только ожидающее бронирование"
            );
        }

        booking.setStatus(approved ? Booking.Status.APPROVED : Booking.Status.REJECTED);
        return toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getById(Long userId, Long bookingId) {
        Booking b = getBooking(bookingId);
        if (!b.getBooker().getId().equals(userId) && !b.getItem().getOwner().getId().equals(userId))
            throw new NotFoundException("Просматривать бронирование могут только его автор и владелец вещи");
        return toDto(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getByBooker(Long userId, BookingState state) {
        userService.getUserOrThrow(userId);
        return select(bookingRepository.findByBooker_Id(userId, NEWEST), state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getByOwner(Long userId, BookingState state) {
        userService.getUserOrThrow(userId);
        return select(bookingRepository.findByItem_Owner_Id(userId, NEWEST), state);
    }

    private List<BookingDto> select(List<Booking> all, BookingState state) {
        LocalDateTime now = LocalDateTime.now();
        return all.stream().filter(b -> switch (state) {
            case ALL -> true;
            case CURRENT -> !b.getStart().isAfter(now) && !b.getEnd().isBefore(now);
            case PAST -> b.getEnd().isBefore(now);
            case FUTURE -> b.getStart().isAfter(now);
            case WAITING -> b.getStatus() == Booking.Status.WAITING;
            case REJECTED -> b.getStatus() == Booking.Status.REJECTED;
        }).map(this::toDto).toList();
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Бронирование с id=" + id + " не найдено"));
    }

    private BookingDto toDto(Booking b) {
        return BookingDto.builder().id(b.getId()).start(b.getStart()).end(b.getEnd())
                .item(new BookingItemDto(b.getItem().getId(), b.getItem().getName()))
                .booker(new BookingBookerDto(b.getBooker().getId(), b.getBooker().getName())).status(b.getStatus().name()).build();
    }
}
