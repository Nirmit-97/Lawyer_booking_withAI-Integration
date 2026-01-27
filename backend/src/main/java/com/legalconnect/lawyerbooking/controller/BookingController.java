package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.AppointmentDTO;
import com.legalconnect.lawyerbooking.dto.BookingRequest;
import com.legalconnect.lawyerbooking.dto.BookingResponse;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private LawyerRepository lawyerRepository;

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createAppointment(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody BookingRequest request) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BookingResponse(false, "User ID is required"));
            }

            AppointmentDTO appointment = bookingService.createAppointment(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BookingResponse(true, "Appointment booked successfully", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error creating appointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getUserAppointments(@PathVariable Long userId) {
        try {
            List<AppointmentDTO> appointments = bookingService.getUserAppointments(userId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            System.err.println("Error fetching user appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lawyer/{lawyerId}")
    public ResponseEntity<List<AppointmentDTO>> getLawyerAppointments(@PathVariable Long lawyerId) {
        try {
            List<AppointmentDTO> appointments = bookingService.getLawyerAppointments(lawyerId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            System.err.println("Error fetching lawyer appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingUserAppointments(@PathVariable Long userId) {
        try {
            List<AppointmentDTO> appointments = bookingService.getUpcomingUserAppointments(userId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            System.err.println("Error fetching upcoming appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/lawyer/{lawyerId}/upcoming")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingLawyerAppointments(@PathVariable Long lawyerId) {
        try {
            List<AppointmentDTO> appointments = bookingService.getUpcomingLawyerAppointments(lawyerId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            System.err.println("Error fetching upcoming appointments: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentDTO> getAppointment(@PathVariable Long appointmentId) {
        try {
            AppointmentDTO appointment = bookingService.getAppointmentById(appointmentId);
            return ResponseEntity.ok(appointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Error fetching appointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<BookingResponse> updateAppointmentStatus(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestParam String status) {
        try {
            AppointmentDTO appointment = bookingService.updateAppointmentStatus(appointmentId, status);
            return ResponseEntity.ok(new BookingResponse(true, "Appointment status updated", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating appointment status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<BookingResponse> cancelAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BookingResponse(false, "User ID is required"));
            }

            AppointmentDTO appointment = bookingService.cancelAppointment(appointmentId, userId);
            return ResponseEntity.ok(new BookingResponse(true, "Appointment cancelled successfully", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error cancelling appointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<BookingResponse> updateAppointment(
            @PathVariable Long appointmentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody BookingRequest request) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BookingResponse(false, "User ID is required"));
            }

            AppointmentDTO appointment = bookingService.updateAppointment(appointmentId, userId, request);
            return ResponseEntity.ok(new BookingResponse(true, "Appointment updated successfully", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating appointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @PutMapping("/{appointmentId}/confirm")
    public ResponseEntity<BookingResponse> confirmAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestHeader(value = "X-Lawyer-Id", required = false) Long lawyerId) {
        try {
            if (lawyerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BookingResponse(false, "Lawyer ID is required"));
            }

            AppointmentDTO appointment = bookingService.confirmAppointment(appointmentId, lawyerId);
            return ResponseEntity.ok(new BookingResponse(true, "Appointment confirmed successfully", appointment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BookingResponse(false, e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error confirming appointment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/lawyers")
    public ResponseEntity<List<Map<String, Object>>> getAllLawyers() {
        try {
            List<Lawyer> lawyers = lawyerRepository.findAll();
            List<Map<String, Object>> lawyerList = lawyers.stream()
                .map(lawyer -> {
                    Map<String, Object> lawyerMap = new HashMap<>();
                    lawyerMap.put("id", lawyer.getId());
                    lawyerMap.put("fullName", lawyer.getFullName());
                    lawyerMap.put("specializations", lawyer.getSpecializations());
                    lawyerMap.put("barNumber", lawyer.getBarNumber());
                    lawyerMap.put("email", lawyer.getEmail());
                    return lawyerMap;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(lawyerList);
        } catch (Exception e) {
            System.err.println("Error fetching lawyers: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

