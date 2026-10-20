package com.tiendalaesquina.domain.repository;

import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
