package com.project.ims.repository;
import com.project.ims.model.Message;
import com.project.ims.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiverAndIsDeletedFalseOrderBySentAtDesc(User receiver);
    List<Message> findBySenderAndIsDeletedFalseOrderBySentAtDesc(User sender);
    List<Message> findByReceiverAndFolderAndIsDeletedFalseOrderBySentAtDesc(User receiver, String folder);
    Long countByReceiverAndIsReadFalseAndIsDeletedFalse(User receiver);
    
    Long countBySentAtAfter(LocalDateTime dateTime);
    Long countByIsReadFalseAndIsDeletedFalse();
    Long countBySenderId(Long senderId);
    Long countByReceiverId(Long receiverId);
}
