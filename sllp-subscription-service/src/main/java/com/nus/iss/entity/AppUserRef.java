package com.nus.iss.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal read/write projection of the app_user table.
 * Used only to synchronise the role column when a subscription changes.
 * The user-service owns the full AppUser entity; this class intentionally
 * maps only the fields the subscription service needs to touch.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUserRef {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String role;
}
