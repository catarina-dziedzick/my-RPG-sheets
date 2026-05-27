package com.myrpgsheets.controller;

import com.myrpgsheets.model.*;
import com.myrpgsheets.service.CharacterSpellService;
import com.myrpgsheets.service.InventoryItemService;
import com.myrpgsheets.service.RpgCharacterService;
import com.myrpgsheets.service.SpellSlotService;
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
    private final InventoryItemService inventoryItemService;
    private final CharacterSpellService characterSpellService;
    private final SpellSlotService spellSlotService;

    public RpgCharacterController(
            RpgCharacterService rpgCharacterService,
            InventoryItemService inventoryItemService,
            CharacterSpellService characterSpellService,
            SpellSlotService spellSlotService)
    {
        this.rpgCharacterService = rpgCharacterService;
        this.inventoryItemService = inventoryItemService;
        this.characterSpellService = characterSpellService;
        this.spellSlotService = spellSlotService;
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

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        model.addAttribute("character", character);
        model.addAttribute("items", inventoryItemService.findByCharacter(character));
        model.addAttribute("spells", characterSpellService.findByCharacter(character));
        model.addAttribute("spellSlots", spellSlotService.findByCharacter(character));

        model.addAttribute("newItem", new InventoryItem());
        model.addAttribute("newSpell", new CharacterSpell());
        model.addAttribute("newSpellSlot", new SpellSlot());

        return "characters/view";
    }

    @GetMapping("/edit/{id}")
    public String editCharacter(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(id);

        if (characterOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        model.addAttribute("character", character);

        return "characters/form";
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

    @PostMapping("/{characterId}/items/save")
    public String saveItem(
            @PathVariable Long characterId,
            @ModelAttribute InventoryItem item,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        item.setCharacter(character);

        if (item.getQuantity() == null) {
            item.setQuantity(1);
        }

        inventoryItemService.save(item);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spells/save")
    public String saveSpell(
            @PathVariable Long characterId,
            @ModelAttribute CharacterSpell spell,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        spell.setCharacter(character);

        if (spell.getCantrip() == null) {
            spell.setCantrip(false);
        }

        if (Boolean.TRUE.equals(spell.getCantrip())) {
            spell.setSpellCircle(0);
        }

        characterSpellService.save(spell);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spell-slots/save")
    public String saveSpellSlot(
            @PathVariable Long characterId,
            @ModelAttribute SpellSlot spellSlot,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        spellSlot.setCharacter(character);

        if (spellSlot.getUsedSlots() == null) {
            spellSlot.setUsedSlots(0);
        }

        if (spellSlot.getTotalSlots() == null) {
            spellSlot.setTotalSlots(0);
        }

        spellSlotService.save(spellSlot);

        return "redirect:/characters/view/" + characterId;
    }

    @GetMapping("/{characterId}/items/delete/{itemId}")
    public String deleteItem(
            @PathVariable Long characterId,
            @PathVariable Long itemId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        inventoryItemService.delete(itemId);

        return "redirect:/characters/view/" + characterId;
    }

    @GetMapping("/{characterId}/spells/delete/{spellId}")
    public String deleteSpell(
            @PathVariable Long characterId,
            @PathVariable Long spellId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        characterSpellService.delete(spellId);

        return "redirect:/characters/view/" + characterId;
    }

    @GetMapping("/{characterId}/spell-slots/delete/{slotId}")
    public String deleteSpellSlot(
            @PathVariable Long characterId,
            @PathVariable Long slotId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        spellSlotService.delete(slotId);

        return "redirect:/characters/view/" + characterId;
    }
}