package com.myrpgsheets.service;

import com.myrpgsheets.model.CharacterImage;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.repository.CharacterImageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CharacterImageService {

    private final CharacterImageRepository characterImageRepository;

    public CharacterImageService(CharacterImageRepository characterImageRepository) {
        this.characterImageRepository = characterImageRepository;
    }

    public List<CharacterImage> findByCharacter(RpgCharacter character) {
        return characterImageRepository.findByCharacterOrderByUploadedAtDesc(character);
    }

    public Optional<CharacterImage> findById(Long id) {
        return characterImageRepository.findById(id);
    }

    public CharacterImage save(CharacterImage image) {
        return characterImageRepository.save(image);
    }

    public void delete(Long id) {
        characterImageRepository.deleteById(id);
    }
}