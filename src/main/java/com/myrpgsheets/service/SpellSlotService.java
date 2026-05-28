package com.myrpgsheets.service;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.SpellSlot;
import com.myrpgsheets.repository.SpellSlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpellSlotService {

    private final SpellSlotRepository spellSlotRepository;

    public SpellSlotService(SpellSlotRepository spellSlotRepository) {
        this.spellSlotRepository = spellSlotRepository;
    }

    public List<SpellSlot> findByCharacter(RpgCharacter character) {
        return spellSlotRepository.findByCharacter(character);
    }

    public Optional<SpellSlot> findById(Long id) {
        return spellSlotRepository.findById(id);
    }

    public Optional<SpellSlot> findByCharacterAndSpellCircle(RpgCharacter character, Integer spellCircle) {
        return spellSlotRepository.findByCharacterAndSpellCircle(character, spellCircle);
    }

    public SpellSlot save(SpellSlot spellSlot) {
        return spellSlotRepository.save(spellSlot);
    }

    public void delete(Long id) {
        spellSlotRepository.deleteById(id);
    }
}