package com.myrpgsheets.repository;

import com.myrpgsheets.model.CharacterImage;
import com.myrpgsheets.model.RpgCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterImageRepository extends JpaRepository<CharacterImage, Long> {

    List<CharacterImage> findByCharacterOrderByUploadedAtDesc(RpgCharacter character);
}