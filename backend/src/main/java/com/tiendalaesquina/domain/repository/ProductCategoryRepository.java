package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByNameIgnoreCase(String name);

    List<ProductCategory> findAllByOrderByNameAsc();

    List<ProductCategory> findAllByActiveTrueOrderByNameAsc();
}
