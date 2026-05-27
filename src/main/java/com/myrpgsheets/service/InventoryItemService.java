package com.myrpgsheets.service;

import com.myrpgsheets.model.InventoryItem;
import com.myrpgsheets.model.RpgCharacter;
import com.myrpgsheets.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public List<InventoryItem> findByCharacter(RpgCharacter character) {
        return inventoryItemRepository.findByCharacter(character);
    }

    public InventoryItem save(InventoryItem item) {
        return inventoryItemRepository.save(item);
    }

    public void delete(Long id) {
        inventoryItemRepository.deleteById(id);
    }
}