package com.myrpgsheets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    @Column(unique = true)
    private String inviteCode;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "dungeon_master_id")
    private User dungeonMaster;
}
