package ru.practicum.shareit.request;

import java.util.Optional;

public interface ItemRequestRepository {
    Optional<ItemRequest> findById(Long requestId);
}
