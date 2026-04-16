package com.hometalk.onepass.community.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    // 변경 method
    public void updateName(String name) {
        this.name = name;
    }

    // Tag가 1인 관계
    @OneToMany(mappedBy = "tag")
    private List<PostTag> postTags = new ArrayList<>();
}
