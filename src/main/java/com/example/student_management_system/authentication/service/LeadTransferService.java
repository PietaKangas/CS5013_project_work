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

// This service class handles the transfer of the lead role from one user to another within the system.
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

        // Lock the current lead's row first
        User currentLead = userRepository.findWithLockByEmail(currentLeadEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current lead not found"));

        if (currentLead.getRole() != Role.LEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the current LEAD can transfer leadership");
        }

        User targetUser = userRepository.findById(newLeadUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (targetUser.getRole() == Role.LEAD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is already the LEAD");
        }

        // Order matters: demote and flush first, so the "one LEAD" unique index is not violated.
        currentLead.changeRole(Role.STUDENT);
        userRepository.flush();
        //userRepository.saveAndFlush(currentLead);

        targetUser.changeRole(Role.LEAD);
        userRepository.flush();
        //userRepository.saveAndFlush(targetUser);

        return new LeadTransferResponse(currentLeadEmail, targetUser.getEmail(), "Leadership transferred successfully");
    }
}
