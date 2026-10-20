package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {

    Optional<MeasurementUnit> findByCodeIgnoreCase(String code);

    Optional<MeasurementUnit> findByNameIgnoreCase(String name);

    List<MeasurementUnit> findAllByOrderByNameAsc();

    List<MeasurementUnit> findAllByActiveTrueOrderByNameAsc();
}
