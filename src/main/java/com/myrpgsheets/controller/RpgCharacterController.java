package com.myrpgsheets.controller;

import com.myrpgsheets.model.*;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

        model.addAttribute("character", character);
        model.addAttribute("items", inventoryItemService.findByCharacter(character));
        model.addAttribute("spellSlots", spellSlotService.findByCharacter(character));

        model.addAttribute("spells", spells);
        model.addAttribute("circles", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));

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

        character.setHitPoints(updatedHitPoints);
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

    private void copyEditableFields(RpgCharacter source, RpgCharacter target) {
        target.setName(source.getName());
        target.setRace(source.getRace());
        target.setCharacterClass(source.getCharacterClass());
        target.setLevel(source.getLevel());
        target.setHitPoints(source.getHitPoints());
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
}