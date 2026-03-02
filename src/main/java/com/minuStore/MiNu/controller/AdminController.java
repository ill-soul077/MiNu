package com.minuStore.MiNu.controller;

import com.minuStore.MiNu.model.OrderStatus;
import com.minuStore.MiNu.service.OrderService;
import com.minuStore.MiNu.service.ProductService;
import com.minuStore.MiNu.service.StoreService;
import com.minuStore.MiNu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final StoreService storeService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.countAll());
        model.addAttribute("unverifiedCount", userService.countUnverified());
        model.addAttribute("storeCount", storeService.findAll().size());
        model.addAttribute("orderCount", orderService.findAll().size());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/verify")
    public String verifyUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.verifyUser(id);
            redirectAttributes.addFlashAttribute("message", "User verified successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reject")
    public String rejectUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.rejectUser(id);
            redirectAttributes.addFlashAttribute("message", "User rejected and removed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/stores")
    public String listStores(Model model) {
        model.addAttribute("stores", storeService.findAll());
        return "admin/stores";
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductAsAdmin(id);
            redirectAttributes.addFlashAttribute("message", "Product deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.findAll());
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, OrderStatus.valueOf(status));
            redirectAttributes.addFlashAttribute("message", "Order status updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
