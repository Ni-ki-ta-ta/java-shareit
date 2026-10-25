package ru.practicum.shareit.item;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDtoValidationTest {

    private static ValidatorFactory factory;
    private Validator validator;

    @BeforeAll
    static void createValidatorFactory() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        factory.close();
    }

    @BeforeEach
    void setUp() {
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenNameIsNull() {
        ItemDto item = ItemDto.builder()
                .name(null)
                .description("Описание вещи")
                .available(true)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять вещь без названия"
        );
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        ItemDto item = ItemDto.builder()
                .name(" ")
                .description("Описание вещи")
                .available(true)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять вещь с пустым названием"
        );
    }

    @Test
    void shouldFailWhenDescriptionIsNull() {
        ItemDto item = ItemDto.builder()
                .name("Дрель")
                .description(null)
                .available(true)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять вещь без описания"
        );
    }

    @Test
    void shouldFailWhenDescriptionIsBlank() {
        ItemDto item = ItemDto.builder()
                .name("Дрель")
                .description(" ")
                .available(true)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять вещь с пустым описанием"
        );
    }

    @Test
    void shouldFailWhenAvailableIsNull() {
        ItemDto item = ItemDto.builder()
                .name("Дрель")
                .description("Описание вещи")
                .available(null)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять вещь без указания доступности"
        );
    }

    @Test
    void shouldPassWithValidItem() {
        ItemDto item = ItemDto.builder()
                .name("Дрель")
                .description("Мощная электрическая дрель")
                .available(true)
                .build();

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(item);

        assertTrue(
                violations.isEmpty(),
                "Валидация должна пропускать корректную вещь"
        );
    }
}
