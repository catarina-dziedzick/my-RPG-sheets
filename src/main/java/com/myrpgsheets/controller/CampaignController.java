package com.myrpgsheets.controller;

import com.myrpgsheets.model.Campaign;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import com.myrpgsheets.service.CampaignService;
import com.myrpgsheets.service.RpgCharacterService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final RpgCharacterService characterService;

    public CampaignController(
            CampaignService campaignService,
            RpgCharacterService characterService
    ) {
        this.campaignService = campaignService;
        this.characterService = characterService;
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("campaignsAsMaster", campaignService.findCampaignsAsMaster(user));
        model.addAttribute("campaignMemberships", campaignService.findCampaignsAsMember(user));

        return "campaigns/index";
    }

    @GetMapping("/new")
    public String newCampaign(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("campaign", new Campaign());

        return "campaigns/form";
    }

    @PostMapping("/save")
    public String saveCampaign(
            @ModelAttribute Campaign campaign,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Campaign savedCampaign = campaignService.createCampaign(campaign, user);

        return "redirect:/campaigns/view/" + savedCampaign.getId();
    }

    @GetMapping("/join")
    public String joinPage(HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        return "campaigns/join";
    }

    @PostMapping("/join")
    public String joinCampaign(
            @RequestParam String inviteCode,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        try {
            campaignService.joinCampaign(inviteCode.trim().toUpperCase(), user);
            return "redirect:/campaigns";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "campaigns/join";
        }
    }

    @GetMapping("/view/{id}")
    public String viewCampaign(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<Campaign> campaignOptional = campaignService.findById(id);

        if (campaignOptional.isEmpty()) {
            return "redirect:/campaigns";
        }

        Campaign campaign = campaignOptional.get();

        if (!campaignService.isMember(campaign, user)) {
            return "redirect:/campaigns";
        }

        boolean isMaster = campaignService.isMaster(campaign, user);

        model.addAttribute("campaign", campaign);
        model.addAttribute("isMaster", isMaster);
        model.addAttribute("members", campaignService.findMembers(campaign));
        model.addAttribute("campaignCharacters", campaignService.findVisibleCharacters(campaign, user));
        model.addAttribute("userCharacters", characterService.findByUser(user));

        return "campaigns/view";
    }

    @PostMapping("/{campaignId}/characters/add")
    public String addCharacterToCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long characterId,
            HttpSession session,
            Model model
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<Campaign> campaignOptional = campaignService.findById(campaignId);
        Optional<RpgCharacter> characterOptional = characterService.findById(characterId);

        if (campaignOptional.isEmpty() || characterOptional.isEmpty()) {
            return "redirect:/campaigns";
        }

        Campaign campaign = campaignOptional.get();
        RpgCharacter character = characterOptional.get();

        if (!campaignService.isMember(campaign, user)) {
            return "redirect:/campaigns";
        }

        if (character.getUser() == null || !character.getUser().getId().equals(user.getId())) {
            return "redirect:/campaigns/view/" + campaignId;
        }

        try {
            campaignService.addCharacterToCampaign(campaign, character, user);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "redirect:/campaigns/view/" + campaignId;
    }

    @GetMapping("/{campaignId}/characters/remove/{campaignCharacterId}")
    public String removeCharacterFromCampaign(
            @PathVariable Long campaignId,
            @PathVariable Long campaignCharacterId,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        Optional<Campaign> campaignOptional = campaignService.findById(campaignId);

        if (campaignOptional.isEmpty()) {
            return "redirect:/campaigns";
        }

        Campaign campaign = campaignOptional.get();

        if (!campaignService.isMaster(campaign, user)) {
            return "redirect:/campaigns/view/" + campaignId;
        }

        campaignService.removeCharacterFromCampaign(campaignCharacterId);

        return "redirect:/campaigns/view/" + campaignId;
    }
}