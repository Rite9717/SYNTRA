package com.project.ims.service;

import com.project.ims.dto.AdminDashboardStats;
import com.project.ims.dto.UpdateUserRequest;
import com.project.ims.dto.UserManagementResponse;
import com.project.ims.model.User;
import com.project.ims.repository.MessageRepository;
import com.project.ims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    public AdminDashboardStats getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByActiveTrue();
        Long totalMessages = messageRepository.count();
        
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long messagesToday = messageRepository.countBySentAtAfter(startOfDay);
        
        Long unreadMessages = messageRepository.countByIsReadFalseAndIsDeletedFalse();
        
        return new AdminDashboardStats(totalUsers, activeUsers, totalMessages, messagesToday, unreadMessages);
    }

    public List<UserManagementResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::convertToUserManagementResponse).collect(Collectors.toList());
    }

    public UserManagementResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToUserManagementResponse(user);
    }

    @Transactional
    public UserManagementResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        }

        User updatedUser = userRepository.save(user);
        return convertToUserManagementResponse(updatedUser);
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.getActive());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        // Soft delete by deactivating
        User user = userRepository.findById(userId).get();
        user.setActive(false);
        userRepository.save(user);
    }

    public List<UserManagementResponse> searchUsers(String keyword) {
        List<User> users = userRepository.findByUsernameContainingOrEmailContainingOrFirstNameContainingOrLastNameContaining(
                keyword, keyword, keyword, keyword);
        return users.stream().map(this::convertToUserManagementResponse).collect(Collectors.toList());
    }

    private UserManagementResponse convertToUserManagementResponse(User user) {
        Long sentCount = messageRepository.countBySenderId(user.getId());
        Long receivedCount = messageRepository.countByReceiverId(user.getId());

        return new UserManagementResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getActive(),
                user.getCreatedAt(),
                user.getLastLogin(),
                user.getRoles(),
                sentCount,
                receivedCount
        );
    }
}
