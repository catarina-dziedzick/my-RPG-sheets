package com.myrpgsheets.service;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import com.myrpgsheets.repository.RpgCharacterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RpgCharacterService {

    private final RpgCharacterRepository rpgCharacterRepository;

    public RpgCharacterService(RpgCharacterRepository rpgCharacterRepository) {
        this.rpgCharacterRepository = rpgCharacterRepository;
    }

    public List<RpgCharacter> findByUser(User user) {
        return rpgCharacterRepository.findByUser(user);
    }

    public RpgCharacter save(RpgCharacter rpgCharacter) {
        return rpgCharacterRepository.save(rpgCharacter);
    }

    public Optional<RpgCharacter> findById(Long id) {
        return rpgCharacterRepository.findById(id);
    }

    public void delete(Long id) {
        rpgCharacterRepository.deleteById(id);
    }
}