package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        log.info("Создание вещи пользователем с id={}", userId);

        User owner = userService.getUserOrThrow(userId);
        ItemRequest request = findRequest(itemDto.getRequestId());

        Item item = ItemMapper.toItem(itemDto, owner, request);
        Item saved = itemRepository.save(item);

        log.info("Вещь создана: id={}", saved.getId());
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        log.info("Обновление вещи с id={} пользователем с id={}", itemId, userId);

        User owner = userService.getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(owner.getId())) {
            log.warn("Пользователь с id={} не является владельцем вещи с id={}", userId, itemId);
            throw new NotFoundException("Изменять вещь может только её владелец");
        }

        if (itemDto.getName() != null) {
            validateRequired(
                    itemDto.getName(),
                    "Название вещи не должно быть пустым"
            );
            item.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null) {
            validateRequired(
                    itemDto.getDescription(),
                    "Описание вещи не должно быть пустым"
            );
            item.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        if (itemDto.getRequestId() != null) {
            item.setRequest(findRequest(itemDto.getRequestId()));
        }

        Item updated = itemRepository.update(item);

        log.info("Вещь с id={} обновлена", itemId);
        return ItemMapper.toItemDto(updated);
    }

    @Override
    public ItemDto getById(Long itemId) {
        log.info("Получение вещи с id={}", itemId);

        return ItemMapper.toItemDto(getItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(Long userId) {
        log.info("Получение списка вещей пользователя с id={}", userId);

        userService.getUserOrThrow(userId);

        return itemRepository.findAllByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Поиск вещей по тексту: {}", text);

        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Вещь с id={} не найдена", itemId);
                    return new NotFoundException("Вещь с id=" + itemId + " не найдена");
                });
    }

    private ItemRequest findRequest(Long requestId) {
        if (requestId == null) {
            return null;
        }

        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Запрос с id={} не найден", requestId);
                    return new NotFoundException("Запрос с id=" + requestId + " не найден");
                });
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            log.warn("Ошибка валидации: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
}
