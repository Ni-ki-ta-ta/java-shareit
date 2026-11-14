package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        return ItemDto.builder().id(item.getId()).name(item.getName()).description(item.getDescription())
                .available(item.isAvailable()).requestId(item.getRequest() == null ? null : item.getRequest().getId()).build();
    }

    public static Item toItem(ItemDto dto, User owner, ItemRequest request) {
        return Item.builder().name(dto.getName()).description(dto.getDescription()).available(Boolean.TRUE.equals(dto.getAvailable()))
                .owner(owner).request(request).build();
    }
}
