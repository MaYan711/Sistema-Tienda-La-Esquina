package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
