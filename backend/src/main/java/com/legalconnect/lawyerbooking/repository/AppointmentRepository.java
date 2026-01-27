package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OPTIMIZED APPOINTMENT REPOSITORY
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Paginated query methods
 * - Optimized native queries
 * - Proper indexing support
 * - Efficient date range queries
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    // PAGINATED METHODS (recommended for production)
    
    /**
     * Get all appointments for a user with pagination
     */
    Page<Appointment> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Get all appointments for a lawyer with pagination
     */
    Page<Appointment> findByLawyerId(Long lawyerId, Pageable pageable);
    
    /**
     * Get appointments by status with pagination
     */
    Page<Appointment> findByStatus(String status, Pageable pageable);
    
    /**
     * Get upcoming appointments for a user with pagination
     */
    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    Page<Appointment> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Get upcoming appointments for a lawyer with pagination
     */
    @Query("SELECT a FROM Appointment a WHERE a.lawyerId = :lawyerId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    Page<Appointment> findUpcomingByLawyerId(@Param("lawyerId") Long lawyerId, @Param("now") LocalDateTime now, Pageable pageable);
    
    // LEGACY METHODS (deprecated - use paginated versions)
    
    @Deprecated
    List<Appointment> findByUserIdOrderByAppointmentDateDesc(Long userId);
    
    @Deprecated
    List<Appointment> findByLawyerIdOrderByAppointmentDateDesc(Long lawyerId);
    
    @Deprecated
    List<Appointment> findByStatusOrderByAppointmentDateAsc(String status);
    
    @Deprecated
    List<Appointment> findByUserIdAndStatusOrderByAppointmentDateAsc(Long userId, String status);
    
    @Deprecated
    List<Appointment> findByLawyerIdAndStatusOrderByAppointmentDateAsc(Long lawyerId, String status);
    
    @Deprecated
    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Deprecated
    @Query("SELECT a FROM Appointment a WHERE a.lawyerId = :lawyerId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByLawyerId(@Param("lawyerId") Long lawyerId, @Param("now") LocalDateTime now);
    
    // OPTIMIZED QUERY METHODS
    
    /**
     * Check for overlapping appointments for a lawyer
     * 
     * PERFORMANCE: Uses native MySQL DATE_ADD function
     * INDEX: Uses idx_appointment_lawyer_schedule
     */
    @Query(value = "SELECT * FROM appointments WHERE lawyer_id = :lawyerId " +
           "AND status != 'cancelled' " +
           "AND appointment_date < :endTime " +
           "AND DATE_ADD(appointment_date, INTERVAL duration_minutes MINUTE) > :startTime", 
           nativeQuery = true)
    List<Appointment> findOverlappingAppointments(
        @Param("lawyerId") Long lawyerId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Get appointment statistics for a lawyer
     * 
     * PERFORMANCE: Single query for dashboard metrics
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) as cancelled " +
           "FROM appointments WHERE lawyer_id = :lawyerId", 
           nativeQuery = true)
    Object[] getLawyerAppointmentStats(@Param("lawyerId") Long lawyerId);
    
    /**
     * Get appointment statistics for a user
     * 
     * PERFORMANCE: Single query for dashboard metrics
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN status = 'cancelled' THEN 1 ELSE 0 END) as cancelled " +
           "FROM appointments WHERE user_id = :userId", 
           nativeQuery = true)
    Object[] getUserAppointmentStats(@Param("userId") Long userId);
    
    /**
     * Get appointments in a date range for a lawyer
     * 
     * PERFORMANCE: Optimized date range query
     * INDEX: Uses idx_appointment_lawyer_schedule
     */
    @Query("SELECT a FROM Appointment a WHERE a.lawyerId = :lawyerId " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findLawyerAppointmentsInDateRange(
        @Param("lawyerId") Long lawyerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Get appointments in a date range for a user
     * 
     * PERFORMANCE: Optimized date range query
     * INDEX: Uses idx_appointment_user_history
     */
    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId " +
           "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDate DESC")
    List<Appointment> findUserAppointmentsInDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

