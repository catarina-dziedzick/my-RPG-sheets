package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @NotBlank(message = "Nome é obrigatório.")
    private String name;

    @NotBlank(message = "Raça é obrigatória.")
    private String race;

    @NotBlank(message = "Classe é obrigatória.")
    private String characterClass;

    @NotNull(message = "Nível é obrigatório.")
    @Min(value = 1, message = "Nível mínimo é 1.")
    @Max(value = 20, message = "Nível máximo é 20.")
    private Integer level;

    @NotNull(message = "XP é obrigatório.")
    @Min(value = 0, message = "XP não pode ser negativo.")
    private Integer experiencePoints;

    @NotNull(message = "Pontos de Vida são obrigatórios.")
    @Min(value = 0, message = "HP não pode ser negativo.")
    private Integer hitPoints;

    @NotNull(message = "Vida Máxima é obrigatória.")
    @Min(value = 1, message = "Vida Máxima deve ser ao menos 1.")
    private Integer maxHitPoints;

    private Integer temporaryHitPoints;

    @NotNull(message = "Classe de Armadura é obrigatória.")
    @Min(value = 0, message = "CA não pode ser negativa.")
    private Integer armorClass;

    @NotNull(message = "Força é obrigatória.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer strength;

    @NotNull(message = "Destreza é obrigatória.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer dexterity;

    @NotNull(message = "Constituição é obrigatória.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer constitution;

    @NotNull(message = "Inteligência é obrigatória.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer intelligence;

    @NotNull(message = "Sabedoria é obrigatória.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer wisdom;

    @NotNull(message = "Carisma é obrigatório.")
    @Min(value = 1, message = "Atributo mínimo é 1.") @Max(value = 30, message = "Atributo máximo é 30.")
    private Integer charisma;

    private String spellcastingAbility;

    private String background;
    private String alignment;

    // ===== Proficiências em Testes de Resistência =====
    private Boolean saveProfStrength;
    private Boolean saveProfDexterity;
    private Boolean saveProfConstitution;
    private Boolean saveProfIntelligence;
    private Boolean saveProfWisdom;
    private Boolean saveProfCharisma;

    // ===== Proficiências em Perícias =====
    private Boolean profAcrobatics;        // Acrobacia (Destreza)
    private Boolean profAnimalHandling;    // Adestrar Animais (Sabedoria)
    private Boolean profArcana;            // Arcanismo (Inteligência)
    private Boolean profAthletics;         // Atletismo (Força)
    private Boolean profPerformance;       // Atuação (Carisma)
    private Boolean profDeception;         // Enganação (Carisma)
    private Boolean profStealth;           // Furtividade (Destreza)
    private Boolean profHistory;           // História (Inteligência)
    private Boolean profIntimidation;      // Intimidação (Carisma)
    private Boolean profInsight;           // Intuição (Sabedoria)
    private Boolean profInvestigation;     // Investigação (Inteligência)
    private Boolean profMedicine;          // Medicina (Sabedoria)
    private Boolean profNature;            // Natureza (Inteligência)
    private Boolean profPerception;        // Percepção (Sabedoria)
    private Boolean profPersuasion;        // Persuasão (Carisma)
    private Boolean profSleightOfHand;     // Prestidigitação (Destreza)
    private Boolean profReligion;          // Religião (Inteligência)
    private Boolean profSurvival;          // Sobrevivência (Sabedoria)

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

    // ===== Perícias =====

    private int skillBonus(Integer abilityScore, Boolean proficient) {
        int mod = getAbilityModifier(abilityScore);
        if (Boolean.TRUE.equals(proficient)) mod += getProficiencyBonus();
        return mod;
    }

    public String getSkillAcrobatics()     { return formatBonus(skillBonus(dexterity,     profAcrobatics)); }
    public String getSkillAnimalHandling() { return formatBonus(skillBonus(wisdom,         profAnimalHandling)); }
    public String getSkillArcana()         { return formatBonus(skillBonus(intelligence,   profArcana)); }
    public String getSkillAthletics()      { return formatBonus(skillBonus(strength,       profAthletics)); }
    public String getSkillPerformance()    { return formatBonus(skillBonus(charisma,       profPerformance)); }
    public String getSkillDeception()      { return formatBonus(skillBonus(charisma,       profDeception)); }
    public String getSkillStealth()        { return formatBonus(skillBonus(dexterity,      profStealth)); }
    public String getSkillHistory()        { return formatBonus(skillBonus(intelligence,   profHistory)); }
    public String getSkillIntimidation()   { return formatBonus(skillBonus(charisma,       profIntimidation)); }
    public String getSkillInsight()        { return formatBonus(skillBonus(wisdom,         profInsight)); }
    public String getSkillInvestigation()  { return formatBonus(skillBonus(intelligence,   profInvestigation)); }
    public String getSkillMedicine()       { return formatBonus(skillBonus(wisdom,         profMedicine)); }
    public String getSkillNature()         { return formatBonus(skillBonus(intelligence,   profNature)); }
    public String getSkillPerception()     { return formatBonus(skillBonus(wisdom,         profPerception)); }
    public String getSkillPersuasion()     { return formatBonus(skillBonus(charisma,       profPersuasion)); }
    public String getSkillSleightOfHand()  { return formatBonus(skillBonus(dexterity,      profSleightOfHand)); }
    public String getSkillReligion()       { return formatBonus(skillBonus(intelligence,   profReligion)); }
    public String getSkillSurvival()       { return formatBonus(skillBonus(wisdom,         profSurvival)); }

    // ===== Testes de Resistência =====
    public String getSaveStrength()        { return formatBonus(skillBonus(strength,      saveProfStrength)); }
    public String getSaveDexterity()       { return formatBonus(skillBonus(dexterity,     saveProfDexterity)); }
    public String getSaveConstitution()    { return formatBonus(skillBonus(constitution,  saveProfConstitution)); }
    public String getSaveIntelligence()    { return formatBonus(skillBonus(intelligence,  saveProfIntelligence)); }
    public String getSaveWisdom()          { return formatBonus(skillBonus(wisdom,         saveProfWisdom)); }
    public String getSaveCharisma()        { return formatBonus(skillBonus(charisma,       saveProfCharisma)); }

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