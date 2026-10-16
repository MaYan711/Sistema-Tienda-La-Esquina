package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.EmployeeOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;

public interface EmployeeOperatingHourRepository extends JpaRepository<EmployeeOperatingHour, Long> {

    Optional<EmployeeOperatingHour> findByDayOfWeek(DayOfWeek dayOfWeek);
}
