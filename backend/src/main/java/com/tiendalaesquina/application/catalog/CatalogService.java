package com.tiendalaesquina.application.catalog;

import com.tiendalaesquina.domain.model.MeasurementUnit;
import com.tiendalaesquina.domain.model.ProductCategory;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.MeasurementUnitRepository;
import com.tiendalaesquina.domain.repository.ProductCategoryRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CategoryRequest;
import com.tiendalaesquina.web.dto.CategoryResponse;
import com.tiendalaesquina.web.dto.MeasurementUnitRequest;
import com.tiendalaesquina.web.dto.MeasurementUnitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final ProductCategoryRepository categories;
    private final MeasurementUnitRepository units;
    private final UserAccountRepository users;

    public CatalogService(ProductCategoryRepository categories,
                          MeasurementUnitRepository units,
                          UserAccountRepository users) {
        this.categories = categories;
        this.units = units;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories(boolean activeOnly) {
        List<ProductCategory> result = activeOnly
                ? categories.findAllByActiveTrueOrderByNameAsc()
                : categories.findAllByOrderByNameAsc();
        return result.stream().map(this::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, String actorEmail) {
        String name = request.name().trim();
        ensureCategoryNameAvailable(name, null);
        ProductCategory category = new ProductCategory(
                name,
                normalizeNullable(request.description()),
                requireActor(actorEmail)
        );
        return toCategoryResponse(categories.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, String actorEmail) {
        ProductCategory category = requireCategory(id);
        String name = request.name().trim();
        ensureCategoryNameAvailable(name, id);
        category.update(name, normalizeNullable(request.description()), requireActor(actorEmail));
        return toCategoryResponse(categories.save(category));
    }

    @Transactional
    public CategoryResponse setCategoryActive(Long id, boolean active, String actorEmail) {
        ProductCategory category = requireCategory(id);
        category.setActive(active, requireActor(actorEmail));
        return toCategoryResponse(categories.save(category));
    }

    @Transactional(readOnly = true)
    public List<MeasurementUnitResponse> units(boolean activeOnly) {
        List<MeasurementUnit> result = activeOnly
                ? units.findAllByActiveTrueOrderByNameAsc()
                : units.findAllByOrderByNameAsc();
        return result.stream().map(this::toUnitResponse).toList();
    }

    @Transactional
    public MeasurementUnitResponse createUnit(MeasurementUnitRequest request) {
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        ensureUnitAvailable(code, name, null);
        return toUnitResponse(units.save(new MeasurementUnit(code, name, request.allowsDecimal())));
    }

    @Transactional
    public MeasurementUnitResponse updateUnit(Long id, MeasurementUnitRequest request) {
        MeasurementUnit unit = requireUnit(id);
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        ensureUnitAvailable(code, name, id);
        unit.update(code, name, request.allowsDecimal());
        return toUnitResponse(units.save(unit));
    }

    @Transactional
    public MeasurementUnitResponse setUnitActive(Long id, boolean active) {
        MeasurementUnit unit = requireUnit(id);
        unit.setActive(active);
        return toUnitResponse(units.save(unit));
    }

    private ProductCategory requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "category_not_found",
                        "Categoría no encontrada",
                        "La categoría solicitada no existe"
                ));
    }

    private MeasurementUnit requireUnit(Long id) {
        return units.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "measurement_unit_not_found",
                        "Unidad de medida no encontrada",
                        "La unidad de medida solicitada no existe"
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

    private void ensureCategoryNameAvailable(String name, Long currentId) {
        categories.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "category_name_already_exists",
                        "Categoría duplicada",
                        "Ya existe una categoría con ese nombre"
                );
            }
        });
    }

    private void ensureUnitAvailable(String code, String name, Long currentId) {
        units.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "measurement_unit_code_already_exists",
                        "Código de unidad duplicado",
                        "Ya existe una unidad de medida con ese código"
                );
            }
        });

        units.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "measurement_unit_name_already_exists",
                        "Unidad de medida duplicada",
                        "Ya existe una unidad de medida con ese nombre"
                );
            }
        });
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private CategoryResponse toCategoryResponse(ProductCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private MeasurementUnitResponse toUnitResponse(MeasurementUnit unit) {
        return new MeasurementUnitResponse(
                unit.getId(),
                unit.getCode(),
                unit.getName(),
                unit.isAllowsDecimal(),
                unit.isActive(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }
}
