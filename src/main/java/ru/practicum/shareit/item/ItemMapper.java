package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        return new ItemDto(item.getId(), item.getName(), item.getDescription(), item.isAvailable(),
                item.getRequest() == null ? null : item.getRequest().getId());
    }
}
