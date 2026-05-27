package com.myrpgsheets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spell_slots")
@Getter
@Setter
@NoArgsConstructor
public class SpellSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer spellCircle;

    private Integer totalSlots;

    private Integer usedSlots;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;
}