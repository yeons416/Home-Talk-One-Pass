package com.hometalk.onepass.entity.auth;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "household")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Household {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @Column(name = "dong", nullable = false, length = 20)
    private String dong;

    @Column(name = "ho", nullable = false, length = 20)
    private String ho;

    @Column(name = "move_in_date", nullable = false)
    private LocalDateTime moveInDate;

    @Column(name = "post_num", nullable = false)
    private Integer postNum;

    @OneToMany(mappedBy = "household", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @Builder
    public Household(String buildingName, String dong, String ho,
                     LocalDateTime moveInDate, Integer postNum) {
        this.buildingName = buildingName;
        this.dong = dong;
        this.ho = ho;
        this.moveInDate = moveInDate;
        this.postNum = postNum;
    }
}
