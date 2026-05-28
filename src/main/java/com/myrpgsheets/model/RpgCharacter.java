package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpgCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Character name is required.")
    private String name;

    private String race;

    private String characterClass;

    @NotNull(message = "Level is required.")
    private Integer level;

    private Integer hitPoints;

    private Integer armorClass;

    private Integer strength;
    private Integer dexterity;
    private Integer constitution;
    private Integer intelligence;
    private Integer wisdom;
    private Integer charisma;

    private String spellcastingAbility;

    @Column(length = 1000)
    private String description;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] avatarData;

    private String avatarContentType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryItem> inventoryItems;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterSpell> spells;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpellSlot> spellSlots;

    // Métodos para calcular bônus de proficiência, modificadores de habilidade, etc.

    public Integer getProficiencyBonus() {
        if (level == null) {
            return 2;
        }

        if (level >= 17) {
            return 6;
        }

        if (level >= 13) {
            return 5;
        }

        if (level >= 9) {
            return 4;
        }

        if (level >= 5) {
            return 3;
        }

        return 2;
    }

    public Integer getAbilityModifier(Integer abilityScore) {
        if (abilityScore == null) {
            abilityScore = 10;
        }

        return Math.floorDiv(abilityScore - 10, 2);
    }

    public Integer getSpellcastingModifier() {
        if (spellcastingAbility == null || spellcastingAbility.isBlank()) {
            return 0;
        }

        return switch (spellcastingAbility) {
            case "intelligence" -> getAbilityModifier(intelligence);
            case "wisdom" -> getAbilityModifier(wisdom);
            case "charisma" -> getAbilityModifier(charisma);
            default -> 0;
        };
    }

    public Integer getSpellAttackBonus() {
        return getProficiencyBonus() + getSpellcastingModifier();
    }

    public Integer getSpellSaveDc() {
        return 8 + getProficiencyBonus() + getSpellcastingModifier();
    }

    public String getSpellcastingAbilityLabel() {
        if (spellcastingAbility == null || spellcastingAbility.isBlank()) {
            return "Não definido";
        }

        return switch (spellcastingAbility) {
            case "intelligence" -> "INT";
            case "wisdom" -> "SAB";
            case "charisma" -> "CAR";
            default -> "Não definido";
        };
    }

    public String getFormattedProficiencyBonus() {
        return formatBonus(getProficiencyBonus());
    }

    public String getFormattedSpellcastingModifier() {
        return formatBonus(getSpellcastingModifier());
    }

    public String getFormattedSpellAttackBonus() {
        return formatBonus(getSpellAttackBonus());
    }

    public boolean hasAvatar() {
        return avatarData != null && avatarData.length > 0;
    }

    private String formatBonus(Integer value) {
        if (value == null) {
            value = 0;
        }

        if (value >= 0) {
            return "+" + value;
        }

        return value.toString();
    }
}