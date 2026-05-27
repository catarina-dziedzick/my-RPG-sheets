package com.myrpgsheets.controller;

import com.myrpgsheets.model.User;
import com.myrpgsheets.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute User user,
            BindingResult result,
            Model model
    ) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(user);
            return "redirect:/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        Optional<User> userOptional = userService.authenticate(email, password);

        if (userOptional.isPresent()) {
            session.setAttribute("loggedUser", userOptional.get());
            return "redirect:/characters";
        }

        model.addAttribute("error", "Invalid email or password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}