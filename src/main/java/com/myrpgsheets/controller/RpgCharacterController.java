package com.myrpgsheets.controller;

import com.myrpgsheets.model.*;
import com.myrpgsheets.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/characters")
public class RpgCharacterController {

    private final RpgCharacterService rpgCharacterService;
    private final InventoryItemService inventoryItemService;
    private final CharacterSpellService characterSpellService;
    private final SpellSlotService spellSlotService;
    private final CharacterAbilityService characterAbilityService;
    private final CampaignService campaignService;
    private final CharacterImageService characterImageService;

    public RpgCharacterController(
            RpgCharacterService rpgCharacterService,
            InventoryItemService inventoryItemService,
            CharacterSpellService characterSpellService,
            SpellSlotService spellSlotService,
            CharacterAbilityService characterAbilityService,
            CampaignService campaignService,
            CharacterImageService characterImageService)
    {
        this.rpgCharacterService = rpgCharacterService;
        this.inventoryItemService = inventoryItemService;
        this.characterSpellService = characterSpellService;
        this.spellSlotService = spellSlotService;
        this.characterAbilityService = characterAbilityService;
        this.campaignService = campaignService;
        this.characterImageService = characterImageService;
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
            @RequestParam(required = false) Long campaignId,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            if (campaignId != null) {
                model.addAttribute("campaignId", campaignId);
            }

            return "characters/form";
        }

        RpgCharacter characterToSave = rpgCharacter;

        if (rpgCharacter.getId() != null) {
            Optional<RpgCharacter> existingCharacterOptional = rpgCharacterService.findById(rpgCharacter.getId());

            if (existingCharacterOptional.isEmpty()) {
                if (campaignId != null) {
                    return "redirect:/campaigns/view/" + campaignId;
                }

                return "redirect:/characters";
            }

            RpgCharacter existingCharacter = existingCharacterOptional.get();

            boolean isOwner = existingCharacter.getUser() != null
                    && existingCharacter.getUser().getId().equals(user.getId());

            boolean canEditByCampaign = false;

            if (campaignId != null) {
                Optional<Campaign> campaignOptional = campaignService.findById(campaignId);

                if (campaignOptional.isPresent()) {
                    Campaign campaign = campaignOptional.get();

                    canEditByCampaign = campaignService.canEditCharacterInCampaign(
                            campaign,
                            existingCharacter,
                            user
                    );
                }
            }

            if (!isOwner && !canEditByCampaign) {
                if (campaignId != null) {
                    return "redirect:/campaigns/view/" + campaignId;
                }

                return "redirect:/characters";
            }

            copyEditableFields(rpgCharacter, existingCharacter);
            characterToSave = existingCharacter;
        } else {
            characterToSave.setUser(user);
        }

        handleAvatarUpload(characterToSave, avatarFile);

        Integer maxHp = characterToSave.getMaxHitPoints();
        Integer curHp = characterToSave.getHitPoints();

        if (maxHp != null && maxHp > 0 && curHp != null && curHp > maxHp) {
            characterToSave.setHitPoints(maxHp);
        }

        clampSaveProficiencies(characterToSave);

        rpgCharacterService.save(characterToSave);

        if (campaignId != null) {
            return "redirect:/campaigns/view/" + campaignId;
        }

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

        if (!character.hasAvatar()) {
            return ResponseEntity.notFound().build();
        }

        boolean isOwner = character.getUser() != null
                && character.getUser().getId().equals(user.getId());

        boolean canViewThroughCampaign = campaignService.canViewCharacterThroughCampaign(character, user);

        if (!isOwner && !canViewThroughCampaign) {
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

        addCharacterViewAttributes(model, character);
        model.addAttribute("canEditCharacter", true);

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
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        int currentHitPoints = character.getHitPoints() == null ? 0 : character.getHitPoints();
        int updatedHitPoints = Math.max(0, currentHitPoints + delta);
        updatedHitPoints = clampHp(character, updatedHitPoints);

        character.setHitPoints(updatedHitPoints);
        rpgCharacterService.save(character);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/hit-points/set")
    public String setHitPoints(
            @PathVariable Long characterId,
            @RequestParam Integer hitPoints,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        int normalizedHitPoints = hitPoints == null ? 0 : Math.max(0, hitPoints);
        normalizedHitPoints = clampHp(character, normalizedHitPoints);

        character.setHitPoints(normalizedHitPoints);
        rpgCharacterService.save(character);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/xp/adjust")
    public String adjustXp(
            @PathVariable Long characterId,
            @RequestParam Integer delta,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        int current = character.getExperiencePoints() == null ? 0 : character.getExperiencePoints();
        int updated = current + (delta == null ? 0 : delta);
        character.setExperiencePoints(clampXp(character, updated));
        rpgCharacterService.save(character);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/xp/set")
    public String setXp(
            @PathVariable Long characterId,
            @RequestParam Integer experiencePoints,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        int value = experiencePoints == null ? 0 : experiencePoints;
        character.setExperiencePoints(clampXp(character, value));
        rpgCharacterService.save(character);

        return redirectToCharacterView(characterId, campaignId);
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
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";

        Optional<RpgCharacter> opt = rpgCharacterService.findById(characterId);
        if (opt.isEmpty()) return redirectToSafePlace(campaignId);

        RpgCharacter character = opt.get();
        if (!canModifyCharacter(character, user, campaignId)) return redirectToSafePlace(campaignId);

        int val = temporaryHitPoints == null ? 0 : Math.max(0, temporaryHitPoints);
        character.setTemporaryHitPoints(val);
        rpgCharacterService.save(character);
        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spell-slots/use")
    public String useSpellSlot(
            @PathVariable Long characterId,
            @RequestParam Integer spellCircle,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
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

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spell-slots/toggle")
    public String toggleSpellSlot(
            @PathVariable Long characterId,
            @RequestParam Integer spellCircle,
            @RequestParam Integer slotIndex,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        Optional<SpellSlot> slotOptional = spellSlotService.findByCharacterAndSpellCircle(character, spellCircle);

        if (slotOptional.isEmpty()) {
            return redirectToCharacterView(characterId, campaignId);
        }

        SpellSlot slot = slotOptional.get();
        int total = slot.getTotalSlots() == null ? 0 : slot.getTotalSlots();
        int used = slot.getUsedSlots() == null ? 0 : slot.getUsedSlots();

        if (slotIndex == null || slotIndex < 1 || slotIndex > total) {
            return redirectToCharacterView(characterId, campaignId);
        }

        if (slotIndex <= used) {
            slot.setUsedSlots(slotIndex - 1);
        } else {
            slot.setUsedSlots(slotIndex);
        }

        spellSlotService.save(slot);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/items/save")
    public String saveItem(
            @PathVariable Long characterId,
            @ModelAttribute InventoryItem item,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        item.setCharacter(character);

        if (item.getQuantity() == null) {
            item.setQuantity(1);
        }

        inventoryItemService.save(item);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spells/save")
    public String saveSpell(
            @PathVariable Long characterId,
            @ModelAttribute CharacterSpell spell,
            @RequestParam(required = false) Long campaignId,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        spell.setCharacter(character);

        if (spell.getCantrip() == null) {
            spell.setCantrip(false);
        }

        if (Boolean.TRUE.equals(spell.getCantrip())) {
            spell.setSpellCircle(0);
        } else {
            if (spell.getSpellCircle() == null || spell.getSpellCircle() < 1) {
                redirectAttributes.addFlashAttribute("spellError", "Selecione um círculo válido ou marque como truque.");
                return redirectToCharacterView(characterId, campaignId);
            }

            if (!hasSpellSlotForCircle(character, spell.getSpellCircle())) {
                redirectAttributes.addFlashAttribute("spellError", "Cadastre o espaço desse círculo antes de adicionar a magia.");
                return redirectToCharacterView(characterId, campaignId);
            }
        }

        characterSpellService.save(spell);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spell-slots/save")
    public String saveSpellSlot(
            @PathVariable Long characterId,
            @ModelAttribute SpellSlot spellSlot,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        spellSlot.setCharacter(character);

        if (spellSlot.getUsedSlots() == null) {
            spellSlot.setUsedSlots(0);
        }

        if (spellSlot.getTotalSlots() == null) {
            spellSlot.setTotalSlots(0);
        }

        spellSlotService.save(spellSlot);

        return redirectToCharacterView(characterId, campaignId);
    }

    @GetMapping("/{characterId}/items/delete/{itemId}")
    public String deleteItem(
            @PathVariable Long characterId,
            @PathVariable Long itemId,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        if (characterOptional.isEmpty() || !canModifyCharacter(characterOptional.get(), user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        inventoryItemService.delete(itemId);

        return redirectToCharacterView(characterId, campaignId);
    }

    @GetMapping("/{characterId}/spells/delete/{spellId}")
    public String deleteSpell(
            @PathVariable Long characterId,
            @PathVariable Long spellId,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        if (characterOptional.isEmpty() || !canModifyCharacter(characterOptional.get(), user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        characterSpellService.delete(spellId);

        return redirectToCharacterView(characterId, campaignId);
    }

    @GetMapping("/{characterId}/spell-slots/delete/{slotId}")
    public String deleteSpellSlot(
            @PathVariable Long characterId,
            @PathVariable Long slotId,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        if (characterOptional.isEmpty() || !canModifyCharacter(characterOptional.get(), user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        spellSlotService.delete(slotId);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/items/update/{itemId}")
    public String updateItem(
            @PathVariable Long characterId,
            @PathVariable Long itemId,
            @ModelAttribute InventoryItem updatedItem,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<InventoryItem> itemOptional = inventoryItemService.findById(itemId);

        if (characterOptional.isEmpty() || itemOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        InventoryItem item = itemOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (!item.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
        }

        item.setName(updatedItem.getName());
        item.setQuantity(updatedItem.getQuantity());
        item.setDescription(updatedItem.getDescription());

        if (item.getQuantity() == null || item.getQuantity() < 1) {
            item.setQuantity(1);
        }

        inventoryItemService.save(item);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spells/update/{spellId}")
    public String updateSpell(
            @PathVariable Long characterId,
            @PathVariable Long spellId,
            @ModelAttribute CharacterSpell updatedSpell,
            @RequestParam(required = false) Long campaignId,
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
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        CharacterSpell spell = spellOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (!spell.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
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
                return redirectToCharacterView(characterId, campaignId);
            }

            if (!hasSpellSlotForCircle(character, updatedSpell.getSpellCircle())) {
                redirectAttributes.addFlashAttribute("spellError", "Cadastre o espaco desse circulo antes de salvar a magia.");
                return redirectToCharacterView(characterId, campaignId);
            }

            spell.setSpellCircle(updatedSpell.getSpellCircle());
        }

        characterSpellService.save(spell);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/spell-slots/update/{slotId}")
    public String updateSpellSlot(
            @PathVariable Long characterId,
            @PathVariable Long slotId,
            @ModelAttribute SpellSlot updatedSlot,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<SpellSlot> slotOptional = spellSlotService.findById(slotId);

        if (characterOptional.isEmpty() || slotOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        SpellSlot slot = slotOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (!slot.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
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

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/abilities/save")
    public String saveAbility(
            @PathVariable Long characterId,
            @ModelAttribute CharacterAbility ability,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        ability.setCharacter(character);
        normalizeAbilityUses(ability);
        characterAbilityService.save(ability);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/abilities/update/{abilityId}")
    public String updateAbility(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @ModelAttribute CharacterAbility updatedAbility,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterAbility> abilityOptional = characterAbilityService.findById(abilityId);

        if (characterOptional.isEmpty() || abilityOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        CharacterAbility ability = abilityOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (!ability.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
        }

        ability.setName(updatedAbility.getName());
        ability.setAbilityType(updatedAbility.getAbilityType());
        ability.setUses(updatedAbility.getUses());
        ability.setMaxUses(updatedAbility.getMaxUses());
        ability.setDescription(updatedAbility.getDescription());
        normalizeAbilityUses(ability);

        characterAbilityService.save(ability);

        return redirectToCharacterView(characterId, campaignId);
    }

    @GetMapping("/{characterId}/abilities/delete/{abilityId}")
    public String deleteAbility(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        if (characterOptional.isEmpty() || !canModifyCharacter(characterOptional.get(), user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        characterAbilityService.delete(abilityId);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/abilities/{abilityId}/uses/adjust")
    public String adjustAbilityUses(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @RequestParam Integer delta,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterAbility> abilityOptional = characterAbilityService.findById(abilityId);

        if (characterOptional.isEmpty() || abilityOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        CharacterAbility ability = abilityOptional.get();

        if (!canModifyCharacter(character, user, campaignId)
                || !ability.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
        }

        int current = ability.getUses() == null ? 0 : ability.getUses();
        int updated = current + (delta == null ? 0 : delta);
        ability.setUses(updated);
        normalizeAbilityUses(ability);

        characterAbilityService.save(ability);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/abilities/{abilityId}/uses/adjust.json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> adjustAbilityUsesJson(
            @PathVariable Long characterId,
            @PathVariable Long abilityId,
            @RequestParam Integer delta,
            @RequestParam(required = false) Long campaignId,
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

        if (!canModifyCharacter(character, user, campaignId)
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

    @GetMapping("/campaign/{campaignId}/edit/{characterId}")
    public String editCharacterFromCampaign(
            @PathVariable Long campaignId,
            @PathVariable Long characterId,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<Campaign> campaignOptional = campaignService.findById(campaignId);
        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (campaignOptional.isEmpty() || characterOptional.isEmpty()) {
            return "redirect:/campaigns";
        }

        Campaign campaign = campaignOptional.get();
        RpgCharacter character = characterOptional.get();

        if (!campaignService.canEditCharacterInCampaign(campaign, character, user)) {
            return "redirect:/campaigns/view/" + campaignId;
        }

        model.addAttribute("character", character);
        model.addAttribute("campaignId", campaignId);

        return "characters/form";
    }

    @GetMapping("/campaign/{campaignId}/view/{characterId}")
    public String viewCharacterFromCampaign(
            @PathVariable Long campaignId,
            @PathVariable Long characterId,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<Campaign> campaignOptional = campaignService.findById(campaignId);
        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (campaignOptional.isEmpty() || characterOptional.isEmpty()) {
            return "redirect:/campaigns";
        }

        Campaign campaign = campaignOptional.get();
        RpgCharacter character = characterOptional.get();

        boolean isMember = campaignService.isMember(campaign, user);
        boolean canView = campaignService.canViewCharacterInCampaign(campaign, character, user);
        boolean canEdit = campaignService.canEditCharacterInCampaign(campaign, character, user);

        if (!isMember || !canView) {
            return "redirect:/campaigns/view/" + campaignId;
        }

        addCharacterViewAttributes(model, character);
        model.addAttribute("campaignId", campaignId);
        model.addAttribute("canEditCharacter", canEdit);

        return "characters/view";
    }

    @PostMapping("/{characterId}/lore/save")
    public String saveLore(
            @PathVariable Long characterId,
            @RequestParam(required = false) String backstory,
            @RequestParam(required = false) String appearance,
            @RequestParam(required = false) String personalityTraits,
            @RequestParam(required = false) String ideals,
            @RequestParam(required = false) String bonds,
            @RequestParam(required = false) String flaws,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        character.setBackstory(backstory);
        character.setAppearance(appearance);
        character.setPersonalityTraits(personalityTraits);
        character.setIdeals(ideals);
        character.setBonds(bonds);
        character.setFlaws(flaws);
        character.setNotes(notes);

        rpgCharacterService.save(character);

        return redirectToCharacterView(characterId, campaignId);
    }

    @PostMapping("/{characterId}/images/save")
    public String saveCharacterImages(
            @PathVariable Long characterId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);

        if (characterOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (imageFiles != null) {
            for (MultipartFile imageFile : imageFiles) {
                if (imageFile == null || imageFile.isEmpty()) {
                    continue;
                }

                String contentType = imageFile.getContentType();

                if (contentType == null || !contentType.startsWith("image/")) {
                    continue;
                }

                try {
                    CharacterImage image = new CharacterImage();
                    image.setCharacter(character);
                    image.setCaption(caption);
                    image.setContentType(contentType);
                    image.setUploadedAt(java.time.LocalDateTime.now());
                    image.setImageData(imageFile.getBytes());

                    characterImageService.save(image);
                } catch (IOException ignored) {
                    // Ignora arquivo com falha de leitura
                }
            }
        }

        return redirectToCharacterView(characterId, campaignId);
    }

    @GetMapping("/images/{imageId}")
    @ResponseBody
    public ResponseEntity<byte[]> characterImage(
            @PathVariable Long imageId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<CharacterImage> imageOptional = characterImageService.findById(imageId);

        if (imageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CharacterImage image = imageOptional.get();
        RpgCharacter character = image.getCharacter();

        boolean isOwner = character.getUser() != null
                && character.getUser().getId().equals(user.getId());

        boolean canViewThroughCampaign = campaignService.canViewCharacterThroughCampaign(character, user);

        if (!isOwner && !canViewThroughCampaign) {
            return ResponseEntity.notFound().build();
        }

        if (!image.hasImage()) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

        if (image.getContentType() != null && !image.getContentType().isBlank()) {
            try {
                contentType = MediaType.parseMediaType(image.getContentType());
            } catch (IllegalArgumentException ignored) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(image.getImageData());
    }

    private void addCharacterViewAttributes(Model model, RpgCharacter character) {
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
        model.addAttribute("newItem", new InventoryItem());

        model.addAttribute("spells", spells);
        model.addAttribute("newSpell", new CharacterSpell());

        model.addAttribute("spellSlots", spellSlots);
        model.addAttribute("spellSlotsByCircle", spellSlotsByCircle);
        model.addAttribute("availableSpellCircles", availableSpellCircles);
        model.addAttribute("availableSpellSlots", availableSpellSlots);
        model.addAttribute("newSpellSlot", new SpellSlot());

        model.addAttribute("abilities", characterAbilityService.findByCharacter(character));
        model.addAttribute("newAbility", new CharacterAbility());

        model.addAttribute("characterImages", characterImageService.findByCharacter(character));
    }

    @GetMapping("/{characterId}/images/delete/{imageId}")
    public String deleteCharacterImage(
            @PathVariable Long characterId,
            @PathVariable Long imageId,
            @RequestParam(required = false) Long campaignId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<RpgCharacter> characterOptional = rpgCharacterService.findById(characterId);
        Optional<CharacterImage> imageOptional = characterImageService.findById(imageId);

        if (characterOptional.isEmpty() || imageOptional.isEmpty()) {
            return redirectToSafePlace(campaignId);
        }

        RpgCharacter character = characterOptional.get();
        CharacterImage image = imageOptional.get();

        if (!canModifyCharacter(character, user, campaignId)) {
            return redirectToSafePlace(campaignId);
        }

        if (!image.getCharacter().getId().equals(character.getId())) {
            return redirectToCharacterView(characterId, campaignId);
        }

        characterImageService.delete(imageId);

        return redirectToCharacterView(characterId, campaignId);
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
        target.setBackground(source.getBackground());
        target.setAlignment(source.getAlignment());
        target.setDescription(source.getDescription());

        // Proficiências em Testes de Resistência
        target.setSaveProfStrength(source.getSaveProfStrength());
        target.setSaveProfDexterity(source.getSaveProfDexterity());
        target.setSaveProfConstitution(source.getSaveProfConstitution());
        target.setSaveProfIntelligence(source.getSaveProfIntelligence());
        target.setSaveProfWisdom(source.getSaveProfWisdom());
        target.setSaveProfCharisma(source.getSaveProfCharisma());

        // Proficiências em perícias
        target.setProfAcrobatics(source.getProfAcrobatics());
        target.setProfAnimalHandling(source.getProfAnimalHandling());
        target.setProfArcana(source.getProfArcana());
        target.setProfAthletics(source.getProfAthletics());
        target.setProfPerformance(source.getProfPerformance());
        target.setProfDeception(source.getProfDeception());
        target.setProfStealth(source.getProfStealth());
        target.setProfHistory(source.getProfHistory());
        target.setProfIntimidation(source.getProfIntimidation());
        target.setProfInsight(source.getProfInsight());
        target.setProfInvestigation(source.getProfInvestigation());
        target.setProfMedicine(source.getProfMedicine());
        target.setProfNature(source.getProfNature());
        target.setProfPerception(source.getProfPerception());
        target.setProfPersuasion(source.getProfPersuasion());
        target.setProfSleightOfHand(source.getProfSleightOfHand());
        target.setProfReligion(source.getProfReligion());
        target.setProfSurvival(source.getProfSurvival());

        target.setBackstory(source.getBackstory());
        target.setAppearance(source.getAppearance());
        target.setPersonalityTraits(source.getPersonalityTraits());
        target.setIdeals(source.getIdeals());
        target.setBonds(source.getBonds());
        target.setFlaws(source.getFlaws());
        target.setNotes(source.getNotes());
    }

    /** Se mais de 2 saves estiverem marcados, desmarca os excedentes (ordem: CAR, SAB, INT, CON, DES, FOR). */
    private void clampSaveProficiencies(RpgCharacter c) {
        boolean[] saves = {
            Boolean.TRUE.equals(c.getSaveProfStrength()),
            Boolean.TRUE.equals(c.getSaveProfDexterity()),
            Boolean.TRUE.equals(c.getSaveProfConstitution()),
            Boolean.TRUE.equals(c.getSaveProfIntelligence()),
            Boolean.TRUE.equals(c.getSaveProfWisdom()),
            Boolean.TRUE.equals(c.getSaveProfCharisma())
        };
        int count = 0;
        for (boolean b : saves) if (b) count++;
        if (count <= 2) return;
        // desmarca do final até restar 2
        if (saves[5] && count > 2) { c.setSaveProfCharisma(false);   count--; }
        if (saves[4] && count > 2) { c.setSaveProfWisdom(false);      count--; }
        if (saves[3] && count > 2) { c.setSaveProfIntelligence(false); count--; }
        if (saves[2] && count > 2) { c.setSaveProfConstitution(false); count--; }
        if (saves[1] && count > 2) { c.setSaveProfDexterity(false);   count--; }
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

    private boolean canModifyCharacter(RpgCharacter character, User user, Long campaignId) {
        boolean isOwner = character.getUser() != null
                && character.getUser().getId().equals(user.getId());

        if (isOwner) {
            return true;
        }

        if (campaignId == null) {
            return false;
        }

        Optional<Campaign> campaignOptional = campaignService.findById(campaignId);

        if (campaignOptional.isEmpty()) {
            return false;
        }

        Campaign campaign = campaignOptional.get();

        return campaignService.canEditCharacterInCampaign(campaign, character, user);
    }

    private String redirectToCharacterView(Long characterId, Long campaignId) {
        if (campaignId != null) {
            return "redirect:/characters/campaign/" + campaignId + "/view/" + characterId;
        }

        return "redirect:/characters/view/" + characterId;
    }

    private String redirectToSafePlace(Long campaignId) {
        if (campaignId != null) {
            return "redirect:/campaigns/view/" + campaignId;
        }

        return "redirect:/characters";
    }
}