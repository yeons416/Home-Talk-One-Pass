package com.hometalk.onepass.facility.controller;

import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facility")
public class FacilityController {

    private final FacilityService facilityService;

    // 시설 등록
    @PostMapping
    public Long register(@RequestBody Facility facility) {
        return facilityService.register(facility);
    }

    // 시설 전체 목록 조회
    @GetMapping
    public List<Facility> findAll() {
        return facilityService.findAll();
    }

    // 시설 상세 조회
    @GetMapping("/{id}")
    public Facility findOne(@PathVariable Long id) {
        return facilityService.findOne(id);
    }

    // 시설 정보 수정
    @PatchMapping("/{id}")
    public void update(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("location") String location) {
                facilityService.update(id, name, location);

    }

}
