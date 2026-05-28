package com.myrpgsheets.service;

import com.myrpgsheets.model.CharacterAbility;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.repository.CharacterAbilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CharacterAbilityService {

    private final CharacterAbilityRepository characterAbilityRepository;

    public CharacterAbilityService(CharacterAbilityRepository characterAbilityRepository) {
        this.characterAbilityRepository = characterAbilityRepository;
    }

    public List<CharacterAbility> findByCharacter(RpgCharacter character) {
        return characterAbilityRepository.findByCharacter(character);
    }

    public Optional<CharacterAbility> findById(Long id) {
        return characterAbilityRepository.findById(id);
    }

    public CharacterAbility save(CharacterAbility ability) {
        return characterAbilityRepository.save(ability);
    }

    public void delete(Long id) {
        characterAbilityRepository.deleteById(id);
    }
}