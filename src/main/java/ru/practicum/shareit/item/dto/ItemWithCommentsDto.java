package ru.practicum.shareit.item.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ItemWithCommentsDto extends ItemDto {
    private List<CommentDto> comments;

    public ItemWithCommentsDto(ItemDto item) {
        super(item.getId(), item.getName(), item.getDescription(), item.getAvailable(), item.getRequestId());
    }
}
