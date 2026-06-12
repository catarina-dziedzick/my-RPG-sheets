package com.myrpgsheets.repository;

import com.myrpgsheets.model.CharacterSpell;
import com.myrpgsheets.model.RpgCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterSpellRepository extends JpaRepository<CharacterSpell, Long> {

    List<CharacterSpell> findByCharacter(RpgCharacter character);

    List<CharacterSpell> findByCharacterAndSpellCircle(RpgCharacter character, Integer spellCircle);

    List<CharacterSpell> findByCharacterAndCantripTrue(RpgCharacter character);

    void deleteByCharacter(RpgCharacter character);
}