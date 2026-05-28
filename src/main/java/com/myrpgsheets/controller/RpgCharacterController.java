package com.myrpgsheets.controller;

import com.myrpgsheets.model.*;
import com.myrpgsheets.service.CharacterAbilityService;
import com.myrpgsheets.service.CharacterSpellService;
import com.myrpgsheets.service.InventoryItemService;
import com.myrpgsheets.service.RpgCharacterService;
import com.myrpgsheets.service.SpellSlotService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Controller
@RequestMapping("/characters")
public class RpgCharacterController {

    private final RpgCharacterService rpgCharacterService;
    private final InventoryItemService inventoryItemService;
    private final CharacterSpellService characterSpellService;
    private final SpellSlotService spellSlotService;
    private final CharacterAbilityService characterAbilityService;

    public RpgCharacterController(
            RpgCharacterService rpgCharacterService,
            InventoryItemService inventoryItemService,
            CharacterSpellService characterSpellService,
            SpellSlotService spellSlotService,
            CharacterAbilityService characterAbilityService)
    {
        this.rpgCharacterService = rpgCharacterService;
        this.inventoryItemService = inventoryItemService;
        this.characterSpellService = characterSpellService;
        this.spellSlotService = spellSlotService;
        this.characterAbilityService = characterAbilityService;
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
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "characters/form";
        }

        RpgCharacter characterToSave = rpgCharacter;

        if (rpgCharacter.getId() != null) {
            Optional<RpgCharacter> existingCharacterOptional = rpgCharacterService.findById(rpgCharacter.getId());

            if (existingCharacterOptional.isEmpty()) {
                return "redirect:/characters";
            }

            RpgCharacter existingCharacter = existingCharacterOptional.get();

            if (!existingCharacter.getUser().getId().equals(user.getId())) {
                return "redirect:/characters";
            }

            copyEditableFields(rpgCharacter, existingCharacter);
            characterToSave = existingCharacter;
        } else {
            characterToSave.setUser(user);
        }

        handleAvatarUpload(characterToSave, avatarFile);

        // normaliza HP atual se ultrapassar o máximo
        Integer maxHp = characterToSave.getMaxHitPoints();
        Integer curHp = characterToSave.getHitPoints();
        if (maxHp != null && maxHp > 0 && curHp != null && curHp > maxHp) {
            characterToSave.setHitPoints(maxHp);
        }

        rpgCharacterService.save(characterToSave);

        return "redirect:/characters";
    }

    @GetMapping(value = "/{id}/avatar")
    @ResponseBody
    public ResponseEntity<byte[]> avatar(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(id);

        if (characterOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RpgCharacter character = characterOptional.get();

        if (!character.getUser().getId().equals(user.getId()) || !character.hasAvatar()) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

        if (character.getAvatarContentType() != null && !character.getAvatarContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(character.getAvatarContentType());
            } catch (IllegalArgumentException ignored) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(character.getAvatarData());
    }

    @PostMapping("/{id}/avatar/remove")
    public String removeAvatar(@PathVariable Long id, HttpSession session) {
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

        character.setAvatarData(null);
        character.setAvatarContentType(null);
        rpgCharacterService.save(character);

        return "redirect:/characters/edit/" + id;
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

        List<CharacterSpell> spells = characterSpellService.findByCharacter(character);
        List<SpellSlot> spellSlots = spellSlotService.findByCharacter(character);
        Map<Integer, SpellSlot> spellSlotsByCircle = new TreeMap<>();

        for (SpellSlot slot : spellSlots) {
            Integer circle = slot.getSpellCircle();

            if (circle != null && circle > 0) {
                spellSlotsByCircle.putIfAbsent(circle, slot);
            }
        }

        List<Integer> availableSpellCircles = new ArrayList<>(spellSlotsByCircle.keySet());
        List<SpellSlot> availableSpellSlots = new ArrayList<>(spellSlotsByCircle.values());

        model.addAttribute("character", character);
        model.addAttribute("items", inventoryItemService.findByCharacter(character));
        model.addAttribute("spellSlots", spellSlots);
        model.addAttribute("spellSlotsByCircle", spellSlotsByCircle);
        model.addAttribute("availableSpellSlots", availableSpellSlots);

        model.addAttribute("spells", spells);
        model.addAttribute("availableSpellCircles", availableSpellCircles);

        model.addAttribute("newItem", new InventoryItem());
        model.addAttribute("newSpell", new CharacterSpell());
        model.addAttribute("newSpellSlot", new SpellSlot());

        model.addAttribute("abilities", characterAbilityService.findByCharacter(character));
        model.addAttribute("newAbility", new CharacterAbility());

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

    @PostMapping("/{characterId}/hit-points/adjust")
    public String adjustHitPoints(
            @PathVariable Long characterId,
            @RequestParam Integer delta,
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

        int currentHitPoints = character.getHitPoints() == null ? 0 : character.getHitPoints();
        int updatedHitPoints = Math.max(0, currentHitPoints + delta);
        updatedHitPoints = clampHp(character, updatedHitPoints);

        character.setHitPoints(updatedHitPoints);
        rpgCharacterService.save(character);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/hit-points/set")
    public String setHitPoints(
            @PathVariable Long characterId,
            @RequestParam Integer hitPoints,
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

        int normalizedHitPoints = hitPoints == null ? 0 : Math.max(0, hitPoints);
        normalizedHitPoints = clampHp(character, normalizedHitPoints);

        character.setHitPoints(normalizedHitPoints);
        rpgCharacterService.save(character);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/xp/adjust")
    public String adjustXp(
            @PathVariable Long characterId,
            @RequestParam Integer delta,
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

        int current = character.getExperiencePoints() == null ? 0 : character.getExperiencePoints();
        int updated = current + (delta == null ? 0 : delta);
        character.setExperiencePoints(clampXp(character, updated));
        rpgCharacterService.save(character);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/xp/set")
    public String setXp(
            @PathVariable Long characterId,
            @RequestParam Integer experiencePoints,
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

        int value = experiencePoints == null ? 0 : experiencePoints;
        character.setExperiencePoints(clampXp(character, value));
        rpgCharacterService.save(character);

        return "redirect:/characters/view/" + characterId;
    }

    private int clampXp(RpgCharacter character, int value) {
        int min = character.getCurrentLevelXp();
        int max = character.getMaxAllowedXp();
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private int clampHp(RpgCharacter character, int value) {
        int max = character.getMaxHitPoints() == null ? 0 : character.getMaxHitPoints();
        if (max <= 0) return value; // sem máximo definido, não limita
        if (value > max) return max;
        return value;
    }

    @PostMapping("/{characterId}/temp-hp/set")
    public String setTempHp(
            @PathVariable Long characterId,
            @RequestParam Integer temporaryHitPoints,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        Optional<RpgCharacter> opt = rpgCharacterService.findById(characterId);
        if (opt.isEmpty()) return "redirect:/characters";

        RpgCharacter character = opt.get();
        if (!character.getUser().getId().equals(user.getId())) return "redirect:/characters";

        int val = temporaryHitPoints == null ? 0 : Math.max(0, temporaryHitPoints);
        character.setTemporaryHitPoints(val);
        rpgCharacterService.save(character);
        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spell-slots/use")
    public String useSpellSlot(
            @PathVariable Long characterId,
            @RequestParam Integer spellCircle,
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

        Optional<SpellSlot> slotOptional = spellSlotService.findByCharacterAndSpellCircle(character, spellCircle);

        if (slotOptional.isPresent()) {
            SpellSlot slot = slotOptional.get();
            int total = slot.getTotalSlots() == null ? 0 : slot.getTotalSlots();
            int used = slot.getUsedSlots() == null ? 0 : slot.getUsedSlots();

            if (used < total) {
                slot.setUsedSlots(used + 1);
                spellSlotService.save(slot);
            }
        }

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spell-slots/toggle")
    public String toggleSpellSlot(
            @PathVariable Long characterId,
            @RequestParam Integer spellCircle,
            @RequestParam Integer slotIndex,
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

        Optional<SpellSlot> slotOptional = spellSlotService.findByCharacterAndSpellCircle(character, spellCircle);

        if (slotOptional.isEmpty()) {
            return "redirect:/characters/view/" + characterId;
        }

        SpellSlot slot = slotOptional.get();
        int total = slot.getTotalSlots() == null ? 0 : slot.getTotalSlots();
        int used = slot.getUsedSlots() == null ? 0 : slot.getUsedSlots();

        if (slotIndex == null || slotIndex < 1 || slotIndex > total) {
            return "redirect:/characters/view/" + characterId;
        }

        if (slotIndex <= used) {
            slot.setUsedSlots(slotIndex - 1);
        } else {
            slot.setUsedSlots(slotIndex);
        }

        spellSlotService.save(slot);

        return "redirect:/characters/view/" + characterId;
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
            RedirectAttributes redirectAttributes,
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
        } else {
            if (spell.getSpellCircle() == null || spell.getSpellCircle() < 1) {
                redirectAttributes.addFlashAttribute("spellError", "Selecione um circulo valido ou marque como truque.");
                return "redirect:/characters/view/" + characterId;
            }

            if (!hasSpellSlotForCircle(character, spell.getSpellCircle())) {
                redirectAttributes.addFlashAttribute("spellError", "Cadastre o espaco desse circulo antes de adicionar a magia.");
                return "redirect:/characters/view/" + characterId;
            }
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

    @PostMapping("/{characterId}/items/update/{itemId}")
    public String updateItem(
            @PathVariable Long characterId,
            @PathVariable Long itemId,
            @ModelAttribute InventoryItem updatedItem,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<InventoryItem> itemOptional = inventoryItemService.findById(itemId);

        if (characterOptional.isEmpty() || itemOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();
        InventoryItem item = itemOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        if (!item.getCharacter().getId().equals(character.getId())) {
            return "redirect:/characters/view/" + characterId;
        }

        item.setName(updatedItem.getName());
        item.setQuantity(updatedItem.getQuantity());
        item.setDescription(updatedItem.getDescription());

        if (item.getQuantity() == null || item.getQuantity() < 1) {
            item.setQuantity(1);
        }

        inventoryItemService.save(item);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spells/update/{spellId}")
    public String updateSpell(
            @PathVariable Long characterId,
            @PathVariable Long spellId,
            @ModelAttribute CharacterSpell updatedSpell,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterSpell> spellOptional = characterSpellService.findById(spellId);

        if (characterOptional.isEmpty() || spellOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();
        CharacterSpell spell = spellOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        if (!spell.getCharacter().getId().equals(character.getId())) {
            return "redirect:/characters/view/" + characterId;
        }

        spell.setName(updatedSpell.getName());
        spell.setDescription(updatedSpell.getDescription());
        spell.setCantrip(updatedSpell.getCantrip());

        if (spell.getCantrip() == null) {
            spell.setCantrip(false);
        }

        if (Boolean.TRUE.equals(spell.getCantrip())) {
            spell.setSpellCircle(0);
        } else {
            if (updatedSpell.getSpellCircle() == null || updatedSpell.getSpellCircle() < 1) {
                redirectAttributes.addFlashAttribute("spellError", "Selecione um circulo valido ou marque como truque.");
                return "redirect:/characters/view/" + characterId;
            }

            if (!hasSpellSlotForCircle(character, updatedSpell.getSpellCircle())) {
                redirectAttributes.addFlashAttribute("spellError", "Cadastre o espaco desse circulo antes de salvar a magia.");
                return "redirect:/characters/view/" + characterId;
            }

            spell.setSpellCircle(updatedSpell.getSpellCircle());
        }

        characterSpellService.save(spell);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/spell-slots/update/{slotId}")
    public String updateSpellSlot(
            @PathVariable Long characterId,
            @PathVariable Long slotId,
            @ModelAttribute SpellSlot updatedSlot,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<SpellSlot> slotOptional = spellSlotService.findById(slotId);

        if (characterOptional.isEmpty() || slotOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();
        SpellSlot slot = slotOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        if (!slot.getCharacter().getId().equals(character.getId())) {
            return "redirect:/characters/view/" + characterId;
        }

        slot.setSpellCircle(updatedSlot.getSpellCircle());
        slot.setTotalSlots(updatedSlot.getTotalSlots());
        slot.setUsedSlots(updatedSlot.getUsedSlots());

        if (slot.getTotalSlots() == null || slot.getTotalSlots() < 0) {
            slot.setTotalSlots(0);
        }

        if (slot.getUsedSlots() == null || slot.getUsedSlots() < 0) {
            slot.setUsedSlots(0);
        }

        if (slot.getUsedSlots() > slot.getTotalSlots()) {
            slot.setUsedSlots(slot.getTotalSlots());
        }

        spellSlotService.save(slot);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/abilities/save")
    public String saveAbility(
            @PathVariable Long characterId,
            @ModelAttribute CharacterAbility ability,
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

        ability.setCharacter(character);
        normalizeAbilityUses(ability);
        characterAbilityService.save(ability);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/abilities/update/{abilityId}")
    public String updateAbility(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @ModelAttribute CharacterAbility updatedAbility,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterAbility> abilityOptional = characterAbilityService.findById(abilityId);

        if (characterOptional.isEmpty() || abilityOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();
        CharacterAbility ability = abilityOptional.get();

        if (!character.getUser().getId().equals(user.getId())) {
            return "redirect:/characters";
        }

        if (!ability.getCharacter().getId().equals(character.getId())) {
            return "redirect:/characters/view/" + characterId;
        }

        ability.setName(updatedAbility.getName());
        ability.setAbilityType(updatedAbility.getAbilityType());
        ability.setUses(updatedAbility.getUses());
        ability.setMaxUses(updatedAbility.getMaxUses());
        ability.setDescription(updatedAbility.getDescription());
        normalizeAbilityUses(ability);

        characterAbilityService.save(ability);

        return "redirect:/characters/view/" + characterId;
    }

    @GetMapping("/{characterId}/abilities/delete/{abilityId}")
    public String deleteAbility(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        characterAbilityService.delete(abilityId);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/abilities/{abilityId}/uses/adjust")
    public String adjustAbilityUses(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @RequestParam Integer delta,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterAbility> abilityOptional = characterAbilityService.findById(abilityId);

        if (characterOptional.isEmpty() || abilityOptional.isEmpty()) {
            return "redirect:/characters";
        }

        RpgCharacter character = characterOptional.get();
        CharacterAbility ability = abilityOptional.get();

        if (!character.getUser().getId().equals(user.getId())
                || !ability.getCharacter().getId().equals(character.getId())) {
            return "redirect:/characters/view/" + characterId;
        }

        int current = ability.getUses() == null ? 0 : ability.getUses();
        int updated = current + (delta == null ? 0 : delta);
        ability.setUses(updated);
        normalizeAbilityUses(ability);

        characterAbilityService.save(ability);

        return "redirect:/characters/view/" + characterId;
    }

    @PostMapping("/{characterId}/abilities/{abilityId}/uses/adjust.json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adjustAbilityUsesJson(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @RequestParam Integer delta,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterAbility> abilityOptional = characterAbilityService.findById(abilityId);

        if (characterOptional.isEmpty() || abilityOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RpgCharacter character = characterOptional.get();
        CharacterAbility ability = abilityOptional.get();

        if (!character.getUser().getId().equals(user.getId())
                || !ability.getCharacter().getId().equals(character.getId())) {
            return ResponseEntity.status(403).build();
        }

        int current = ability.getUses() == null ? 0 : ability.getUses();
        int updated = current + (delta == null ? 0 : delta);
        ability.setUses(updated);
        normalizeAbilityUses(ability);

        characterAbilityService.save(ability);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", ability.getId());
        body.put("uses", ability.getUses() == null ? 0 : ability.getUses());
        body.put("maxUses", ability.getMaxUses() == null ? 0 : ability.getMaxUses());
        return ResponseEntity.ok(body);
    }

    private void normalizeAbilityUses(CharacterAbility ability) {
        if (ability.getMaxUses() != null && ability.getMaxUses() < 0) {
            ability.setMaxUses(0);
        }

        if (ability.getUses() != null && ability.getUses() < 0) {
            ability.setUses(0);
        }

        if (ability.getMaxUses() != null
                && ability.getUses() != null
                && ability.getUses() > ability.getMaxUses()) {
            ability.setUses(ability.getMaxUses());
        }
    }

    private void copyEditableFields(RpgCharacter source, RpgCharacter target) {
        target.setName(source.getName());
        target.setRace(source.getRace());
        target.setCharacterClass(source.getCharacterClass());
        target.setLevel(source.getLevel());
        target.setExperiencePoints(source.getExperiencePoints());
        target.setHitPoints(source.getHitPoints());
        target.setMaxHitPoints(source.getMaxHitPoints());
        target.setTemporaryHitPoints(source.getTemporaryHitPoints());
        target.setArmorClass(source.getArmorClass());
        target.setStrength(source.getStrength());
        target.setDexterity(source.getDexterity());
        target.setConstitution(source.getConstitution());
        target.setIntelligence(source.getIntelligence());
        target.setWisdom(source.getWisdom());
        target.setCharisma(source.getCharisma());
        target.setSpellcastingAbility(source.getSpellcastingAbility());
        target.setDescription(source.getDescription());
    }

    private void handleAvatarUpload(RpgCharacter character, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return;
        }

        String contentType = avatarFile.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            return;
        }

        try {
            character.setAvatarData(avatarFile.getBytes());
            character.setAvatarContentType(contentType);
        } catch (IOException ignored) {
            // Ignore upload read failures and keep current avatar unchanged.
        }
    }

    private boolean hasSpellSlotForCircle(RpgCharacter character, Integer spellCircle) {
        return spellCircle != null
                && spellCircle > 0
                && spellSlotService.findByCharacterAndSpellCircle(character, spellCircle).isPresent();
    }
}