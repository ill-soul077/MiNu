package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.dto.ProductDto;
import com.minuStore.MiNu.dto.StoreDto;
import com.minuStore.MiNu.model.Product;
import com.minuStore.MiNu.model.Store;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.service.ProductService;
import com.minuStore.MiNu.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerController {

    private final ProductService productService;
    private final StoreService storeService;

    // ─── Store Management ───────────────────────────────────────────

    @GetMapping("/store")
    public String viewStore(@AuthenticationPrincipal User seller, Model model) {
        if (!seller.isVerified()) {
            model.addAttribute("error", "Your account must be verified by an admin before you can create a store.");
            return "seller/store-form";
        }
        Optional<Store> store = storeService.findBySellerId(seller.getId());
        if (store.isPresent()) {
            model.addAttribute("store", store.get());
            model.addAttribute("storeDto", new StoreDto(
                    store.get().getId(),
                    store.get().getName(),
                    store.get().getDescription()
            ));
            model.addAttribute("hasStore", true);
        } else {
            model.addAttribute("storeDto", new StoreDto());
            model.addAttribute("hasStore", false);
        }
        return "seller/store";
    }

    @PostMapping("/store")
    public String createStore(@Valid @ModelAttribute("storeDto") StoreDto dto,
                              BindingResult result,
                              @AuthenticationPrincipal User seller,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "seller/store";
        }
        try {
            storeService.createStore(dto, seller);
            redirectAttributes.addFlashAttribute("message", "Store created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/store";
    }

    @PostMapping("/store/update")
    public String updateStore(@ModelAttribute("storeDto") StoreDto dto,
                              @AuthenticationPrincipal User seller,
                              RedirectAttributes redirectAttributes) {
        try {
            storeService.updateStore(dto, seller);
            redirectAttributes.addFlashAttribute("message", "Store updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/store";
    }

    // ─── Product Management ─────────────────────────────────────────

    @GetMapping("/products")
    public String myProducts(@AuthenticationPrincipal User seller, Model model) {
        List<Product> products = productService.findBySeller(seller.getId());
        model.addAttribute("products", products);
        model.addAttribute("hasStore", storeService.sellerHasStore(seller.getId()));
        return "seller/products";
    }

    @GetMapping("/products/new")
    public String newProductForm(@AuthenticationPrincipal User seller, Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!seller.isVerified()) {
            redirectAttributes.addFlashAttribute("error", "Your account must be verified first.");
            return "redirect:/seller/products";
        }
        if (!storeService.sellerHasStore(seller.getId())) {
            redirectAttributes.addFlashAttribute("error", "You must create a store first.");
            return "redirect:/seller/store";
        }
        model.addAttribute("product", new ProductDto());
        return "seller/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@Valid @ModelAttribute("product") ProductDto dto,
                                BindingResult result,
                                @AuthenticationPrincipal User seller,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "seller/product-form";
        }
        try {
            productService.createProduct(dto, seller);
            redirectAttributes.addFlashAttribute("message", "Product created!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Long id,
                                  @AuthenticationPrincipal User seller,
                                  Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getStore().getSeller().getId().equals(seller.getId())) {
            return "redirect:/seller/products";
        }

        ProductDto dto = ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .build();

        model.addAttribute("product", dto);
        return "seller/product-form";
    }

    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") ProductDto dto,
                                BindingResult result,
                                @AuthenticationPrincipal User seller,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "seller/product-form";
        }
        try {
            productService.updateProduct(id, dto, seller);
            redirectAttributes.addFlashAttribute("message", "Product updated!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal User seller,
                                RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id, seller);
            redirectAttributes.addFlashAttribute("message", "Product deleted!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    @GetMapping("/orders")
    public String sellerOrders(@AuthenticationPrincipal User seller, Model model) {
        model.addAttribute("seller", seller);
        return "seller/orders";
    }
}