package com.myrpgsheets.repository;

import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RpgCharacterRepository extends JpaRepository<RpgCharacter, Long> {

    List<RpgCharacter> findByUser(User user);
}