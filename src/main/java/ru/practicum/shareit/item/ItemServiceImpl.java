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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Изменять вещь может только её владелец");
        }

        if (dto.getName() != null) {
            validateRequired(dto.getName(), "Название вещи не должно быть пустым");
            item.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            validateRequired(dto.getDescription(), "Описание вещи не должно быть пустым");
            item.setDescription(dto.getDescription());
        }

        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }

        if (dto.getRequestId() != null) {
            item.setRequest(findRequest(dto.getRequestId()));
        }

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemWithBookingsDto getById(Long itemId) {
        Item item = getItemOrThrow(itemId);
        ItemWithBookingsDto dto = new ItemWithBookingsDto(ItemMapper.toItemDto(item));
        dto.setComments(getComments(itemId));
        return dto;
    }

    @Override
    public List<ItemWithBookingsDto> getByOwner(Long userId) {
        userService.getUserOrThrow(userId);

        List<Item> items = itemRepository.findAllByOwnerIdOrderById(userId);

        if (items.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();

        List<Booking> previousBookings =
                bookingRepository.findByItem_IdInAndStatusAndEndIsBefore(
                        itemIds, Booking.Status.APPROVED, now);

        List<Booking> nextBookings =
                bookingRepository.findByItem_IdInAndStatusAndStartIsAfter(
                        itemIds, Booking.Status.APPROVED, now);

        List<Comment> comments =
                commentRepository.findByItem_IdInOrderByCreatedDesc(itemIds);

        Map<Long, Booking> lastBookingByItem = new HashMap<>();
        for (Booking booking : previousBookings) {
            Long itemId = booking.getItem().getId();
            lastBookingByItem.merge(itemId, booking,
                    (first, second) -> first.getStart().isAfter(second.getStart())
                            ? first : second);
        }

        Map<Long, Booking> nextBookingByItem = new HashMap<>();
        for (Booking booking : nextBookings) {
            Long itemId = booking.getItem().getId();
            nextBookingByItem.merge(itemId, booking,
                    (first, second) -> first.getStart().isBefore(second.getStart())
                            ? first : second);
        }

        Map<Long, List<CommentDto>> commentsByItem = comments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(this::toCommentDto, Collectors.toList())
                ));

        return items.stream().map(item -> {
            ItemWithBookingsDto dto =
                    new ItemWithBookingsDto(ItemMapper.toItemDto(item));

            Booking lastBooking = lastBookingByItem.get(item.getId());
            if (lastBooking != null) {
                dto.setLastBooking(toBookingDto(lastBooking));
            }

            Booking nextBooking = nextBookingByItem.get(item.getId());
            if (nextBooking != null) {
                dto.setNextBooking(toBookingDto(nextBooking));
            }

            dto.setComments(commentsByItem.getOrDefault(item.getId(), List.of()));

            return dto;
        }).toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text.trim()).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, String text) {
        User author = userService.getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        LocalDateTime now = LocalDateTime.now();

        if (!bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                itemId, userId, Booking.Status.APPROVED, now)) {
            throw new IllegalArgumentException(
                    "Оставить отзыв можно только после завершённого бронирования");
        }

        Comment saved = commentRepository.save(Comment.builder()
                .text(text)
                .item(item)
                .author(author)
                .created(now)
                .build());

        return toCommentDto(saved);
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findByItem_IdOrderByCreatedDesc(itemId).stream()
                .map(this::toCommentDto)
                .toList();
    }

    private CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }

    private BookingDto toBookingDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(new BookingItemDto(
                        booking.getItem().getId(),
                        booking.getItem().getName()))
                .booker(new BookingBookerDto(
                        booking.getBooker().getId(),
                        booking.getBooker().getName()))
                .status(booking.getStatus().name())
                .build();
    }

    private Item getItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + id + " не найдена"));
    }

    private ItemRequest findRequest(Long id) {
        if (id == null) {
            return null;
        }

        return itemRequestRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Запрос с id=" + id + " не найден"));
    }

    private void validateRequired(String value, String message) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
