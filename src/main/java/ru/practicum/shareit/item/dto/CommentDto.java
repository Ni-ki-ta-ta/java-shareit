package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;

    @NotBlank(message = "Текст комментария не должен быть пустым")
    @Size(max = 1000, message = "Текст комментария не должен превышать 1000 символов")
    private String text;

    private String authorName;
    private LocalDateTime created;
}
