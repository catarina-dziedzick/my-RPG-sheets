package com.myrpgsheets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campaign_characters")
@Getter
@Setter
@NoArgsConstructor
public class CampaignCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private User player;
}
