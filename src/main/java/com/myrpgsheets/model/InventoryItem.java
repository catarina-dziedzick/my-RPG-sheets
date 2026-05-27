package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required.")
    private String name;

    private Integer quantity;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;
}