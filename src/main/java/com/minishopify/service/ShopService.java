package com.minishopify.service;

import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.model.Tag;
import com.minishopify.repository.ShopRepository;
import com.minishopify.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Handles shop related work
@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final TagRepository tagRepository;

    public ShopService(ShopRepository shopRepository, TagRepository tagRepository) {
        this.shopRepository = shopRepository;
        this.tagRepository = tagRepository;
    }

    // Creates a shop from the submitted form data and returns the shop
    @Transactional
    public Shop createShop(ShopForm form) {
        String shopName = form.getName().trim();
        Shop shop = new Shop(shopName, form.getDescription());

        for (String tagName : normalizeTags(form.getTags())) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            shop.addTag(tag);
        }

        return shopRepository.save(shop);
    }

    @Transactional(readOnly = true)
    public List<Shop> searchShops(String query) {

        String keyword = query.trim();
        Set<Shop> results = new LinkedHashSet<>(shopRepository.findByNameContainingIgnoreCase(keyword));
        results.addAll(shopRepository.findByTags_NameIgnoreCase(keyword));
        return new ArrayList<>(results);
    }

    private Set<String> normalizeTags(Iterable<String> submittedTags) {
        Set<String> normalizedTags = new LinkedHashSet<>();

        if (submittedTags == null) {
            return normalizedTags;
        }

        for (String submittedTag : submittedTags) {
            if (submittedTag == null) {
                continue;
            }

            String tagName = submittedTag.trim().toLowerCase(Locale.ROOT);
            if (!tagName.isEmpty()) {
                normalizedTags.add(tagName);
            }
        }

        return normalizedTags;
    }

}
