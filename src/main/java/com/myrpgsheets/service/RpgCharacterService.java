package com.myrpgsheets.service;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import com.myrpgsheets.repository.RpgCharacterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RpgCharacterService {

    private final RpgCharacterRepository characterRepository;

    public RpgCharacterService(RpgCharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
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

    public void delete(Long id) {
        characterRepository.deleteById(id);
    }
}