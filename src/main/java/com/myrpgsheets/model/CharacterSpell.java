package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_spells")
@Getter
@Setter
@NoArgsConstructor
public class CharacterSpell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Spell name is required.")
    private String name;

    private Integer spellCircle;

    private Boolean cantrip;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;
}