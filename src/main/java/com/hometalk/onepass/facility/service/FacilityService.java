package com.hometalk.onepass.facility.service;

import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.repository.FacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;

    @Transactional
    public Long register(Facility facility) {
        return facilityRepository.save(facility).getId();
    }

    public List<Facility> findAll() {
        return facilityRepository.findAll();
    }

    public Facility findOne(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 시설을 찾을 수 없습니다."));
    }

    @Transactional
    public void update(Long id, String name ,String location) {
        Facility facility = findOne(id);
        facility.updateInfo(name, location);
    }
}
