package ru.practicum.shareit.request;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryItemRequestRepository implements ItemRequestRepository {
    private final Map<Long, ItemRequest> requests = new ConcurrentHashMap<>();

    @Override
    public Optional<ItemRequest> findById(Long requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }
}
