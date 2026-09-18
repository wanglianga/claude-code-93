package com.ccb.qb.repo;

import com.ccb.qb.entity.Counter;
import com.ccb.qb.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {
    List<Counter> findByStatus(Enums.CounterStatus status);
}
