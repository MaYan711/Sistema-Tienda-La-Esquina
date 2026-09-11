package com.tiendalaesquina.application.product;

import com.tiendalaesquina.domain.model.MeasurementUnit;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.ProductCategory;
import com.tiendalaesquina.domain.model.StockStatus;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.MeasurementUnitRepository;
import com.tiendalaesquina.domain.repository.ProductCategoryRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CategoryResponse;
import com.tiendalaesquina.web.dto.CreateProductRequest;
import com.tiendalaesquina.web.dto.MeasurementUnitResponse;
import com.tiendalaesquina.web.dto.ProductResponse;
import com.tiendalaesquina.web.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository products;
    private final ProductCategoryRepository categories;
    private final MeasurementUnitRepository units;
    private final UserAccountRepository users;

    public ProductService(ProductRepository products,
                          ProductCategoryRepository categories,
                          MeasurementUnitRepository units,
                          UserAccountRepository users) {
        this.products = products;
        this.categories = categories;
        this.units = units;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String search, Long categoryId, Boolean active,
                                        String stockStatus, Pageable pageable) {
        String normalizedSearch = normalizeNullable(search);
        String normalizedStatus = normalizeStockStatus(stockStatus);

        Specification<Product> specification = (root, query, cb) -> cb.conjunction();

        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("code")), pattern),
                            cb.like(cb.lower(root.get("name")), pattern)
                    )
            );
        }

        if (categoryId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId)
            );
        }

        if (active != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("active"), active)
            );
        }

        if (normalizedStatus != null) {
            specification = switch (normalizedStatus) {
                case "AVAILABLE" -> specification.and((root, query, cb) ->
                        cb.greaterThan(
                                root.<BigDecimal>get("currentStock"),
                                root.<BigDecimal>get("minimumStock")
                        )
                );
                case "LOW_STOCK" -> specification.and((root, query, cb) ->
                        cb.and(
                                cb.greaterThan(root.<BigDecimal>get("currentStock"), BigDecimal.ZERO),
                                cb.lessThanOrEqualTo(
                                        root.<BigDecimal>get("currentStock"),
                                        root.<BigDecimal>get("minimumStock")
                                )
                        )
                );
                case "OUT_OF_STOCK" -> specification.and((root, query, cb) ->
                        cb.equal(root.<BigDecimal>get("currentStock"), BigDecimal.ZERO)
                );
                default -> specification;
            };
        }

        return products.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(requireProduct(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request, String actorEmail) {
        String code = normalizeRequired(request.code());
        String name = normalizeRequired(request.name());

        if (products.existsByCodeIgnoreCaseAndActiveTrue(code)) {
            throw duplicateCode(code);
        }

        ProductCategory category = requireActiveCategory(request.categoryId());
        MeasurementUnit unit = requireActiveUnit(request.unitId());
        UserAccount actor = requireActor(actorEmail);

        Product product = new Product(
                code,
                name,
                normalizeNullable(request.description()),
                normalizeNullable(request.imageUrl()),
                category,
                unit,
                request.purchasePrice(),
                request.salePrice(),
                request.initialStock(),
                request.minimumStock(),
                actor
        );

        return toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request, String actorEmail) {
        Product product = requireProduct(id);
        String code = normalizeRequired(request.code());
        String name = normalizeRequired(request.name());

        if (products.existsByCodeIgnoreCaseAndActiveTrueAndIdNot(code, id)) {
            throw duplicateCode(code);
        }

        ProductCategory category = requireActiveCategory(request.categoryId());
        MeasurementUnit unit = requireActiveUnit(request.unitId());
        UserAccount actor = requireActor(actorEmail);

        product.updateCatalogData(
                code,
                name,
                normalizeNullable(request.description()),
                normalizeNullable(request.imageUrl()),
                category,
                unit,
                request.purchasePrice(),
                request.salePrice(),
                request.minimumStock(),
                actor
        );

        return toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse setActive(Long id, boolean active, String actorEmail) {
        Product product = requireProduct(id);
        if (product.isActive() == active) {
            return toResponse(product);
        }

        if (active && products.existsByCodeIgnoreCaseAndActiveTrueAndIdNot(product.getCode(), id)) {
            throw duplicateCode(product.getCode());
        }

        product.setActive(active, requireActor(actorEmail));
        return toResponse(products.save(product));
    }

    private Product requireProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "product_not_found",
                        "Producto no encontrado",
                        "El producto solicitado no existe"
                ));
    }

    private ProductCategory requireActiveCategory(Long id) {
        ProductCategory category = categories.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "category_not_found",
                        "Categoría no encontrada",
                        "La categoría seleccionada no existe"
                ));

        if (!category.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "category_inactive",
                    "Categoría inactiva",
                    "La categoría seleccionada está inactiva"
            );
        }
        return category;
    }

    private MeasurementUnit requireActiveUnit(Long id) {
        MeasurementUnit unit = units.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "measurement_unit_not_found",
                        "Unidad de medida no encontrada",
                        "La unidad de medida seleccionada no existe"
                ));

        if (!unit.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "measurement_unit_inactive",
                    "Unidad de medida inactiva",
                    "La unidad de medida seleccionada está inactiva"
            );
        }
        return unit;
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

    private String normalizeStockStatus(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        try {
            return StockStatus.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_stock_status",
                    "Estado de inventario inválido",
                    "El estado debe ser AVAILABLE, LOW_STOCK u OUT_OF_STOCK"
            );
        }
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

    private ApiException duplicateCode(String code) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "product_code_already_exists",
                "Código de producto duplicado",
                "Ya existe un producto activo con el código " + code
        );
    }

    private ProductResponse toResponse(Product product) {
        ProductCategory category = product.getCategory();
        MeasurementUnit unit = product.getUnit();

        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getDescription(),
                        category.isActive(),
                        category.getCreatedAt(),
                        category.getUpdatedAt()
                ),
                new MeasurementUnitResponse(
                        unit.getId(),
                        unit.getCode(),
                        unit.getName(),
                        unit.isAllowsDecimal(),
                        unit.isActive(),
                        unit.getCreatedAt(),
                        unit.getUpdatedAt()
                ),
                product.getPurchasePrice(),
                product.getSalePrice(),
                product.getCurrentStock(),
                product.getMinimumStock(),
                product.getStockStatus().name(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}