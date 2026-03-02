package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.model.Product;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.service.CartService;
import com.minuStore.MiNu.service.OrderService;
import com.minuStore.MiNu.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final ProductService productService;

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            RedirectAttributes redirectAttributes) {
        cartService.addItem(productId, quantity);
        redirectAttributes.addFlashAttribute("message", "Product added to cart!");
        return "redirect:/products";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        List<CartItemView> cartItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var item : cartService.getItems()) {
            Product product = productService.findById(item.getProductId()).orElse(null);
            if (product != null) {
                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                cartItems.add(new CartItemView(product, item.getQuantity(), subtotal));
                total = total.add(subtotal);
            }
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", total);
        model.addAttribute("itemCount", cartService.getItemCount());
        return "orders/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId) {
        cartService.removeItem(productId);
        return "redirect:/cart";
    }

    @PostMapping("/orders/checkout")
    public String checkout(@AuthenticationPrincipal User customer,
                           RedirectAttributes redirectAttributes) {
        try {
            orderService.createOrder(customer, cartService.getItems());
            cartService.clear();
            redirectAttributes.addFlashAttribute("message", "Order placed successfully!");
            return "redirect:/orders";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/orders")
    public String myOrders(@AuthenticationPrincipal User customer, Model model) {
        model.addAttribute("orders", orderService.findByCustomer(customer.getId()));
        return "orders/list";
    }

    public record CartItemView(Product product, Integer quantity, BigDecimal subtotal) {}
}
