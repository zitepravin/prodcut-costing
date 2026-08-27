package com.automotive.costing.rfq.repository;
import com.automotive.costing.rfq.entity.RfqAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RfqAttachmentRepository extends JpaRepository<RfqAttachment, String> {
    List<RfqAttachment> findByRfqIdOrderByUploadedAtDesc(String rfqId);
}
