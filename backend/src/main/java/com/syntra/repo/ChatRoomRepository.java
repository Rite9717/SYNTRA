package com.syntra.repo;

import com.syntra.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByDirectKey(String directKey);

    @Query("""
            SELECT r FROM ChatRoom r
            JOIN RoomMember m ON m.room = r
            WHERE m.user.id = :userId
            ORDER BY r.updatedAt DESC
            """)
    List<ChatRoom> findForUser(@Param("userId") Long userId);
}
