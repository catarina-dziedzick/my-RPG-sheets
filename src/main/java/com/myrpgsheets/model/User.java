package com.myrpgsheets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    private String name;

    @Email(message = "Enter a valid email.")
    @NotBlank(message = "Email is required.")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;
}