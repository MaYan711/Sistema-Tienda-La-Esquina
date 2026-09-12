package com.tiendalaesquina.application.supplier;

import com.tiendalaesquina.domain.model.AuditEvent;
import com.tiendalaesquina.domain.model.Supplier;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.SupplierRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CreateSupplierRequest;
import com.tiendalaesquina.web.dto.SupplierResponse;
import com.tiendalaesquina.web.dto.UpdateSupplierRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final UserAccountRepository users;
    private final AuditEventRepository auditEvents;

    public SupplierService(SupplierRepository suppliers,
                           UserAccountRepository users,
                           AuditEventRepository auditEvents) {
        this.suppliers = suppliers;
        this.users = users;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> search(String search, Boolean active, Pageable pageable) {
        String normalizedSearch = normalizeNullable(search);

        Specification<Supplier> specification = (root, query, cb) -> cb.conjunction();

        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("nit")), pattern),
                            cb.like(cb.lower(root.get("phone")), pattern),
                            cb.like(cb.lower(root.get("email")), pattern)
                    )
            );
        }

        if (active != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("active"), active)
            );
        }

        return suppliers.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        return toResponse(requireSupplier(id));
    }

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request, String actorEmail) {
        String name = normalizeRequired(request.name());
        String nit = normalizeNullable(request.nit());
        String phone = normalizeNullable(request.phone());
        String email = normalizeNullable(request.email());
        String address = normalizeNullable(request.address());

        if (nit != null && suppliers.existsByNitIgnoreCase(nit)) {
            throw duplicateNit(nit);
        }

        UserAccount actor = requireActor(actorEmail);

        Supplier supplier = new Supplier(
                name,
                nit,
                phone,
                email,
                address,
                actor
        );

        supplier = suppliers.save(supplier);

        auditEvents.save(new AuditEvent(
                actor,
                "SUPPLIER_CREATED",
                "SUPPLIER",
                String.valueOf(supplier.getId()),
                "Proveedor creado: " + supplier.getName(),
                null,
                Map.of(
                        "name", supplier.getName(),
                        "nit", supplier.getNit() == null ? "" : supplier.getNit(),
                        "active", supplier.isActive()
                ),
                null,
                null,
                Instant.now()
        ));

        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse update(Long id, UpdateSupplierRequest request, String actorEmail) {
        Supplier supplier = requireSupplier(id);
        UserAccount actor = requireActor(actorEmail);

        String newName = normalizeRequired(request.name());
        String newNit = normalizeNullable(request.nit());
        String newPhone = normalizeNullable(request.phone());
        String newEmail = normalizeNullable(request.email());
        String newAddress = normalizeNullable(request.address());

        if (newNit != null && suppliers.existsByNitIgnoreCaseAndIdNot(newNit, id)) {
            throw duplicateNit(newNit);
        }

        Map<String, Object> beforeData = Map.of(
                "name", supplier.getName(),
                "nit", supplier.getNit() == null ? "" : supplier.getNit(),
                "phone", supplier.getPhone() == null ? "" : supplier.getPhone(),
                "email", supplier.getEmail() == null ? "" : supplier.getEmail(),
                "address", supplier.getAddress() == null ? "" : supplier.getAddress()
        );

        supplier.update(
                newName,
                newNit,
                newPhone,
                newEmail,
                newAddress,
                actor
        );

        supplier = suppliers.save(supplier);

        Map<String, Object> afterData = Map.of(
                "name", supplier.getName(),
                "nit", supplier.getNit() == null ? "" : supplier.getNit(),
                "phone", supplier.getPhone() == null ? "" : supplier.getPhone(),
                "email", supplier.getEmail() == null ? "" : supplier.getEmail(),
                "address", supplier.getAddress() == null ? "" : supplier.getAddress()
        );

        auditEvents.save(new AuditEvent(
                actor,
                "SUPPLIER_UPDATED",
                "SUPPLIER",
                String.valueOf(supplier.getId()),
                "Proveedor actualizado: " + supplier.getName(),
                beforeData,
                afterData,
                null,
                null,
                Instant.now()
        ));

        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse setActive(Long id, boolean active, String actorEmail) {
        Supplier supplier = requireSupplier(id);

        if (supplier.isActive() == active) {
            return toResponse(supplier);
        }

        UserAccount actor = requireActor(actorEmail);
        boolean previous = supplier.isActive();

        supplier.setActive(active, actor);
        supplier = suppliers.save(supplier);

        auditEvents.save(new AuditEvent(
                actor,
                active ? "SUPPLIER_ACTIVATED" : "SUPPLIER_DEACTIVATED",
                "SUPPLIER",
                String.valueOf(supplier.getId()),
                (active ? "Proveedor activado: " : "Proveedor desactivado: ") + supplier.getName(),
                Map.of("active", previous),
                Map.of("active", active),
                null,
                null,
                Instant.now()
        ));

        return toResponse(supplier);
    }

    private Supplier requireSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "supplier_not_found",
                        "Proveedor no encontrado",
                        "El proveedor solicitado no existe"
                ));
    }

    private UserAccount requireActor(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No fue posible identificar al usuario autenticado"
                ));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getNit(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ApiException duplicateNit(String nit) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "supplier_nit_already_exists",
                "NIT duplicado",
                "Ya existe un proveedor con el NIT " + nit
        );
    }
}