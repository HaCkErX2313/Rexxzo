package com.rexxo.config;

import com.rexxo.entity.Category;
import com.rexxo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and seeding canonical REXXZO categories...");

        record CanonicalCategory(String name, String slug, String description, String imageUrl, int order) {}

        List<CanonicalCategory> canonicalCategories = List.of(
            new CanonicalCategory("Home Decor", "home-decor", "Handcrafted ceramic vessels and sculptural essentials", "/images/desk_lamp.jpg", 1),
            new CanonicalCategory("Kitchen & Dining", "kitchen-dining", "Minimalist drinkware and artisanal dining ware", "/images/water_bottle.jpg", 2),
            new CanonicalCategory("Storage & Organizers", "storage-organizers", "Functional architectural organization for calm spaces", "/images/storage_box.jpg", 3),
            new CanonicalCategory("Lighting", "lighting", "Atmospheric dimmable lighting with tactile materiality", "/images/desk_lamp.jpg", 4),
            new CanonicalCategory("Lifestyle", "lifestyle", "Acoustic fidelity wrapped in soft tactile aluminum", "/images/headphones.jpg", 5),
            new CanonicalCategory("Accessories", "accessories", "Refined daily essentials for your workspace and home", "/images/hero_banner.jpg", 6),
            new CanonicalCategory("Gift Corner", "gift-corner", "Thoughtful gifts for every occasion. Curated design objects, sculptural lighting, and artisanal lifestyle essentials.", "/images/hero_banner.jpg", 7)
        );

        for (CanonicalCategory catData : canonicalCategories) {
            categoryRepository.findBySlug(catData.slug()).ifPresentOrElse(
                existing -> {
                    // Update details if needed
                    existing.setIsActive(true);
                    if (existing.getDisplayOrder() == null || existing.getDisplayOrder() == 0) {
                        existing.setDisplayOrder(catData.order());
                    }
                    categoryRepository.save(existing);
                },
                () -> {
                    Category newCat = Category.builder()
                        .name(catData.name())
                        .slug(catData.slug())
                        .description(catData.description())
                        .imageUrl(catData.imageUrl())
                        .displayOrder(catData.order())
                        .isActive(true)
                        .build();
                    categoryRepository.save(newCat);
                    log.info("Created canonical category: {} ({})", catData.name(), catData.slug());
                }
            );
        }
    }
}
