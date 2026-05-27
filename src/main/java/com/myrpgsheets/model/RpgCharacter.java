package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    private Integer strength;
    private Integer dexterity;
    private Integer constitution;
    private Integer intelligence;
    private Integer wisdom;
    private Integer charisma;

    @Column(length = 1000)
    private String description;

    private String avatarUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}