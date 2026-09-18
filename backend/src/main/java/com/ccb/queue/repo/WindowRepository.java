package com.ccb.queue.repo;

import com.ccb.queue.model.ServiceWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WindowRepository extends JpaRepository<ServiceWindow, Long> {
    Optional<ServiceWindow> findByWindowNo(String windowNo);
}
