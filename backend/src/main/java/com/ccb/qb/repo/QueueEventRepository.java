package com.ccb.qb.repo;

import com.ccb.qb.entity.QueueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueueEventRepository extends JpaRepository<QueueEvent, Long> {
    List<QueueEvent> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    List<QueueEvent> findAllByOrderByCreatedAtDesc();
    List<QueueEvent> findByResolvedFalseOrderByCreatedAtDesc();
    long countByResolvedFalse();
}
