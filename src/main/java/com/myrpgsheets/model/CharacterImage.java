package com.myrpgsheets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "character_images")
@Getter
@Setter
@NoArgsConstructor
public class CharacterImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caption;

    private String contentType;

    private LocalDateTime uploadedAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] imageData;

    @ManyToOne
    @JoinColumn(name = "character_id")
    private RpgCharacter character;

    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }
}