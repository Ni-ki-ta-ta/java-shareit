package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> items = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Item save(Item item) {
        item.setId(idCounter.getAndIncrement());
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Item update(Item item) {
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public List<Item> findAllByOwnerId(Long ownerId) {
        return items.values().stream().filter(item -> item.getOwner().getId().equals(ownerId))
                .sorted(Comparator.comparing(Item::getId)).toList();
    }

    @Override
    public List<Item> findAll() {
        return new ArrayList<>(items.values());
    }
}
