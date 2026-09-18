package com.ccb.queue.repo;

import com.ccb.queue.model.ElderReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ElderReservation, Long> {
}
