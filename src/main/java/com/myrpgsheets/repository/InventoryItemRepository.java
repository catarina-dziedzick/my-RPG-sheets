package com.myrpgsheets.repository;

import com.myrpgsheets.model.InventoryItem;
import com.myrpgsheets.model.RpgCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByCharacter(RpgCharacter character);
}