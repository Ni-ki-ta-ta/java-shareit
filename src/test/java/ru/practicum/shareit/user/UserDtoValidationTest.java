package ru.practicum.shareit.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDtoValidationTest {

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
        UserDto user = new UserDto(null, null, "nikita@example.org");

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя без имени"
        );
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        UserDto user = new UserDto(null, " ", "nikita@example.org");

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя с пустым именем"
        );
    }

    @Test
    void shouldFailWhenEmailIsNull() {
        UserDto user = new UserDto(null, "Никита", null);

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя без email"
        );
    }

    @Test
    void shouldFailWhenEmailIsBlank() {
        UserDto user = new UserDto(null, "Никита", " ");

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя с пустым email"
        );
    }

    @Test
    void shouldFailWhenEmailIsInvalid() {
        UserDto user = new UserDto(null, "Никита", "invalid-email");

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя с некорректным email"
        );
    }

    @Test
    void shouldPassWithValidUser() {
        UserDto user = new UserDto(
                null,
                "Никита",
                "nikita@example.org"
        );

        Set<ConstraintViolation<UserDto>> violations = validator.validate(user);

        assertTrue(
                violations.isEmpty(),
                "Валидация должна пропускать корректного пользователя"
        );
    }
}
