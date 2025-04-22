package com.example.storesecurity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    private int id;

    private int storeId;

    private String password;

    private String username;

    private String email;

    private String phone;
}
