
package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.notice.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}