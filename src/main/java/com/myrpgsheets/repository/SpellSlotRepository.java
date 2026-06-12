package com.myrpgsheets.repository;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.SpellSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpellSlotRepository extends JpaRepository<SpellSlot, Long> {

    List<SpellSlot> findByCharacter(RpgCharacter character);

    Optional<SpellSlot> findByCharacterAndSpellCircle(RpgCharacter character, Integer spellCircle);

    void deleteByCharacter(RpgCharacter character);
}