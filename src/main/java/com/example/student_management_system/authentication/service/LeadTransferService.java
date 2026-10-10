package com.example.student_management_system.authentication.service;

import com.example.student_management_system.authentication.dto.LeadTransferResponse;
import com.example.student_management_system.authentication.model.Role;
import com.example.student_management_system.authentication.model.User;
import com.example.student_management_system.authentication.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadTransferService {

    private final UserRepository userRepository;

    public LeadTransferService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public LeadTransferResponse transferLead(Long newLeadUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String currentLeadEmail = authentication.getName();
        User currentLead = userRepository.findByEmail(currentLeadEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current lead not found"));

        if (currentLead.getRole() != Role.LEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the current LEAD can transfer leadership");
        }

        User targetUser = userRepository.findById(newLeadUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (targetUser.getRole() == Role.LEAD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is already the LEAD");
        }

        currentLead.changeRole(Role.STUDENT);
        userRepository.saveAndFlush(currentLead);

        targetUser.changeRole(Role.LEAD);
        userRepository.saveAndFlush(targetUser);

        return new LeadTransferResponse(currentLeadEmail, targetUser.getEmail(), "Leadership transferred successfully");
    }
}
