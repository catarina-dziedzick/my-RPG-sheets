package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "character_abilities")
@Getter
@Setter
@NoArgsConstructor
public class CharacterAbility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ability name is required.")
    private String name;

    /**
     * Type / source of the ability (e.g.: Racial, Classe, Talento, Passiva, Ativa).
     */
    private String abilityType;

    /**
     * Uses left / per rest, if applicable.
     */
    private Integer uses;

    private Integer maxUses;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;
}

