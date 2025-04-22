package com.example.storesecurity.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private int id;

    private int storeId;

    private String password;

    private String username;

    private String email;

    private String phone;
}
