package com.ccb.queue.repo;

import com.ccb.queue.model.Enums;
import com.ccb.queue.model.QueueTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketRepository extends JpaRepository<QueueTicket, Long> {
    List<QueueTicket> findByStatusOrderByPriorityDescCreatedAtAsc(Enums.TicketStatus status);
    List<QueueTicket> findByStatusInOrderByCreatedAtDesc(List<Enums.TicketStatus> statuses);
    List<QueueTicket> findByAssignedWindowAndStatusIn(String windowNo, List<Enums.TicketStatus> statuses);
    long countByStatus(Enums.TicketStatus status);
    long countByChannel(Enums.Channel channel);
    long countByStatusAndElderTrue(Enums.TicketStatus status);
}
