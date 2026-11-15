package ru.practicum.shareit.booking;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBooker_Id(Long bookerId, Sort sort);

    List<Booking> findByBooker_IdAndEndIsBefore(Long bookerId, LocalDateTime now, Sort sort);

    List<Booking> findByBooker_IdAndStartIsAfter(Long bookerId, LocalDateTime now, Sort sort);

    List<Booking> findByBooker_IdAndStartIsBeforeAndEndIsAfter(Long bookerId, LocalDateTime start, LocalDateTime end, Sort sort);

    List<Booking> findByBooker_IdAndStatus(Long bookerId, Booking.Status status, Sort sort);

    List<Booking> findByItem_Owner_Id(Long ownerId, Sort sort);

    List<Booking> findByItem_Owner_IdAndEndIsBefore(Long ownerId, LocalDateTime now, Sort sort);

    List<Booking> findByItem_Owner_IdAndStartIsAfter(Long ownerId, LocalDateTime now, Sort sort);

    List<Booking> findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(Long ownerId, LocalDateTime start, LocalDateTime end, Sort sort);

    List<Booking> findByItem_Owner_IdAndStatus(Long ownerId, Booking.Status status, Sort sort);

    List<Booking> findByItem_IdAndStatusAndEndIsBefore(Long itemId, Booking.Status status, LocalDateTime now, Sort sort);

    List<Booking> findByItem_IdAndStatusAndStartIsAfter(Long itemId, Booking.Status status, LocalDateTime now, Sort sort);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_IdInAndStatusAndEndIsBefore(List<Long> itemIds, Booking.Status status, LocalDateTime now);

    @EntityGraph(attributePaths = {"item", "booker"})
    List<Booking> findByItem_IdInAndStatusAndStartIsAfter(List<Long> itemIds, Booking.Status status, LocalDateTime now);

    boolean existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(Long itemId, Long bookerId, Booking.Status status, LocalDateTime now);

    @Query("select b from Booking b where b.item.id = :itemId and b.status = :status and b.start <= :end and b.end >= :start")
    List<Booking> findOverlapping(@Param("itemId") Long itemId, @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end, @Param("status") Booking.Status status);
}
