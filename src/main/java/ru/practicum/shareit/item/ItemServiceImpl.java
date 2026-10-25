package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = userService.getUserOrThrow(userId);

        validateRequired(itemDto.getName(), "Название вещи не должно быть пустым");
        validateRequired(itemDto.getDescription(), "Описание вещи не должно быть пустым");

        if (itemDto.getAvailable() == null) {
            throw new IllegalArgumentException("Необходимо указать доступность вещи");
        }

        Item item = new Item(
                null,
                itemDto.getName(),
                itemDto.getDescription(),
                itemDto.getAvailable(),
                owner,
                findRequest(itemDto.getRequestId())
        );

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        User owner = userService.getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        if (!item.getOwner().getId().equals(owner.getId())) {
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

        return ItemMapper.toItemDto(itemRepository.update(item));
    }

    @Override
    public ItemDto getById(Long itemId) {
        return ItemMapper.toItemDto(getItemOrThrow(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(Long userId) {
        userService.getUserOrThrow(userId);

        return itemRepository.findAllByOwnerId(userId)
                .stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String query = text.toLowerCase(Locale.ROOT);

        return itemRepository.findAll()
                .stream()
                .filter(Item::isAvailable)
                .filter(item ->
                        item.getName().toLowerCase(Locale.ROOT).contains(query)
                                || item.getDescription()
                                .toLowerCase(Locale.ROOT)
                                .contains(query)
                )
                .sorted(Comparator.comparing(Item::getId))
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + itemId + " не найдена")
                );
    }

    private ItemRequest findRequest(Long requestId) {
        if (requestId == null) {
            return null;
        }

        return itemRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new NotFoundException("Запрос с id=" + requestId + " не найден")
                );
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
