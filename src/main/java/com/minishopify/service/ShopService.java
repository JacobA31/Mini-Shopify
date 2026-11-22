package com.minishopify.service;

import com.minishopify.dto.ShopForm;
import com.minishopify.exception.ShopNotFoundException;
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
        addTags(shop, form.getTags());
        return shopRepository.save(shop);
    }

    // Finds a shop by id or throws ShopNotFoundException
    @Transactional(readOnly = true)
    public Shop getShop(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(shopId));
    }

    // The edit form pre-filled with the shop's info
    @Transactional(readOnly = true)
    public ShopForm getShopForm(Long shopId) {
        return toForm(getShop(shopId));
    }

    // Replaces the shop's info with the new form data
    @Transactional
    public Shop updateShop(Long shopId, ShopForm form) {
        Shop shop = getShop(shopId);
        shop.update(form.getName().trim(), form.getDescription());
        shop.clearTags();
        addTags(shop, form.getTags());
        return shop;
    }

    public boolean hasShops() {
        return shopRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public List<Shop> searchShops(String query) {
        String keyword = query.trim();
        Set<Shop> results = new LinkedHashSet<>(shopRepository.findByNameContainingIgnoreCase(keyword));
        results.addAll(shopRepository.findByTags_NameIgnoreCase(keyword));
        return new ArrayList<>(results);
    }

    // Copies a shop into a form.
    private static ShopForm toForm(Shop shop) {
        ShopForm form = new ShopForm();
        form.setName(shop.getName());
        form.setDescription(shop.getDescription());
        form.setTags(shop.getTags().stream().map(Tag::getName).sorted().toList());
        return form;
    }

    private void addTags(Shop shop, Iterable<String> submittedTags) {
        for (String tagName : normalizeTags(submittedTags)) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            shop.addTag(tag);
        }
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
