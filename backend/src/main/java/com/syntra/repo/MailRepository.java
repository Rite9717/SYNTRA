package com.syntra.repo;

import com.syntra.domain.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MailRepository extends JpaRepository<MailMessage, Long> {
    List<MailMessage> findByReceiverIdAndDeletedFalseAndArchivedFalseOrderBySentAtDesc(Long receiverId);
    List<MailMessage> findBySenderIdAndDeletedFalseOrderBySentAtDesc(Long senderId);
    List<MailMessage> findByReceiverIdAndDeletedFalseAndStarredTrueOrderBySentAtDesc(Long receiverId);
    List<MailMessage> findByReceiverIdAndDeletedFalseAndArchivedTrueOrderBySentAtDesc(Long receiverId);
    List<MailMessage> findByFolderIdAndReceiverIdAndDeletedFalseOrderBySentAtDesc(Long folderId, Long receiverId);
    List<MailMessage> findByThreadIdAndDeletedFalseOrderBySentAtAsc(Long threadId);
    long countByReceiverIdAndDeletedFalseAndReadFlagFalse(Long receiverId);
    long countBySentAtAfter(LocalDateTime after);
    long countByReadFlagFalseAndDeletedFalse();

    @Query("""
            SELECT m FROM MailMessage m
            WHERE m.deleted = false
              AND (m.sender.id = :uid OR m.receiver.id = :uid)
              AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.body) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(m.sender.username) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY m.sentAt DESC
            """)
    List<MailMessage> search(@Param("uid") Long uid, @Param("q") String q);
}
