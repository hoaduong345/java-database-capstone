package com.project.back_end.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(
        PatientRepository patientRepository,
        AppointmentRepository appointmentRepository,
        TokenService tokenService
    ) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public int createPatient(Patient patient) {
        // Reject duplicate email/phone before persisting a new patient.
        try {
            if (patient == null || patientRepository.findByEmailOrPhone(
                patient.getEmail(), patient.getPhone()) != null) return 0;
            patientRepository.save(patient);
            return 1;
        } catch (RuntimeException error) {
            return 0;
        }
    }

    public List<AppointmentDTO> getPatientAppointment(Long patientId) {
        // Load patient appointments and expose only the fields needed by clients.
        return appointmentRepository.findByPatient_Id(patientId).stream()
            .map(PatientService::toDto)
            .toList();
    }

    public List<AppointmentDTO> filterByCondition(Long patientId, String condition) {
        // Map supported conditions to status codes and keep repository ordering.
        Integer status = statusForCondition(condition);
        if (status == null) return getPatientAppointment(patientId);
        return appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, status)
            .stream()
            .map(PatientService::toDto)
            .toList();
    }

    public List<AppointmentDTO> filterByDoctor(Long patientId, String doctorName) {
        // Search this patient's appointments by a partial doctor name.
        if (doctorName == null || doctorName.isBlank() || doctorName.equalsIgnoreCase("null")) {
            return getPatientAppointment(patientId);
        }
        return appointmentRepository.findByDoctor_NameContainingIgnoreCaseAndPatient_Id(
            doctorName.trim(), patientId).stream()
            .map(PatientService::toDto)
            .toList();
    }

    public List<AppointmentDTO> filterByDoctorAndCondition(
        Long patientId,
        String doctorName,
        String condition
    ) {
        // Apply both doctor-name and appointment-status filters.
        Integer status = statusForCondition(condition);
        if (status == null) return filterByDoctor(patientId, doctorName);
        if (doctorName == null || doctorName.isBlank() || doctorName.equalsIgnoreCase("null")) {
            return filterByCondition(patientId, condition);
        }
        return appointmentRepository.findByDoctor_NameContainingIgnoreCaseAndPatient_IdAndStatus(
            doctorName.trim(), patientId, status).stream()
            .map(PatientService::toDto)
            .toList();
    }

    public Patient getPatientDetails(String token) {
        // Resolve the patient identity from a valid token subject.
        String email = tokenService.extractEmail(token);
        if (email == null || email.isBlank()) return null;
        return patientRepository.findByEmail(email);
    }

    private static Integer statusForCondition(String condition) {
        if (condition == null || condition.isBlank() || condition.equalsIgnoreCase("null") ||
            condition.equalsIgnoreCase("allAppointments")) return null;
        if (condition.equalsIgnoreCase("past") || condition.equalsIgnoreCase("completed")) return 1;
        if (condition.equalsIgnoreCase("future") || condition.equalsIgnoreCase("upcoming")) return 0;
        throw new IllegalArgumentException("Unsupported appointment condition: " + condition);
    }

    private static AppointmentDTO toDto(Appointment appointment) {
        return new AppointmentDTO(
            appointment.getId(),
            appointment.getDoctor() == null ? null : appointment.getDoctor().getId(),
            appointment.getDoctor() == null ? null : appointment.getDoctor().getName(),
            appointment.getPatient() == null ? null : appointment.getPatient().getId(),
            appointment.getPatient() == null ? null : appointment.getPatient().getName(),
            appointment.getPatient() == null ? null : appointment.getPatient().getEmail(),
            appointment.getPatient() == null ? null : appointment.getPatient().getPhone(),
            appointment.getPatient() == null ? null : appointment.getPatient().getAddress(),
            appointment.getAppointmentTime(),
            appointment.getStatus()
        );
    }
// 1. **Add @Service Annotation**:
//    - The `@Service` annotation is used to mark this class as a Spring service component. 
//    - It will be managed by Spring's container and used for business logic related to patients and appointments.
//    - Instruction: Ensure that the `@Service` annotation is applied above the class declaration.

// 2. **Constructor Injection for Dependencies**:
//    - The `PatientService` class has dependencies on `PatientRepository`, `AppointmentRepository`, and `TokenService`.
//    - These dependencies are injected via the constructor to maintain good practices of dependency injection and testing.
//    - Instruction: Ensure constructor injection is used for all the required dependencies.

// 3. **createPatient Method**:
//    - Creates a new patient in the database. It saves the patient object using the `PatientRepository`.
//    - If the patient is successfully saved, the method returns `1`; otherwise, it logs the error and returns `0`.
//    - Instruction: Ensure that error handling is done properly and exceptions are caught and logged appropriately.

// 4. **getPatientAppointment Method**:
//    - Retrieves a list of appointments for a specific patient, based on their ID.
//    - The appointments are then converted into `AppointmentDTO` objects for easier consumption by the API client.
//    - This method is marked as `@Transactional` to ensure database consistency during the transaction.
//    - Instruction: Ensure that appointment data is properly converted into DTOs and the method handles errors gracefully.

// 5. **filterByCondition Method**:
//    - Filters appointments for a patient based on the condition (e.g., "past" or "future").
//    - Retrieves appointments with a specific status (0 for future, 1 for past) for the patient.
//    - Converts the appointments into `AppointmentDTO` and returns them in the response.
//    - Instruction: Ensure the method correctly handles "past" and "future" conditions, and that invalid conditions are caught and returned as errors.

// 6. **filterByDoctor Method**:
//    - Filters appointments for a patient based on the doctor's name.
//    - It retrieves appointments where the doctor’s name matches the given value, and the patient ID matches the provided ID.
//    - Instruction: Ensure that the method correctly filters by doctor's name and patient ID and handles any errors or invalid cases.

// 7. **filterByDoctorAndCondition Method**:
//    - Filters appointments based on both the doctor's name and the condition (past or future) for a specific patient.
//    - This method combines filtering by doctor name and appointment status (past or future).
//    - Converts the appointments into `AppointmentDTO` objects and returns them in the response.
//    - Instruction: Ensure that the filter handles both doctor name and condition properly, and catches errors for invalid input.

// 8. **getPatientDetails Method**:
//    - Retrieves patient details using the `tokenService` to extract the patient's email from the provided token.
//    - Once the email is extracted, it fetches the corresponding patient from the `patientRepository`.
//    - It returns the patient's information in the response body.
    //    - Instruction: Make sure that the token extraction process works correctly and patient details are fetched properly based on the extracted email.

// 9. **Handling Exceptions and Errors**:
//    - The service methods handle exceptions using try-catch blocks and log any issues that occur. If an error occurs during database operations, the service responds with appropriate HTTP status codes (e.g., `500 Internal Server Error`).
//    - Instruction: Ensure that error handling is consistent across the service, with proper logging and meaningful error messages returned to the client.

// 10. **Use of DTOs (Data Transfer Objects)**:
//    - The service uses `AppointmentDTO` to transfer appointment-related data between layers. This ensures that sensitive or unnecessary data (e.g., password or private patient information) is not exposed in the response.
//    - Instruction: Ensure that DTOs are used appropriately to limit the exposure of internal data and only send the relevant fields to the client.



}
