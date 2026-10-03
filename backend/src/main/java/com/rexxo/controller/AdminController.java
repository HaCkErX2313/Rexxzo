package com.rexxo.controller;

import com.rexxo.config.ShiprocketConfig;
import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import com.rexxo.service.AdminAuditService;
import com.rexxo.service.AnalyticsService;
import com.rexxo.service.ReturnService;
import com.rexxo.service.WhatsAppNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.rexxo.otp.service.EmailProvider;
import com.rexxo.otp.service.SmsProvider;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final CouponRepository couponRepository;
    private final ReviewRepository reviewRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final RefundRepository refundRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final ReturnService returnService;
    private final AnalyticsService analyticsService;
    private final AdminAuditService auditService;
    private final WhatsAppNotificationService whatsAppService;
    private final ShiprocketConfig shiprocketConfig;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Autowired(required = false)
    private EmailProvider emailProvider;

    @Autowired(required = false)
    private List<SmsProvider> smsProviders;

    // ─── 1. Dashboard ────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> dashboard() {
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = userRepository.countByRole(User.Role.CUSTOMER);

        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED
                      || o.getStatus() == Order.OrderStatus.PAID)
            .map(Order::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.PENDING
                      || o.getStatus() == Order.OrderStatus.PENDING_PAYMENT)
            .count();

        long deliveredOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
            .count();

        long cancelledOrders = allOrders.stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED)
            .count();

        long returnRequests = returnRequestRepository.countByStatus(ReturnRequest.ReturnStatus.RETURN_REQUESTED);

        BigDecimal refundAmount = refundRepository.findAll().stream()
            .map(Refund::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Product> products = productRepository.findAll();
        long lowStockProducts = products.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getStock() > 0 && p.getStock() < 10)
            .count();

        long outOfStockProducts = products.stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && p.getStock() <= 0)
            .count();

        List<AdminOrderDto> recentOrders = orderRepository.findAll(
            PageRequest.of(0, 5, Sort.by("createdAt").descending())
        ).getContent().stream().map(this::toAdminOrderDto).toList();

        List<CustomerDto> recentCustomers = userRepository.findAll(
            PageRequest.of(0, 5, Sort.by("createdAt").descending())
        ).getContent().stream().map(u -> new CustomerDto(
            u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getIsActive(), u.getCreatedAt()
        )).toList();

        List<AnalyticsService.TopProductDto> topProducts = analyticsService.getAnalytics("30d").topProducts();
        if (topProducts == null) {
            topProducts = Collections.emptyList();
        }

        return ResponseEntity.ok(new DashboardDto(
            totalRevenue, totalOrders, totalProducts, totalCustomers,
            pendingOrders, deliveredOrders, cancelledOrders, returnRequests,
            refundAmount, lowStockProducts, outOfStockProducts,
            recentOrders, recentCustomers, topProducts
        ));
    }

    // ─── 2. Orders ───────────────────────────────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<Page<AdminOrderDto>> allOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderRepository.findAll(pageable);

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findAll(
                    (root, q, cb) -> cb.equal(root.get("status"), orderStatus), pageable);
            } catch (IllegalArgumentException ignored) {}
        }

        return ResponseEntity.ok(orders.map(this::toAdminOrderDto));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminOrderDetailDto> getOrderDetail(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        List<AdminOrderItemDto> items = order.getItems().stream().map(i -> new AdminOrderItemDto(
            i.getId(), i.getProduct().getId(), i.getProductName(), i.getProductImageUrl(),
            i.getUnitPrice(), i.getQuantity(), i.getSubtotal()
        )).toList();

        String awb = order.getShipment() != null ? order.getShipment().getAwbNumber() : null;
        String courier = order.getShipment() != null ? order.getShipment().getCourierName() : null;
        String shipmentStatus = order.getShipment() != null && order.getShipment().getShipmentStatus() != null
            ? order.getShipment().getShipmentStatus().name() : null;
        String paymentStatus = order.getPayment() != null && order.getPayment().getStatus() != null
            ? order.getPayment().getStatus().name() : "PENDING";

        List<ReturnRequest> returns = returnRequestRepository.findByOrderId(order.getId());
        String returnStatus = !returns.isEmpty() ? returns.get(0).getStatus().name() : null;

        return ResponseEntity.ok(new AdminOrderDetailDto(
            order.getId(), order.getOrderNumber(), order.getUser().getId(),
            order.getUser().getName(), order.getUser().getEmail(), order.getUser().getPhone(),
            order.getStatus().name(), paymentStatus, order.getPaymentMethod(),
            courier, awb, shipmentStatus, returnStatus,
            order.getSubtotal(), order.getShippingFee(), order.getDiscountAmount(), order.getTotal(),
            order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
            order.getShippingCity(), order.getShippingState(), order.getShippingPincode(),
            items, order.getCreatedAt()
        ));
    }

    @PatchMapping("/orders/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, String>> updateOrderStatus(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Order not found"));
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(body.get("status").toUpperCase());
            order.setStatus(newStatus);
            orderRepository.save(order);

            auditService.log(auth.getName(), "ADMIN_UPDATED_ORDER_STATUS", "Order", id,
                "Updated status to " + newStatus, null);

            return ResponseEntity.ok(Map.of("message", "Order status updated", "status", newStatus.name()));
        } catch (IllegalArgumentException e) {
            throw RexxoException.badRequest("Invalid status: " + body.get("status"));
        }
    }

    @PatchMapping("/orders/{id}/cancel")
    @Transactional
    public ResponseEntity<Map<String, String>> cancelOrder(Authentication auth, @PathVariable Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Order not found"));

        if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw RexxoException.badRequest("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Restock inventory
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }

        auditService.log(auth.getName(), "ADMIN_CANCELLED_ORDER", "Order", id,
            "Order cancelled by admin and inventory restocked", null);

        return ResponseEntity.ok(Map.of("message", "Order cancelled and inventory restored"));
    }

    // ─── 3. Products CRUD & Inventory ────────────────────────────────────────
    @GetMapping("/products")
    public ResponseEntity<Page<Product>> allProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stockFilter,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim() : null;
        String cleanStock = (stockFilter != null && !stockFilter.isBlank() && !"ALL".equalsIgnoreCase(stockFilter)) ? stockFilter.trim() : null;

        return ResponseEntity.ok(productRepository.findAdminProductsWithFilters(categoryId, cleanStatus, cleanStock, cleanSearch, pageable));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found with ID: " + id));
        return ResponseEntity.ok(p);
    }

    @PostMapping("/products")
    @Transactional
    public ResponseEntity<Product> createProduct(Authentication auth, @Valid @RequestBody ProductRequest req) {
        Category cat = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> RexxoException.notFound("Category not found with ID: " + req.categoryId()));

        // 1. Exact Price & MRP validation
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw RexxoException.badRequest("Selling price cannot be negative");
        }

        BigDecimal discountPercent = BigDecimal.ZERO;
        if (req.compareAtPrice() != null && req.compareAtPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (req.price().compareTo(req.compareAtPrice()) > 0) {
                throw RexxoException.badRequest("Selling price (₹" + req.price() + ") cannot be greater than MRP (₹" + req.compareAtPrice() + ")");
            }
            discountPercent = req.compareAtPrice().subtract(req.price())
                .divide(req.compareAtPrice(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        }

        // 2. Stock validation
        int stockQuantity = req.resolveStock(0);
        if (stockQuantity < 0) {
            throw RexxoException.badRequest("Stock quantity cannot be negative");
        }

        // 3. SKU generation & unique validation
        String sku = req.sku() != null && !req.sku().isBlank()
            ? req.sku().trim().toUpperCase()
            : "REX-" + System.currentTimeMillis();
        if (productRepository.existsBySku(sku)) {
            throw RexxoException.conflict("SKU is already in use by another product: " + sku);
        }

        // 4. Slug generation & uniqueness
        String slug = req.slug() != null && !req.slug().isBlank()
            ? req.slug().toLowerCase().replaceAll("[^a-z0-9-]", "-")
            : req.name().toLowerCase().replaceAll("[^a-z0-9-]", "-") + "-" + System.currentTimeMillis();
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + (System.currentTimeMillis() % 10000);
        }

        // 5. Status & Publishing visibility
        String statusStr = req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "ACTIVE";
        boolean isActive = !"DRAFT".equalsIgnoreCase(statusStr) && !"ARCHIVED".equalsIgnoreCase(statusStr);
        if (req.isActive() != null && !req.isActive()) {
            isActive = false;
            statusStr = "DRAFT";
        }
        if (isActive && stockQuantity <= 0 && "ACTIVE".equalsIgnoreCase(statusStr)) {
            statusStr = "OUT_OF_STOCK";
        }

        int lowStockThreshold = req.lowStockThreshold() != null && req.lowStockThreshold() >= 0 ? req.lowStockThreshold() : 10;

        Product product = Product.builder()
            .name(req.name().trim())
            .slug(slug)
            .sku(sku)
            .description(req.description())
            .price(req.price())
            .originalPrice(req.compareAtPrice())
            .discountPercent(discountPercent)
            .stock(stockQuantity)
            .lowStockThreshold(lowStockThreshold)
            .status(statusStr)
            .category(cat)
            .isActive(isActive)
            .isFeatured(req.isFeatured() != null ? req.isFeatured() : false)
            .weightGrams(req.weightGrams())
            .lengthCm(req.lengthCm())
            .widthCm(req.widthCm())
            .heightCm(req.heightCm())
            .material(req.materials())
            .dimensions(req.dimensions())
            .build();

        if (req.imageUrls() != null && !req.imageUrls().isEmpty()) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < req.imageUrls().size(); i++) {
                String imgUrl = req.imageUrls().get(i);
                if (imgUrl != null && !imgUrl.isBlank()) {
                    images.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(imgUrl.trim())
                        .isPrimary(i == 0)
                        .displayOrder(i)
                        .build());
                }
            }
            product.setImages(images);
        }

        Product saved = productRepository.save(product);
        auditService.log(auth.getName(), "ADMIN_CREATED_PRODUCT", "Product", saved.getId(),
            "Created product " + saved.getName() + " with price ₹" + saved.getPrice() + " (status: " + statusStr + ")", null);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/products/{id}")
    @Transactional
    public ResponseEntity<Product> updateProduct(Authentication auth,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody ProductRequest req) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found with ID: " + id));

        Category cat = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> RexxoException.notFound("Category not found with ID: " + req.categoryId()));

        // 1. Price & MRP validation
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw RexxoException.badRequest("Selling price cannot be negative");
        }

        BigDecimal discountPercent = BigDecimal.ZERO;
        if (req.compareAtPrice() != null && req.compareAtPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (req.price().compareTo(req.compareAtPrice()) > 0) {
                throw RexxoException.badRequest("Selling price (₹" + req.price() + ") cannot be greater than MRP (₹" + req.compareAtPrice() + ")");
            }
            discountPercent = req.compareAtPrice().subtract(req.price())
                .divide(req.compareAtPrice(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        }

        // 2. Stock validation
        int stockQuantity = req.resolveStock(product.getStock() != null ? product.getStock() : 0);
        if (stockQuantity < 0) {
            throw RexxoException.badRequest("Stock quantity cannot be negative");
        }

        // 3. SKU uniqueness check
        if (req.sku() != null && !req.sku().isBlank()) {
            String cleanSku = req.sku().trim().toUpperCase();
            if (productRepository.existsBySkuAndIdNot(cleanSku, id)) {
                throw RexxoException.conflict("SKU is already in use by another product: " + cleanSku);
            }
            product.setSku(cleanSku);
        }

        // 4. Status & publishing logic
        String statusStr = req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : product.getStatus();
        boolean isActive = !"DRAFT".equalsIgnoreCase(statusStr) && !"ARCHIVED".equalsIgnoreCase(statusStr);
        if (req.isActive() != null && !req.isActive()) {
            isActive = false;
            statusStr = "DRAFT";
        }
        if (isActive && stockQuantity <= 0 && "ACTIVE".equalsIgnoreCase(statusStr)) {
            statusStr = "OUT_OF_STOCK";
        }

        product.setName(req.name().trim());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setOriginalPrice(req.compareAtPrice());
        product.setDiscountPercent(discountPercent);
        product.setStock(stockQuantity);
        product.setLowStockThreshold(req.lowStockThreshold() != null ? req.lowStockThreshold() : product.getLowStockThreshold());
        product.setStatus(statusStr);
        product.setIsActive(isActive);
        product.setCategory(cat);

        if (req.isFeatured() != null) product.setIsFeatured(req.isFeatured());
        if (req.weightGrams() != null) product.setWeightGrams(req.weightGrams());
        if (req.lengthCm() != null) product.setLengthCm(req.lengthCm());
        if (req.widthCm() != null) product.setWidthCm(req.widthCm());
        if (req.heightCm() != null) product.setHeightCm(req.heightCm());
        if (req.materials() != null) product.setMaterial(req.materials());
        if (req.dimensions() != null) product.setDimensions(req.dimensions());

        // 5. Update images
        if (req.imageUrls() != null) {
            product.getImages().clear();
            for (int i = 0; i < req.imageUrls().size(); i++) {
                String imgUrl = req.imageUrls().get(i);
                if (imgUrl != null && !imgUrl.isBlank()) {
                    product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(imgUrl.trim())
                        .isPrimary(i == 0)
                        .displayOrder(i)
                        .build());
                }
            }
        }

        Product saved = productRepository.save(product);
        auditService.log(auth.getName(), "ADMIN_UPDATED_PRODUCT", "Product", saved.getId(),
            "Updated product " + saved.getName() + " with price ₹" + saved.getPrice() + " (status: " + statusStr + ")", null);

        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/products/{id}/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleProduct(Authentication auth, @PathVariable Long id) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found with ID: " + id));
        p.setIsActive(!p.getIsActive());
        p.setStatus(p.getIsActive() ? (p.getStock() > 0 ? "ACTIVE" : "OUT_OF_STOCK") : "DRAFT");
        productRepository.save(p);
        auditService.log(auth.getName(), "ADMIN_TOGGLED_PRODUCT", "Product", id,
            "Toggled active status to " + p.getIsActive() + " (status: " + p.getStatus() + ")", null);
        return ResponseEntity.ok(Map.of("id", id, "isActive", p.getIsActive(), "status", p.getStatus()));
    }

    @DeleteMapping("/products/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteProduct(Authentication auth, @PathVariable Long id) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found with ID: " + id));

        boolean hasOrders = orderRepository.existsByProductId(id);
        if (hasOrders) {
            // Price & order history integrity: archive rather than hard-deleting
            p.setIsActive(false);
            p.setStatus("ARCHIVED");
            productRepository.save(p);
            auditService.log(auth.getName(), "ADMIN_ARCHIVED_PRODUCT", "Product", id,
                "Archived product with existing order history: " + p.getName(), null);
            return ResponseEntity.ok(Map.of(
                "message", "Product has historical order records; archived and removed from storefront catalog",
                "archived", true,
                "id", id
            ));
        } else {
            productRepository.delete(p);
            auditService.log(auth.getName(), "ADMIN_DELETED_PRODUCT", "Product", id,
                "Permanently deleted product: " + p.getName(), null);
            return ResponseEntity.ok(Map.of(
                "message", "Product successfully deleted",
                "deleted", true,
                "id", id
            ));
        }
    }

    @PostMapping("/products/upload-image")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            Authentication auth,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw RexxoException.badRequest("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw RexxoException.badRequest("Only image files (JPEG, PNG, WEBP, GIF) are allowed");
        }

        try {
            // Generate clean filename
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "product.jpg";
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            String filename = "prod_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Target directory: frontend public images directory for instant Next.js serving
            Path publicImagesPath = Paths.get("frontend", "public", "images", "uploads");
            if (!Files.exists(publicImagesPath)) {
                // If relative to backend, resolve relative to project root
                Path projectRoot = Paths.get("..", "frontend", "public", "images", "uploads");
                if (Files.exists(Paths.get("..", "frontend"))) {
                    publicImagesPath = projectRoot;
                }
                Files.createDirectories(publicImagesPath);
            }

            Path targetPath = publicImagesPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String webUrl = "/images/uploads/" + filename;
            return ResponseEntity.ok(Map.of(
                "url", webUrl,
                "filename", filename,
                "message", "Image uploaded successfully"
            ));
        } catch (IOException e) {
            throw RexxoException.internal("Failed to save uploaded image: " + e.getMessage());
        }
    }

    @GetMapping({"/inventory", "/inventory/low-stock"})
    public ResponseEntity<InventorySummaryDto> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        List<Product> all = productRepository.findAll();
        long lowStockCount = all.stream().filter(p -> p.getIsActive() && p.getStock() > 0 && p.getStock() < 10).count();
        long outOfStockCount = all.stream().filter(p -> p.getIsActive() && p.getStock() <= 0).count();
        long totalStock = all.stream().mapToLong(Product::getStock).sum();

        Page<Product> paginated = productRepository.findAll(PageRequest.of(page, size, Sort.by("stock").ascending()));
        return ResponseEntity.ok(new InventorySummaryDto(totalStock, lowStockCount, outOfStockCount, paginated));
    }

    @PatchMapping("/inventory/{id}/stock")
    @Transactional
    public ResponseEntity<Map<String, Object>> adjustStock(Authentication auth,
                                                           @PathVariable Long id,
                                                           @RequestBody Map<String, Integer> body) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));
        int newStock = body.getOrDefault("stock", p.getStock());
        if (newStock < 0) {
            throw RexxoException.badRequest("Stock cannot be negative");
        }
        int oldStock = p.getStock();
        p.setStock(newStock);
        productRepository.save(p);

        auditService.log(auth.getName(), "ADMIN_CHANGED_STOCK", "Product", id,
            "Adjusted stock for " + p.getName() + " from " + oldStock + " to " + newStock, null);

        return ResponseEntity.ok(Map.of("id", id, "stock", newStock, "message", "Stock updated successfully"));
    }

    // ─── 4. Returns & Refunds Management ─────────────────────────────────────
    @GetMapping("/returns")
    public ResponseEntity<Page<ReturnRequest>> getReturns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ReturnRequest.ReturnStatus returnStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                returnStatus = ReturnRequest.ReturnStatus.valueOf(status.toUpperCase());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(returnService.getAllReturns(returnStatus, pageable));
    }

    @PatchMapping("/returns/{id}/approve")
    public ResponseEntity<ReturnRequest> approveReturn(Authentication auth,
                                                       @PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("adminNotes") : null;
        return ResponseEntity.ok(returnService.adminApprove(id, auth.getName(), notes));
    }

    @PatchMapping("/returns/{id}/reject")
    public ResponseEntity<ReturnRequest> rejectReturn(Authentication auth,
                                                      @PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String reason = body.get("rejectionReason");
        return ResponseEntity.ok(returnService.adminReject(id, auth.getName(), reason));
    }

    @PatchMapping("/returns/{id}/pickup")
    public ResponseEntity<ReturnRequest> schedulePickup(Authentication auth,
                                                        @PathVariable Long id,
                                                        @RequestBody Map<String, String> body) {
        String courier = body.getOrDefault("courier", "Shiprocket Return");
        String awb = body.getOrDefault("awb", "RET-" + System.currentTimeMillis());
        return ResponseEntity.ok(returnService.adminSchedulePickup(id, auth.getName(), courier, awb));
    }

    @PatchMapping("/returns/{id}/receive")
    public ResponseEntity<ReturnRequest> markReceived(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(returnService.adminMarkReceived(id, auth.getName()));
    }

    @PatchMapping("/returns/{id}/refund")
    public ResponseEntity<ReturnRequest> approveRefund(Authentication auth,
                                                       @PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : "Approved by Admin";
        return ResponseEntity.ok(returnService.adminApproveRefund(id, auth.getName(), reason));
    }

    @GetMapping("/refunds")
    public ResponseEntity<Page<Refund>> allRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(refundRepository.findAll(pageable));
    }

    // ─── 5. Reviews Moderation ───────────────────────────────────────────────
    @GetMapping("/reviews")
    public ResponseEntity<Page<Review>> allReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewRepository.findAll(
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PatchMapping("/reviews/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateReviewStatus(Authentication auth,
                                                                  @PathVariable Long id,
                                                                  @RequestBody Map<String, String> body) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Review not found"));

        String statusStr = body.get("status");
        if (statusStr != null) {
            Review.ReviewStatus status = Review.ReviewStatus.valueOf(statusStr.toUpperCase());
            review.setStatus(status);
            review.setIsApproved(status == Review.ReviewStatus.APPROVED);
        } else if (body.containsKey("isApproved")) {
            boolean approved = Boolean.parseBoolean(body.get("isApproved"));
            review.setIsApproved(approved);
            review.setStatus(approved ? Review.ReviewStatus.APPROVED : Review.ReviewStatus.REJECTED);
        }
        reviewRepository.save(review);
        auditService.log(auth.getName(), "ADMIN_MODERATED_REVIEW", "Review", id,
            "Review status set to " + review.getStatus(), null);
        return ResponseEntity.ok(Map.of("id", id, "status", review.getStatus().name(), "isApproved", review.getIsApproved()));
    }

    @DeleteMapping("/reviews/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteReview(Authentication auth, @PathVariable Long id) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Review not found"));
        reviewRepository.delete(review);
        auditService.log(auth.getName(), "ADMIN_DELETED_REVIEW", "Review", id,
            "Deleted review #" + id, null);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }

    // ─── 6. Analytics ────────────────────────────────────────────────────────
    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsService.AnalyticsSummaryDto> getAnalytics(
            @RequestParam(defaultValue = "30d") String range) {
        return ResponseEntity.ok(analyticsService.getAnalytics(range));
    }

    // ─── 7. Customers & Categories ───────────────────────────────────────────
    @GetMapping("/customers")
    public ResponseEntity<Page<CustomerDto>> customers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(userRepository.findAll(pageable).map(u ->
            new CustomerDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getIsActive(), u.getCreatedAt())
        ));
    }

    @PatchMapping("/customers/{id}/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleCustomer(Authentication auth, @PathVariable Long id) {
        User u = userRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Customer not found"));
        u.setIsActive(!u.getIsActive());
        userRepository.save(u);
        auditService.log(auth.getName(), "ADMIN_TOGGLED_CUSTOMER", "User", id,
            "Toggled active to " + u.getIsActive(), null);
        return ResponseEntity.ok(Map.of("id", id, "isActive", u.getIsActive()));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/categories")
    @Transactional
    public ResponseEntity<Category> createCategory(Authentication auth, @RequestBody Category cat) {
        if (cat.getSlug() == null || cat.getSlug().isBlank()) {
            cat.setSlug(cat.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-"));
        }
        Category saved = categoryRepository.save(cat);
        auditService.log(auth.getName(), "ADMIN_CREATED_CATEGORY", "Category", saved.getId(),
            "Created category " + saved.getName(), null);
        return ResponseEntity.ok(saved);
    }

    // ─── 8. Settings & Audit Logs ────────────────────────────────────────────
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AdminAuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @GetMapping({"/integrations", "/settings/integrations"})
    public ResponseEntity<Map<String, Object>> getIntegrationsStatus() {
        boolean shiprocketReady = shiprocketConfig != null && shiprocketConfig.isConfigured();
        boolean razorpayReady = razorpayKeyId != null && !razorpayKeyId.isBlank()
            && !razorpayKeyId.equalsIgnoreCase("placeholder")
            && !razorpayKeyId.startsWith("rzp_test_placeholder");
        boolean cloudinaryReady = cloudinaryCloudName != null && !cloudinaryCloudName.isBlank()
            && !cloudinaryCloudName.equalsIgnoreCase("placeholder")
            && cloudinaryApiKey != null && !cloudinaryApiKey.isBlank()
            && !cloudinaryApiKey.equalsIgnoreCase("placeholder");
        boolean emailReady = emailProvider != null && emailProvider.isConfigured();
        boolean smsReady = smsProviders != null && smsProviders.stream().anyMatch(SmsProvider::isConfigured);
        String smsProviderName = (smsProviders != null && !smsProviders.isEmpty())
            ? smsProviders.stream().filter(SmsProvider::isConfigured).findFirst().map(SmsProvider::getProviderName).orElse("Fast2SMS")
            : "Fast2SMS";
        boolean whatsappReady = whatsAppService != null && whatsAppService.isConfigured();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("shiprocket", Map.of(
            "configured", shiprocketReady,
            "status", shiprocketReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", "Shiprocket Logistics",
            "details", shiprocketReady
                ? "Active: Pincode serviceability & tracking engine with automated reverse pickup and AWB generation."
                : "Not Configured: Set SHIPROCKET_EMAIL and SHIPROCKET_PASSWORD in backend environment."
        ));
        response.put("razorpay", Map.of(
            "configured", razorpayReady,
            "status", razorpayReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", "Razorpay Payments",
            "keyId", razorpayReady ? (razorpayKeyId.length() > 8 ? razorpayKeyId.substring(0, 8) + "..." : razorpayKeyId) : "Not set",
            "details", razorpayReady
                ? "Active: Razorpay payment gateway, automated refunds and webhook signatures verified."
                : "Not Configured: Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in backend environment."
        ));
        response.put("cloudinary", Map.of(
            "configured", cloudinaryReady,
            "status", cloudinaryReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", "Cloudinary Media Storage",
            "details", cloudinaryReady
                ? "Active: Cloud image optimization and media transformation pipeline active."
                : "Not Configured: Set CLOUDINARY_CLOUD_NAME and CLOUDINARY_API_KEY in backend environment."
        ));
        response.put("email", Map.of(
            "configured", emailReady,
            "status", emailReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", "SMTP / JavaMail",
            "details", emailReady
                ? "Active: SMTP mail gateway ready for transactional notices and receipts."
                : "Not Configured: Set MAIL_HOST, MAIL_USERNAME, and MAIL_PASSWORD in backend environment."
        ));
        response.put("sms", Map.of(
            "configured", smsReady,
            "status", smsReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", smsProviderName,
            "details", smsReady
                ? "Active: SMS delivery gateway active for dispatch alerts."
                : "Not Configured: Set SMS_API_KEY in backend environment."
        ));
        response.put("whatsapp", Map.of(
            "configured", whatsappReady,
            "status", whatsappReady ? "CONFIGURED" : "NOT_CONFIGURED",
            "provider", "Meta Cloud WhatsApp API",
            "details", whatsappReady
                ? "Active: Meta Cloud WhatsApp API sending automated order lifecycle updates."
                : "Not Configured: Set WHATSAPP_API_URL and WHATSAPP_ACCESS_TOKEN in backend environment."
        ));

        return ResponseEntity.ok(response);
    }

    private AdminOrderDto toAdminOrderDto(Order o) {
        String awb = o.getShipment() != null ? o.getShipment().getAwbNumber() : null;
        String courier = o.getShipment() != null ? o.getShipment().getCourierName() : null;
        String paymentStatus = o.getPayment() != null && o.getPayment().getStatus() != null
            ? o.getPayment().getStatus().name() : (o.getPaymentMethod() != null ? o.getPaymentMethod() : "PENDING");

        String customerName = o.getUser() != null && o.getUser().getName() != null
            ? o.getUser().getName()
            : (o.getShippingName() != null ? o.getShippingName() : "Customer");
        String customerEmail = o.getUser() != null && o.getUser().getEmail() != null
            ? o.getUser().getEmail()
            : "N/A";
        int itemCount = o.getItems() != null ? o.getItems().size() : 0;

        return new AdminOrderDto(
            o.getId(), o.getOrderNumber(), customerName, customerEmail,
            o.getStatus().name(), paymentStatus, courier, awb, o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO, itemCount, o.getCreatedAt()
        );
    }

    @PostMapping("/users/create-admin")
    @Transactional
    public ResponseEntity<?> createAdmin(Authentication auth, @Valid @RequestBody CreateAdminRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (adminRepository.existsByEmail(normalizedEmail) || userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "An account with this email address already exists."
            ));
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Admin admin = Admin.builder()
            .name(request.name().trim())
            .email(normalizedEmail)
            .passwordHash(hashedPassword)
            .role("ADMIN")
            .enabled(true)
            .build();
        adminRepository.save(admin);

        User user = User.builder()
            .name(request.name().trim())
            .email(normalizedEmail)
            .password(hashedPassword)
            .role(User.Role.ADMIN)
            .isActive(true)
            .emailVerified(true)
            .phoneVerified(true)
            .build();
        userRepository.save(user);

        auditService.log(auth.getName(), "ADMIN_CREATED_SUBADMIN", "Admin", admin.getId(),
            "Created administrator: " + normalizedEmail, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Administrator account created successfully",
            "email", normalizedEmail,
            "name", admin.getName()
        ));
    }

    public record CreateAdminRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
    ) {}

    // DTO records
    public record DashboardDto(
        BigDecimal totalRevenue, long totalOrders, long totalProducts,
        long totalCustomers, long pendingOrders, long deliveredOrders,
        long cancelledOrders, long returnRequests, BigDecimal refundAmount,
        long lowStockProducts, long outOfStockProducts,
        List<AdminOrderDto> recentOrders, List<CustomerDto> recentCustomers,
        List<AnalyticsService.TopProductDto> topProducts
    ) {}

    public record AdminOrderDto(
        Long id, String orderNumber, String customerName, String customerEmail,
        String status, String paymentStatus, String courier, String awb,
        BigDecimal total, int itemCount, LocalDateTime createdAt
    ) {}

    public record AdminOrderDetailDto(
        Long id, String orderNumber, Long customerId, String customerName,
        String customerEmail, String customerPhone, String status,
        String paymentStatus, String paymentMethod, String courier, String awb,
        String shipmentStatus, String returnStatus, BigDecimal subtotal,
        BigDecimal shippingFee, BigDecimal discountAmount, BigDecimal total,
        String shippingName, String shippingPhone, String shippingAddress,
        String shippingCity, String shippingState, String shippingPincode,
        List<AdminOrderItemDto> items, LocalDateTime createdAt
    ) {}

    public record AdminOrderItemDto(
        Long id, Long productId, String productName, String imageUrl,
        BigDecimal unitPrice, Integer quantity, BigDecimal subtotal
    ) {}

    public record CustomerDto(
        Long id, String name, String email, String phone,
        Boolean isActive, LocalDateTime createdAt
    ) {}

    public record InventorySummaryDto(
        long totalUnitsInStock,
        long lowStockCount,
        long outOfStockCount,
        Page<Product> products
    ) {}

    public record ProductRequest(
        @NotBlank String name,
        String slug,
        String sku,
        String description,
        String shortDescription,
        @NotNull BigDecimal price,
        BigDecimal compareAtPrice,
        Integer stock,
        Integer stockQuantity,
        @NotNull Long categoryId,
        Boolean isActive,
        Boolean isFeatured,
        String status,
        Integer lowStockThreshold,
        Integer weightGrams,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        String materials,
        String dimensions,
        List<String> imageUrls
    ) {
        public int resolveStock(int fallback) {
            if (stockQuantity != null) return stockQuantity;
            if (stock != null) return stock;
            return fallback;
        }
    }
}
