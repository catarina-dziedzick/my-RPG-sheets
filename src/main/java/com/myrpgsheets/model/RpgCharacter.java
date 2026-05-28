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

    private Integer experiencePoints;

    private Integer hitPoints;
    private Integer maxHitPoints;
    private Integer temporaryHitPoints;

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

    public String getFormattedStrengthModifier() {
        return formatAbilityModifier(strength);
    }

    public String getFormattedDexterityModifier() {
        return formatAbilityModifier(dexterity);
    }

    public String getFormattedConstitutionModifier() {
        return formatAbilityModifier(constitution);
    }

    public String getFormattedIntelligenceModifier() {
        return formatAbilityModifier(intelligence);
    }

    public String getFormattedWisdomModifier() {
        return formatAbilityModifier(wisdom);
    }

    public String getFormattedCharismaModifier() {
        return formatAbilityModifier(charisma);
    }

    public boolean hasAvatar() {
        return avatarData != null && avatarData.length > 0;
    }

    public int getHpPercent() {
        int hp  = hitPoints    != null ? hitPoints    : 0;
        int max = maxHitPoints != null ? maxHitPoints : 0;
        if (max <= 0) return 0;
        int pct = (int) Math.round(hp * 100.0 / max);
        if (pct < 0)   return 0;
        if (pct > 100) return 100;
        return pct;
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

    private String formatAbilityModifier(Integer abilityScore) {
        return formatBonus(getAbilityModifier(abilityScore));
    }

    // ===== Progressão de XP (D&D 5e) =====

    /** XP acumulado necessário para alcançar cada nível (índice 1..20). */
    private static final int[] XP_THRESHOLDS = {
            0,         // sentinela (índice 0)
            0,         // nível 1
            300,       // 2
            900,       // 3
            2700,      // 4
            6500,      // 5
            14000,     // 6
            23000,     // 7
            34000,     // 8
            48000,     // 9
            64000,     // 10
            85000,     // 11
            100000,    // 12
            120000,    // 13
            140000,    // 14
            165000,    // 15
            195000,    // 16
            225000,    // 17
            265000,    // 18
            305000,    // 19
            355000     // 20
    };

    public int getCurrentLevelXp() {
        int lvl = level == null ? 1 : Math.max(1, Math.min(20, level));
        return XP_THRESHOLDS[lvl];
    }

    /** XP necessário para o próximo nível (acumulado). Retorna -1 se já está no nível máximo. */
    public int getNextLevelXp() {
        int lvl = level == null ? 1 : Math.max(1, Math.min(20, level));
        if (lvl >= 20) {
            return -1;
        }
        return XP_THRESHOLDS[lvl + 1];
    }

    public boolean isMaxLevel() {
        return level != null && level >= 20;
    }

    public int getXpIntoCurrentLevel() {
        int xp = experiencePoints == null ? 0 : experiencePoints;
        return Math.max(0, xp - getCurrentLevelXp());
    }

    public int getXpNeededForNextLevel() {
        if (isMaxLevel()) {
            return 0;
        }
        return getNextLevelXp() - getCurrentLevelXp();
    }

    /** Percentual de progresso dentro do nível atual (0..100). */
    public int getXpProgressPercent() {
        if (isMaxLevel()) {
            return 100;
        }
        int needed = getXpNeededForNextLevel();
        if (needed <= 0) {
            return 0;
        }
        int into = getXpIntoCurrentLevel();
        int pct = (int) Math.round((into * 100.0) / needed);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    /** Limite "máximo" de XP que pode ser definido (não passa do início do próximo nível). */
    public int getMaxAllowedXp() {
        if (isMaxLevel()) {
            return XP_THRESHOLDS[20];
        }
        // Permite chegar até 1 abaixo do próximo nível
        return getNextLevelXp() - 1;
    }
}