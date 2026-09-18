package com.ccb.queue.repo;

import com.ccb.queue.model.CollaborationEvent;
import com.ccb.queue.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<CollaborationEvent, Long> {
    List<CollaborationEvent> findByStatusInOrderByRaisedAtDesc(List<Enums.EventStatus> statuses);
    List<CollaborationEvent> findByTicketIdOrderByRaisedAtAsc(Long ticketId);
    long countByStatus(Enums.EventStatus status);
}
