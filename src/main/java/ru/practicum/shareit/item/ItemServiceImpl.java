package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.dto.BookingBookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestRepository itemRequestRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto dto) {
        log.info("Создание вещи пользователем id={}", userId);
        User owner = userService.getUserOrThrow(userId);
        ItemRequest request = findRequest(dto.getRequestId());
        Item saved = itemRepository.save(ItemMapper.toItem(dto, owner, request));
        return ItemMapper.toItemDto(saved);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto dto) {
        log.info("Обновление вещи id={} пользователем id={}", itemId, userId);
        userService.getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        if (!item.getOwner().getId().equals(userId))
            throw new NotFoundException("Изменять вещь может только её владелец");
        if (dto.getName() != null) {
            validateRequired(dto.getName(), "Название вещи не должно быть пустым");
            item.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            validateRequired(dto.getDescription(), "Описание вещи не должно быть пустым");
            item.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) item.setAvailable(dto.getAvailable());
        if (dto.getRequestId() != null) item.setRequest(findRequest(dto.getRequestId()));
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemWithBookingsDto getById(Long itemId) {
        Item item = getItemOrThrow(itemId);
        ItemWithBookingsDto dto = new ItemWithBookingsDto(ItemMapper.toItemDto(item));
        dto.setComments(getComments(itemId));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemWithBookingsDto> getByOwner(Long userId) {
        userService.getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        return itemRepository.findAllByOwnerIdOrderById(userId).stream().map(item -> {
            ItemWithBookingsDto dto = new ItemWithBookingsDto(ItemMapper.toItemDto(item));
            fillBookingMetadata(item.getId(), dto, now);
            dto.setComments(getComments(item.getId()));
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) return List.of();
        return itemRepository.search(text.trim()).stream().map(ItemMapper::toItemDto).toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, String text) {
        User author = userService.getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        LocalDateTime now = LocalDateTime.now();
        if (!bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(itemId, userId, Booking.Status.APPROVED, now)) {
            throw new IllegalArgumentException("Оставить отзыв можно только после завершённого бронирования");
        }
        Comment saved = commentRepository.save(Comment.builder().text(text).item(item).author(author).created(now).build());
        return toCommentDto(saved);
    }


    private void fillBookingMetadata(Long itemId, ItemWithBookingsDto dto, LocalDateTime now) {
        List<Booking> previous = bookingRepository.findByItem_IdAndStatusAndEndIsBefore(itemId,
                Booking.Status.APPROVED, now,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "start"));
        List<Booking> next = bookingRepository.findByItem_IdAndStatusAndStartIsAfter(itemId,
                Booking.Status.APPROVED, now,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "start"));
        if (!previous.isEmpty()) dto.setLastBooking(toBookingDto(previous.get(0)));
        if (!next.isEmpty()) dto.setNextBooking(toBookingDto(next.get(0)));
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findByItem_IdOrderByCreatedDesc(itemId).stream().map(this::toCommentDto).toList();
    }

    private CommentDto toCommentDto(Comment c) {
        return CommentDto.builder().id(c.getId()).text(c.getText()).authorName(c.getAuthor().getName()).created(c.getCreated()).build();
    }

    private BookingDto toBookingDto(Booking b) {
        return BookingDto.builder().id(b.getId()).start(b.getStart()).end(b.getEnd())
                .item(new BookingItemDto(b.getItem().getId(), b.getItem().getName()))
                .booker(new BookingBookerDto(b.getBooker().getId(), b.getBooker().getName())).status(b.getStatus().name()).build();
    }

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new NotFoundException("Вещь с id=" + id + " не найдена"));
    }

    private ItemRequest findRequest(Long id) {
        if (id == null) return null;
        return itemRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("Запрос с id=" + id + " не найден"));
    }

    private void validateRequired(String value, String message) {
        if (value.isBlank()) throw new IllegalArgumentException(message);
    }
}
