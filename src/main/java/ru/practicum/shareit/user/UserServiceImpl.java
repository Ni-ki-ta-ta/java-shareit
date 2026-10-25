package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        validateName(userDto.getName());
        validateEmail(userDto.getEmail());
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        User user = UserMapper.toUser(userDto);
        user.setId(null);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        User current = getUserOrThrow(userId);
        String newName = userDto.getName();
        String newEmail = userDto.getEmail();

        if (newName != null) {
            validateName(newName);
        }
        if (newEmail != null) {
            validateEmail(newEmail);
            if (userRepository.existsByEmailAndIdNot(newEmail, userId)) {
                throw new ConflictException("Пользователь с таким email уже существует");
            }
        }

        User updated = new User(current.getId(),
                newName == null ? current.getName() : newName,
                newEmail == null ? current.getEmail() : newEmail);
        return UserMapper.toUserDto(userRepository.update(updated));
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(getUserOrThrow(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    public void delete(Long userId) {
        getUserOrThrow(userId);
        userRepository.deleteById(userId);
    }

    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не должно быть пустым");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+" +
                "@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$")) {
            throw new IllegalArgumentException("Некорректный email");
        }
    }
}
