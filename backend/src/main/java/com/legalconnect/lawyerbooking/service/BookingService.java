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
        appointment.setUserId(userId);
        appointment.setLawyerId(request.getLawyerId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setDurationMinutes(request.getDurationMinutes());
        appointment.setStatus("pending");
        appointment.setMeetingType(request.getMeetingType());
        appointment.setDescription(request.getDescription());
        appointment.setDescription(request.getDescription());
        appointment.setNotes(request.getNotes());
        appointment.setCaseId(request.getCaseId());

        Appointment saved = appointmentRepository.save(appointment);

        // Update case status if linked to a case
        if (request.getCaseId() != null) {
            Optional<Case> caseOpt = caseRepository.findById(request.getCaseId());
            if (caseOpt.isPresent()) {
                Case caseEntity = caseOpt.get();
                if (CaseStatus.OPEN.equals(caseEntity.getCaseStatus())) {
                    caseEntity.setCaseStatus(CaseStatus.IN_PROGRESS);
                    // Ensure lawyer is assigned if not already
                    if (caseEntity.getLawyerId() == null) {
                        caseEntity.setLawyerId(request.getLawyerId());
                    }
                    caseRepository.save(caseEntity);
                }
            }
        }

        return convertToDTO(saved);
    }

    public List<AppointmentDTO> getUserAppointments(Long userId) {
        List<Appointment> appointments = appointmentRepository.findByUserIdOrderByAppointmentDateDesc(userId);
        return appointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getLawyerAppointments(Long lawyerId) {
        List<Appointment> appointments = appointmentRepository.findByLawyerIdOrderByAppointmentDateDesc(lawyerId);
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

    public AppointmentDTO getAppointmentById(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }
        return convertToDTO(appointmentOpt.get());
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
    public AppointmentDTO cancelAppointment(Long appointmentId, Long userId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        
        // Verify user owns the appointment
        if (!appointment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }

        // Can only cancel pending or confirmed appointments
        if (!appointment.getStatus().equals("pending") && !appointment.getStatus().equals("confirmed")) {
            throw new IllegalArgumentException("Cannot cancel appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus("cancelled");
        Appointment updated = appointmentRepository.save(appointment);
        return convertToDTO(updated);
    }

    @Transactional
    public AppointmentDTO updateAppointment(Long appointmentId, Long userId, BookingRequest request) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();

        // Verify user owns the appointment
        if (!appointment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only edit your own appointments");
        }

        // Can only edit pending appointments
        if (!appointment.getStatus().equals("pending")) {
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

    @Transactional
    public AppointmentDTO confirmAppointment(Long appointmentId, Long lawyerId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        
        // Verify lawyer owns the appointment
        if (!appointment.getLawyerId().equals(lawyerId)) {
            throw new IllegalArgumentException("You can only confirm your own appointments");
        }

        if (!appointment.getStatus().equals("pending")) {
            throw new IllegalArgumentException("Can only confirm pending appointments");
        }

        appointment.setStatus("confirmed");
        Appointment updated = appointmentRepository.save(appointment);

        // Ensure case is in-progress if it was still open
        if (updated.getCaseId() != null) {
            Optional<Case> caseOpt = caseRepository.findById(updated.getCaseId());
            if (caseOpt.isPresent()) {
                Case caseEntity = caseOpt.get();
                if (CaseStatus.OPEN.equals(caseEntity.getCaseStatus())) {
                    caseEntity.setCaseStatus(CaseStatus.IN_PROGRESS);
                    caseRepository.save(caseEntity);
                }
            }
        }

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
               (status.equals("pending") || 
                status.equals("confirmed") || 
                status.equals("completed") || 
                status.equals("cancelled"));
    }
}

