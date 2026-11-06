package com.minishopify.service;

import com.minishopify.dto.ShopForm;
import com.minishopify.model.Shop;
import com.minishopify.model.Tag;
import com.minishopify.repository.ShopRepository;
import com.minishopify.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    void createShopCleansUpAndDeduplicatesTags() {
        ShopForm form = new ShopForm();
        form.setName("  Corner Store  ");
        form.setDescription("A small local shop");
        form.setTags(List.of(" Coffee ", "coffee", ""));

        when(tagRepository.findByName("coffee")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class)))
                .thenAnswer(invocation -> (Tag) invocation.getArgument(0));
        when(shopRepository.save(any(Shop.class)))
                .thenAnswer(invocation -> (Shop) invocation.getArgument(0));

        Shop createdShop = shopService.createShop(form);

        assertEquals("Corner Store", createdShop.getName());
        assertEquals("A small local shop", createdShop.getDescription());
        assertEquals(1, createdShop.getTags().size());
        assertEquals("coffee", createdShop.getTags().iterator().next().getName());
        verify(tagRepository).save(any(Tag.class));
        verify(shopRepository).save(createdShop);
    }

    @Test
    void createShopWithNoTagsSavesShopWithoutLookingUpTags() {
        ShopForm form = new ShopForm();
        form.setName("Tagless Shop");
        form.setTags(List.of());

        when(shopRepository.save(any(Shop.class)))
                .thenAnswer(invocation -> (Shop) invocation.getArgument(0));

        Shop createdShop = shopService.createShop(form);

        assertTrue(createdShop.getTags().isEmpty());
        verify(shopRepository).save(createdShop);
        verify(tagRepository, never()).findByName(anyString());
        verify(tagRepository, never()).save(any(Tag.class));
    }

}
