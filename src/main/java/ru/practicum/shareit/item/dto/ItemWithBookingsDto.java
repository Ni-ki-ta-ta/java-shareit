package ru.practicum.shareit.item.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.shareit.booking.dto.BookingDto;

@Getter
@Setter
@NoArgsConstructor
public class ItemWithBookingsDto extends ItemWithCommentsDto {
    private BookingDto lastBooking;
    private BookingDto nextBooking;

    public ItemWithBookingsDto(ItemDto item) {
        super(item);
    }
}
