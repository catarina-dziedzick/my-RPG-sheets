package com.myrpgsheets.service;

import com.myrpgsheets.model.CharacterSpell;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.repository.CharacterSpellRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CharacterSpellService {

    private final CharacterSpellRepository characterSpellRepository;

    public CharacterSpellService(CharacterSpellRepository characterSpellRepository) {
        this.characterSpellRepository = characterSpellRepository;
    }

    public List<CharacterSpell> findByCharacter(RpgCharacter character) {
        return characterSpellRepository.findByCharacter(character);
    }

    public List<CharacterSpell> findCantrips(RpgCharacter character) {
        return characterSpellRepository.findByCharacterAndCantripTrue(character);
    }

    public List<CharacterSpell> findByCircle(RpgCharacter character, Integer circle) {
        return characterSpellRepository.findByCharacterAndSpellCircle(character, circle);
    }

    public Optional<CharacterSpell> findById(Long id) {
        return characterSpellRepository.findById(id);
    }

    public CharacterSpell save(CharacterSpell spell) {
        return characterSpellRepository.save(spell);
    }

    public void delete(Long id) {
        characterSpellRepository.deleteById(id);
    }
}