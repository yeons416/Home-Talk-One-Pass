package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserId(Long userId);
    //List<Complaint> findUserId(Long userId);      --> findBy: 조회하겠다. User: 엔티티 안의 private User user 필드를 참조하겠다. Id: User 엔티티 안에 있는 id 값을 기준으로 하겠다.
}
