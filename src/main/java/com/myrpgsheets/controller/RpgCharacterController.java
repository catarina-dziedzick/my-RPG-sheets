package com.myrpgsheets.controller;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import com.myrpgsheets.service.RpgCharacterService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/characters")
public class RpgCharacterController {

    private final RpgCharacterService rpgCharacterService;

    public RpgCharacterController(RpgCharacterService rpgCharacterService) {
        this.rpgCharacterService = rpgCharacterService;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("characters", rpgCharacterService.findByUser(user));

        return "characters/dashboard";
    }

    @GetMapping("/new")
    public String newCharacter(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("character", new RpgCharacter());
        return "characters/form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute RpgCharacter rpgCharacter,
            BindingResult result,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "characters/form";
        }

        rpgCharacter.setUser(user);
        rpgCharacterService.save(rpgCharacter);

        return "redirect:/characters";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(id);

        if (characterOptional.isEmpty()) {
            return "redirect:/characters";
        }

        model.addAttribute("character", characterOptional.get());
        return "characters/view";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        rpgCharacterService.delete(id);
        return "redirect:/characters";
    }
}