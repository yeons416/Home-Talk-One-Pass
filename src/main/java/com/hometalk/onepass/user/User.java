package com.hometalk.onepass.user;

import jakarta.persistence.*;
import lombok.Getter;



@Entity
@Getter
@Table(name = "kjh_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
