package com.minuStore.MiNu.config;

import com.minuStore.MiNu.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        User user = (User) authentication.getPrincipal();

        String redirectUrl = switch (user.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case SELLER -> "/seller/store";
            case CUSTOMER -> "/products";
        };

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
