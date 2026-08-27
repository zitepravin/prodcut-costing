package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.PortalEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalEventLogRepository extends JpaRepository<PortalEventLog, String> {
}
