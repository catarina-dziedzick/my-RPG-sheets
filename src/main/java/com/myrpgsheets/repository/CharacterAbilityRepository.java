package com.myrpgsheets.repository;

import com.myrpgsheets.model.CharacterAbility;
import com.myrpgsheets.model.RpgCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterAbilityRepository extends JpaRepository<CharacterAbility, Long> {

    List<CharacterAbility> findByCharacter(RpgCharacter character);

    void deleteByCharacter(RpgCharacter character);
}

