package ru.practicum.shareit.item;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.dto.CommentDto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentDtoValidationTest {
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
    void shouldRejectBlankText() {
        CommentDto comment = CommentDto.builder().text("  ").build();

        assertFalse(validator.validate(comment).isEmpty());
    }

    @Test
    void shouldRejectTextLongerThan2048Characters() {
        CommentDto comment = CommentDto.builder().text("a".repeat(2049)).build();

        assertFalse(validator.validate(comment).isEmpty());
    }

    @Test
    void shouldAcceptTextWithinAllowedLength() {
        CommentDto comment = CommentDto.builder().text("Good item").build();

        assertTrue(validator.validate(comment).isEmpty());
    }
}
