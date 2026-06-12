package com.myrpgsheets.service;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import com.myrpgsheets.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RpgCharacterService {

    private final RpgCharacterRepository characterRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CharacterSpellRepository characterSpellRepository;
    private final SpellSlotRepository spellSlotRepository;
    private final CharacterAbilityRepository characterAbilityRepository;
    private final CharacterImageRepository characterImageRepository;
    private final CampaignCharacterRepository campaignCharacterRepository;

    public RpgCharacterService(
            RpgCharacterRepository characterRepository,
            InventoryItemRepository inventoryItemRepository,
            CharacterSpellRepository characterSpellRepository,
            SpellSlotRepository spellSlotRepository,
            CharacterAbilityRepository characterAbilityRepository,
            CharacterImageRepository characterImageRepository,
            CampaignCharacterRepository campaignCharacterRepository
    ) {
        this.characterRepository = characterRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.characterSpellRepository = characterSpellRepository;
        this.spellSlotRepository = spellSlotRepository;
        this.characterAbilityRepository = characterAbilityRepository;
        this.characterImageRepository = characterImageRepository;
        this.campaignCharacterRepository = campaignCharacterRepository;
    }

    public List<RpgCharacter> findByUser(User user) {
        return characterRepository.findByUser(user);
    }

    public RpgCharacter save(RpgCharacter character) {
        return characterRepository.save(character);
    }

    public Optional<RpgCharacter> findById(Long id) {
        return characterRepository.findById(id);
    }

    @Transactional
    public void deleteCompletely(RpgCharacter character) {
        inventoryItemRepository.deleteByCharacter(character);
        characterSpellRepository.deleteByCharacter(character);
        spellSlotRepository.deleteByCharacter(character);
        characterAbilityRepository.deleteByCharacter(character);
        characterImageRepository.deleteByCharacter(character);
        campaignCharacterRepository.deleteByCharacter(character);

        characterRepository.delete(character);
    }
}