package com.myrpgsheets.service;

import com.myrpgsheets.model.*;
import com.myrpgsheets.repository.CampaignCharacterRepository;
import com.myrpgsheets.repository.CampaignMemberRepository;
import com.myrpgsheets.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignCharacterRepository campaignCharacterRepository;

    public CampaignService(
            CampaignRepository campaignRepository,
            CampaignMemberRepository campaignMemberRepository,
            CampaignCharacterRepository campaignCharacterRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignMemberRepository = campaignMemberRepository;
        this.campaignCharacterRepository = campaignCharacterRepository;
    }

    public Campaign createCampaign(Campaign campaign, User dungeonMaster) {
        campaign.setDungeonMaster(dungeonMaster);
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setInviteCode(generateUniqueInviteCode());

        Campaign savedCampaign = campaignRepository.save(campaign);

        CampaignMember masterMember = new CampaignMember();
        masterMember.setCampaign(savedCampaign);
        masterMember.setUser(dungeonMaster);
        masterMember.setRole("MASTER");
        masterMember.setJoinedAt(LocalDateTime.now());

        campaignMemberRepository.save(masterMember);

        return savedCampaign;
    }

    public Optional<Campaign> findById(Long id) {
        return campaignRepository.findById(id);
    }

    public List<Campaign> findCampaignsAsMaster(User user) {
        return campaignRepository.findByDungeonMaster(user);
    }

    public List<CampaignMember> findCampaignsAsMember(User user) {
        return campaignMemberRepository.findByUser(user);
    }

    public List<CampaignMember> findMembers(Campaign campaign) {
        return campaignMemberRepository.findByCampaign(campaign);
    }

    public boolean isMember(Campaign campaign, User user) {
        return campaignMemberRepository.existsByCampaignAndUser(campaign, user);
    }

    public boolean isMaster(Campaign campaign, User user) {
        return campaign.getDungeonMaster() != null
                && campaign.getDungeonMaster().getId().equals(user.getId());
    }

    public CampaignMember joinCampaign(String inviteCode, User user) {
        Campaign campaign = campaignRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Campanha não encontrada."));

        if (campaignMemberRepository.existsByCampaignAndUser(campaign, user)) {
            throw new RuntimeException("Você já participa desta campanha.");
        }

        CampaignMember member = new CampaignMember();
        member.setCampaign(campaign);
        member.setUser(user);
        member.setRole("PLAYER");
        member.setJoinedAt(LocalDateTime.now());

        return campaignMemberRepository.save(member);
    }

    public CampaignCharacter addCharacterToCampaign(
            Campaign campaign,
            RpgCharacter character,
            User player
    ) {
        if (campaignCharacterRepository.existsByCampaignAndCharacter(campaign, character)) {
            throw new RuntimeException("Esta ficha já foi adicionada à campanha.");
        }

        CampaignCharacter campaignCharacter = new CampaignCharacter();
        campaignCharacter.setCampaign(campaign);
        campaignCharacter.setCharacter(character);
        campaignCharacter.setPlayer(player);
        campaignCharacter.setActive(true);

        return campaignCharacterRepository.save(campaignCharacter);
    }

    public List<CampaignCharacter> findVisibleCharacters(Campaign campaign, User user) {
        if (isMaster(campaign, user)) {
            return campaignCharacterRepository.findByCampaignAndActiveTrue(campaign);
        }

        return campaignCharacterRepository.findByCampaignAndPlayerAndActiveTrue(campaign, user);
    }

    public List<CampaignCharacter> findAllCampaignCharacters(Campaign campaign) {
        return campaignCharacterRepository.findByCampaignAndActiveTrue(campaign);
    }

    public boolean canEditCharacterInCampaign(Campaign campaign, RpgCharacter character, User user) {
        if (isMaster(campaign, user)) {
            return campaignCharacterRepository.findByCampaignAndCharacter(campaign, character).isPresent();
        }

        return character.getUser() != null && character.getUser().getId().equals(user.getId());
    }

    public void removeCharacterFromCampaign(Long campaignCharacterId) {
        campaignCharacterRepository.findById(campaignCharacterId).ifPresent(campaignCharacter -> {
            campaignCharacter.setActive(false);
            campaignCharacterRepository.save(campaignCharacter);
        });
    }

    private String generateUniqueInviteCode() {
        String code;

        do {
            code = generateInviteCode();
        } while (campaignRepository.existsByInviteCode(code));

        return code;
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    public boolean canViewCharacterThroughCampaign(RpgCharacter character, User user) {
        List<CampaignCharacter> campaignCharacters = campaignCharacterRepository
                .findByCharacterAndActiveTrue(character);

        for (CampaignCharacter campaignCharacter : campaignCharacters) {
            Campaign campaign = campaignCharacter.getCampaign();

            if (isMaster(campaign, user)) {
                return true;
            }
        }

        return false;
    }

    public boolean canViewCharacterInCampaign(Campaign campaign, RpgCharacter character, User user) {
        Optional<CampaignCharacter> campaignCharacterOptional =
                campaignCharacterRepository.findByCampaignAndCharacter(campaign, character);

        if (campaignCharacterOptional.isEmpty()) {
            return false;
        }

        if (isMaster(campaign, user)) {
            return true;
        }

        return character.getUser() != null
                && character.getUser().getId().equals(user.getId());
    }
}