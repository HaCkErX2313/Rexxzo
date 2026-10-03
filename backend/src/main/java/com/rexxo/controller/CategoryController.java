package com.rexxo.controller;

import com.rexxo.entity.Category;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.CategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAll() {
        List<CategoryDto> list = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
            .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryDto>> getAllForAdmin() {
        List<CategoryDto> list = categoryRepository.findAll()
            .stream().map(this::toDto).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryDto> getBySlug(@PathVariable String slug) {
        Category cat = categoryRepository.findBySlug(slug)
            .filter(Category::getIsActive)
            .orElseThrow(() -> RexxoException.notFound("Category not found: " + slug));
        return ResponseEntity.ok(toDto(cat));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw RexxoException.conflict("Slug already exists: " + request.slug());
        }
        Category cat = Category.builder()
            .name(request.name())
            .slug(request.slug())
            .description(request.description())
            .imageUrl(request.imageUrl())
            .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
            .isActive(true)
            .build();
        return ResponseEntity.ok(toDto(categoryRepository.save(cat)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<CategoryDto> update(@PathVariable Long id,
                                               @Valid @RequestBody CategoryRequest request) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Category not found"));
        cat.setName(request.name());
        cat.setDescription(request.description());
        cat.setImageUrl(request.imageUrl());
        if (request.displayOrder() != null) cat.setDisplayOrder(request.displayOrder());
        return ResponseEntity.ok(toDto(categoryRepository.save(cat)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<CategoryDto> toggle(@PathVariable Long id) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Category not found"));
        cat.setIsActive(!Boolean.TRUE.equals(cat.getIsActive()));
        return ResponseEntity.ok(toDto(categoryRepository.save(cat)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Category not found"));
        cat.setIsActive(false);
        categoryRepository.save(cat);
        return ResponseEntity.ok(Map.of("message", "Category deactivated"));
    }

    private CategoryDto toDto(Category c) {
        int count = c.getProducts() != null ? c.getProducts().size() : 0;
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(),
            c.getDescription(), c.getImageUrl(), c.getDisplayOrder(), c.getIsActive(), count);
    }

    record CategoryDto(Long id, String name, String slug, String description,
                       String imageUrl, Integer displayOrder, Boolean isActive, int productCount) {}

    record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 120) String slug,
        String description,
        String imageUrl,
        Integer displayOrder
    ) {}
}
