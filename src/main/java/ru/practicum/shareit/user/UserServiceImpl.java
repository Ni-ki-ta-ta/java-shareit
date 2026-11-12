package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        log.info("Создание пользователя с email={}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            log.warn("Пользователь с email={} уже существует", userDto.getEmail());
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = UserMapper.toUser(userDto);
        user.setId(null);
        User saved = userRepository.save(user);

        log.info("Пользователь создан: id={}", saved.getId());
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        log.info("Обновление пользователя с id={}", userId);

        User current = getUserOrThrow(userId);

        String newName = userDto.getName();
        String newEmail = userDto.getEmail();

        if (newName != null) {
            validateName(newName);
        }

        if (newEmail != null) {
            validateEmail(newEmail);

            if (userRepository.existsByEmailAndIdNot(newEmail, userId)) {
                log.warn("Email={} уже используется другим пользователем", newEmail);
                throw new ConflictException("Пользователь с таким email уже существует");
            }
        }

        User updated = new User(
                current.getId(),
                newName == null ? current.getName() : newName,
                newEmail == null ? current.getEmail() : newEmail
        );

        User saved = userRepository.save(updated);

        log.info("Пользователь с id={} обновлён", userId);
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto getById(Long userId) {
        log.info("Получение пользователя с id={}", userId);

        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        log.info("Получение списка всех пользователей");

        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        log.info("Удаление пользователя с id={}", userId);

        getUserOrThrow(userId);
        userRepository.deleteById(userId);

        log.info("Пользователь с id={} удалён", userId);
    }

    @Override
    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("Пользователь с id=" + userId + " не найден");
                });
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            log.warn("Ошибка валидации: пустое имя");
            throw new IllegalArgumentException("Имя не должно быть пустым");
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank() || !email.matches(
                "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?" +
                        "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$"
        )) {
            log.warn("Ошибка валидации: некорректный email={}", email);
            throw new IllegalArgumentException("Некорректный email");
        }
    }
}
