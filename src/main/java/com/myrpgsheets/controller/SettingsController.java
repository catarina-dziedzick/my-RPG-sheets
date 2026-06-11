package com.myrpgsheets.controller;

import com.myrpgsheets.model.User;
import com.myrpgsheets.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;

    public SettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String settings(HttpSession session, Model model) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        Optional<User> userOptional = userService.findById(loggedUser.getId());

        if (userOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        User user = userOptional.get();

        session.setAttribute("loggedUser", user);
        model.addAttribute("user", user);

        return "settings";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String userName,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String preferredSystem,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            User updatedUser = userService.updateProfile(
                    loggedUser,
                    name,
                    email,
                    userName,
                    displayName,
                    bio,
                    preferredSystem
            );

            session.setAttribute("loggedUser", updatedUser);
            redirectAttributes.addFlashAttribute("profileSuccess", "Perfil atualizado com sucesso.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
        }

        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User loggedUser = (User) session.getAttribute("loggedUser");

        if (loggedUser == null) {
            return "redirect:/login";
        }

        try {
            User updatedUser = userService.changePassword(
                    loggedUser,
                    currentPassword,
                    newPassword,
                    confirmPassword
            );

            session.setAttribute("loggedUser", updatedUser);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Senha alterada com sucesso.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }

        return "redirect:/settings";
    }
}