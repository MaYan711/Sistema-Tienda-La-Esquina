package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.EmployeeOperatingHourException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeOperatingHourExceptionRepository
        extends JpaRepository<EmployeeOperatingHourException, Long> {

    Optional<EmployeeOperatingHourException> findByExceptionDate(LocalDate exceptionDate);

    boolean existsByExceptionDate(LocalDate exceptionDate);

    boolean existsByExceptionDateAndIdNot(LocalDate exceptionDate, Long id);

    List<EmployeeOperatingHourException> findAllByOrderByExceptionDateAsc();
}
