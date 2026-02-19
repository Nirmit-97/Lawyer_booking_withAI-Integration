package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.AppointmentDTO;
import com.legalconnect.lawyerbooking.dto.BookingRequest;
import com.legalconnect.lawyerbooking.entity.Appointment;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.repository.AppointmentRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Transactional
    public AppointmentDTO createAppointment(Long userId, BookingRequest request) {
        // Validate lawyer exists
        Optional<Lawyer> lawyerOpt = lawyerRepository.findById(request.getLawyerId());
        if (lawyerOpt.isEmpty()) {
            throw new IllegalArgumentException("Lawyer not found");
        }

        // Validate user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Check for overlapping appointments
        LocalDateTime startTime = request.getAppointmentDate();
        LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());
        
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
            request.getLawyerId(), startTime, endTime
        );
        
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Lawyer is not available at this time. Please choose another time slot.");
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setUserId(userId); // System-provided ID for the user
        appointment.setLawyerId(request.getLawyerId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setStatus("REQUESTED");
        appointment.setMeetingType(request.getMeetingType());
        appointment.setDescription(request.getDescription());
        appointment.setNotes(request.getNotes());
        appointment.setCaseId(request.getCaseId());
        appointment.setRequestedByRole(request.getRequestedByRole() != null ? request.getRequestedByRole() : "user");

        // Phase 0: Prerequisite check
        if (request.getCaseId() != null) {
            Optional<Case> caseOpt = caseRepository.findById(request.getCaseId());
            if (caseOpt.isPresent()) {
                Case caseEntity = caseOpt.get();
                // Strictly allow only IN_PROGRESS cases (Phase 0)
                if (!CaseStatus.IN_PROGRESS.equals(caseEntity.getCaseStatus())) {
                    throw new IllegalArgumentException("Phase 0 Exception: Appointments can only be scheduled for cases with IN_PROGRESS status. Current status: " + caseEntity.getCaseStatus());
                }
            } else {
                throw new IllegalArgumentException("Case not found for ID: " + request.getCaseId());
            }
        } else {
             throw new IllegalArgumentException("An active case link is mandatory for establishing an appointment.");
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Notify counter-party (Stub: Actual logic in notificationService)
        // notificationService.sendAppointmentAlert(saved);

        return convertToDTO(saved);
    }

    @Transactional
    public AppointmentDTO proposeReschedule(Long appointmentId, Long userId, String role, BookingRequest request) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();

        // Ownership verification (Role-aware)
        if ("lawyer".equalsIgnoreCase(role)) {
            if (!appointment.getLawyerId().equals(userId)) {
                throw new IllegalArgumentException("Unauthorized: You can only reschedule your own appointments");
            }
        } else {
            if (!appointment.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Unauthorized: You can only reschedule your own appointments");
            }
        }

        // Check availability
        LocalDateTime startTime = request.getAppointmentDate();
        LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
            appointment.getLawyerId(), startTime, endTime
        );
        overlapping = overlapping.stream().filter(a -> !a.getId().equals(appointmentId)).collect(Collectors.toList());
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Slot unavailable. Please select another temporal window.");
        }

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setStatus("RESCHEDULED");
        appointment.setRequestedByRole(role); // Track who proposed the new time
        
        Appointment updated = appointmentRepository.save(appointment);
        
        // Notify counter-party of reschedule
        // notificationService.sendRescheduleAlert(updated);

        return convertToDTO(updated);
    }

    @Transactional
    public AppointmentDTO confirmAppointment(Long appointmentId, Long userId, String role) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        String currentStatus = appointment.getStatus();

        // Phase 4/5 Logic: Who can confirm depends on current state
        if ("REQUESTED".equals(currentStatus)) {
            // Initial request: Lawyer confirms if user requested, User confirms if lawyer requested
            if (role.equals("lawyer") && "user".equals(appointment.getRequestedByRole())) {
                appointment.setStatus("CONFIRMED");
            } else if (role.equals("user") && "lawyer".equals(appointment.getRequestedByRole())) {
                appointment.setStatus("CONFIRMED");
            } else {
                throw new IllegalArgumentException("Waiting for counter-party authorization.");
            }
        } else if ("RESCHEDULED".equals(currentStatus)) {
            // Reschedule: Confirm if the other party suggested it
            if (!role.equals(appointment.getRequestedByRole())) {
                appointment.setStatus("CONFIRMED");
            } else {
                throw new IllegalArgumentException("Awaiting counter-party confirmation for the new slot.");
            }
        } else {
            throw new IllegalArgumentException("State collision: Cannot confirm session in status " + currentStatus);
        }

        Appointment updated = appointmentRepository.save(appointment);
        
        // Finalize notification
        // notificationService.sendConfirmationAlert(updated);

        return convertToDTO(updated);
    }

    @Transactional
    public AppointmentDTO completeAppointment(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Only confirmed sessions can be marked as COMPLETED.");
        }

        appointment.setStatus("COMPLETED");
        Appointment updated = appointmentRepository.save(appointment);
        return convertToDTO(updated);
    }

    public List<AppointmentDTO> getUserAppointments(Long userId) {
        // Enforce Phase 18: Filter by IN_PROGRESS/CLOSED via Repository JOIN
        List<Appointment> appointments = appointmentRepository.findActiveByUserId(userId);
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getLawyerAppointments(Long lawyerId) {
        // Enforce Phase 18: Filter by IN_PROGRESS/CLOSED via Repository JOIN
        List<Appointment> appointments = appointmentRepository.findActiveByLawyerId(lawyerId);
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getUpcomingUserAppointments(Long userId) {
        List<Appointment> appointments = appointmentRepository.findUpcomingByUserId(userId, LocalDateTime.now());
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getUpcomingLawyerAppointments(Long lawyerId) {
        List<Appointment> appointments = appointmentRepository.findUpcomingByLawyerId(lawyerId, LocalDateTime.now());
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // Admin: Get all appointments in the system
    public List<AppointmentDTO> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public AppointmentDTO getAppointmentById(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        
        Appointment appointment = appointmentOpt.get();
        // Phase 18: Verify associated case status
        if (appointment.getCaseId() != null) {
            caseRepository.findById(appointment.getCaseId()).ifPresent(c -> {
                if (!CaseStatus.IN_PROGRESS.equals(c.getCaseStatus()) && !CaseStatus.CLOSED.equals(c.getCaseStatus())) {
                    throw new IllegalArgumentException("Appointment details are locked until the case is active.");
                }
            });
        }
        
        return convertToDTO(appointment);
    }

    @Transactional
    public AppointmentDTO updateAppointmentStatus(Long appointmentId, String status) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new IllegalArgumentException("Appointment not found");
        }
        appointmentRepository.deleteById(appointmentId);
    }

    @Transactional
    public AppointmentDTO cancelAppointment(Long appointmentId, Long requesterId, String role) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        
        // Ownership verification (Role-aware)
        if ("lawyer".equalsIgnoreCase(role)) {
            if (!appointment.getLawyerId().equals(requesterId)) {
                throw new IllegalArgumentException("Unauthorized: You can only cancel your own appointments");
            }
        } else {
            if (!appointment.getUserId().equals(requesterId)) {
                throw new IllegalArgumentException("Unauthorized: You can only cancel your own appointments");
            }
        }

        // Can only cancel pending or confirmed appointments
        if (!appointment.getStatus().equals("REQUESTED") && !appointment.getStatus().equals("CONFIRMED")) {
            throw new IllegalArgumentException("Cannot cancel appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus("CANCELLED");
        Appointment updated = appointmentRepository.save(appointment);
        return convertToDTO(updated);
    }

    @Transactional
    public AppointmentDTO updateAppointment(Long appointmentId, Long requesterId, String role, BookingRequest request) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();

        // Ownership verification (Role-aware)
        if ("lawyer".equalsIgnoreCase(role)) {
            if (!appointment.getLawyerId().equals(requesterId)) {
                throw new IllegalArgumentException("Unauthorized: You can only edit your own appointments");
            }
        } else {
            if (!appointment.getUserId().equals(requesterId)) {
                throw new IllegalArgumentException("Unauthorized: You can only edit your own appointments");
            }
        }

        // Can only edit pending appointments
        if (!appointment.getStatus().equals("REQUESTED")) {
            throw new IllegalArgumentException("Cannot edit appointment with status: " + appointment.getStatus());
        }

        // Check for overlapping appointments if date/time changed
        if (!appointment.getAppointmentDate().equals(request.getAppointmentDate()) || 
            !appointment.getDurationMinutes().equals(request.getDurationMinutes())) {
            
            LocalDateTime startTime = request.getAppointmentDate();
            LocalDateTime endTime = startTime.plusMinutes(request.getDurationMinutes());
            
            List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                request.getLawyerId(), startTime, endTime
            );
            
            // Exclude current appointment from overlap check
            overlapping = overlapping.stream()
                .filter(a -> !a.getId().equals(appointmentId))
                .collect(Collectors.toList());

            if (!overlapping.isEmpty()) {
                throw new IllegalArgumentException("Lawyer is not available at this time. Please choose another time slot.");
            }
        }

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setMeetingType(request.getMeetingType());
        appointment.setDescription(request.getDescription());
        appointment.setNotes(request.getNotes());
        appointment.setCaseId(request.getCaseId());

        Appointment updated = appointmentRepository.save(appointment);
        return convertToDTO(updated);
    }


    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setUserId(appointment.getUserId());
        dto.setLawyerId(appointment.getLawyerId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setDurationMinutes(appointment.getDurationMinutes());
        dto.setStatus(appointment.getStatus());
        dto.setMeetingType(appointment.getMeetingType());
        dto.setDescription(appointment.getDescription());
        dto.setNotes(appointment.getNotes());
        dto.setCaseId(appointment.getCaseId());
        dto.setRequestedByRole(appointment.getRequestedByRole());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());

        // Fetch user and lawyer names
        Optional<User> userOpt = userRepository.findById(appointment.getUserId());
        Optional<Lawyer> lawyerOpt = lawyerRepository.findById(appointment.getLawyerId());
        
        if (userOpt.isPresent()) {
            dto.setUserFullName(userOpt.get().getFullName());
        }
        if (lawyerOpt.isPresent()) {
            dto.setLawyerFullName(lawyerOpt.get().getFullName());
        }

        return dto;
    }

    private boolean isValidStatus(String status) {
        return status != null && 
               (status.equals("REQUESTED") || 
                status.equals("CONFIRMED") || 
                status.equals("COMPLETED") || 
                status.equals("CANCELLED") ||
                status.equals("RESCHEDULED") ||
                status.equals("NO_SHOW"));
    }
}

