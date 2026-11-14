package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "author")
    List<Comment> findByItem_IdInOrderByCreatedDesc(List<Long> itemIds);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByItem_IdOrderByCreatedDesc(Long itemId);
}
