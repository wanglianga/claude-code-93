package com.ccb.qb.repo;

import com.ccb.qb.entity.QueueTicket;
import com.ccb.qb.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface QueueTicketRepository extends JpaRepository<QueueTicket, Long> {
    List<QueueTicket> findAllByOrderByCreatedAtDesc();
    List<QueueTicket> findByStatusOrderByCreatedAtAsc(Enums.TicketStatus status);
    long countByStatus(Enums.TicketStatus status);
    long countByStatusAndElderlyTrue(Enums.TicketStatus status);
    List<QueueTicket> findByCounterIdAndStatusIn(Long counterId, List<Enums.TicketStatus> statuses);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(t.waitSeconds),0) from QueueTicket t where t.waitSeconds is not null and t.finishedAt between ?1 and ?2")
    long sumWaitSeconds(LocalDateTime from, LocalDateTime to);

    @Query("select count(t) from QueueTicket t where t.waitSeconds is not null and t.finishedAt between ?1 and ?2")
    long countFinished(LocalDateTime from, LocalDateTime to);

    long countByResultType(Enums.ResultType type);
    long countByComplaintTrue();
    long countByFraudStatus(Enums.FraudStatus status);
    long countByChannel(Enums.Channel channel);
}
