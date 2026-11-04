package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final TokenService tokenService;

	public AppointmentService(
		AppointmentRepository appointmentRepository,
		DoctorRepository doctorRepository,
		PatientRepository patientRepository,
		TokenService tokenService
	) {
		this.appointmentRepository = appointmentRepository;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.tokenService = tokenService;
	}

	@Transactional
	public int bookAppointment(Appointment appointment, String token) {
		// Bind the new appointment to the patient identified by the submitted token.
		try {
			Patient patient = getAuthenticatedPatient(token);
			if (patient == null || appointment == null) return 0;
			if (appointment.getPatient() != null && appointment.getPatient().getId() != null &&
				!patient.getId().equals(appointment.getPatient().getId())) return 0;

			Long doctorId = appointment.getDoctor() == null ? null : appointment.getDoctor().getId();
			Doctor doctor = doctorId == null ? null : doctorRepository.findById(doctorId).orElse(null);
			if (doctor == null) return -1;
			appointment.setDoctor(doctor);
			appointment.setPatient(patient);
			if (!isAppointmentAvailable(doctor, appointment.getAppointmentTime(), null)) return 0;

			appointment.setStatus(0);
			appointmentRepository.save(appointment);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	@Transactional
	public int updateAppointment(Appointment requestedAppointment, String token) {
		// Confirm ownership before changing an appointment and validate its new slot.
		try {
			Patient patient = getAuthenticatedPatient(token);
			if (patient == null || requestedAppointment == null || requestedAppointment.getId() == null) return 0;

			Appointment existing = appointmentRepository.findById(requestedAppointment.getId()).orElse(null);
			if (existing == null) return -1;
			if (!patient.getId().equals(existing.getPatient().getId())) return 0;

			Long doctorId = requestedAppointment.getDoctor() == null
				? existing.getDoctor().getId()
				: requestedAppointment.getDoctor().getId();
			Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
			if (doctor == null) return -1;

			LocalDateTime requestedTime = requestedAppointment.getAppointmentTime();
			if (!isAppointmentAvailable(doctor, requestedTime, existing.getId())) return 0;
			existing.setDoctor(doctor);
			existing.setAppointmentTime(requestedTime);
			existing.setStatus(0);
			appointmentRepository.save(existing);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	@Transactional
	public int cancelAppointment(Long appointmentId, String token) {
		// Allow cancellation only by the patient who owns the appointment.
		try {
			Patient patient = getAuthenticatedPatient(token);
			if (patient == null) return 0;
			Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
			if (appointment == null) return -1;
			if (!patient.getId().equals(appointment.getPatient().getId())) return 0;
			appointmentRepository.delete(appointment);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	public List<AppointmentDTO> getAppointments(String dateValue, String patientName, String token) {
		// Resolve the doctor from the token and fetch appointments for the requested day.
		String email = tokenService.extractEmail(token);
		Doctor doctor = email == null ? null : doctorRepository.findByEmail(email);
		if (doctor == null) return List.of();

		LocalDate date = LocalDate.parse(dateValue);
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();
		List<Appointment> appointments;
		if (patientName == null || patientName.isBlank() || patientName.equalsIgnoreCase("null")) {
			appointments = appointmentRepository.findByDoctor_IdAndAppointmentTimeBetween(
				doctor.getId(), start, end);
		} else {
			appointments = appointmentRepository.findByDoctor_IdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
				doctor.getId(), patientName.trim(), start, end);
		}
		return appointments.stream().map(AppointmentService::toDto).toList();
	}

	@Transactional
	public int changeStatus(Long appointmentId, int status) {
		// Restrict appointment statuses to the supported scheduled/completed values.
		if (status != 0 && status != 1) return 0;
		return appointmentRepository.updateStatus(status, appointmentId) == 1 ? 1 : -1;
	}

	public int validateAppointment(Appointment appointment) {
		// Return -1 for an unknown doctor, 1 for an available slot, or 0 otherwise.
		if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
			return -1;
		}
		Doctor doctor = doctorRepository.findById(appointment.getDoctor().getId()).orElse(null);
		if (doctor == null) return -1;
		return isAppointmentAvailable(doctor, appointment.getAppointmentTime(), null) ? 1 : 0;
	}

	private Patient getAuthenticatedPatient(String token) {
		String email = tokenService.extractEmail(token);
		return email == null ? null : patientRepository.findByEmail(email);
	}

	private boolean isAppointmentAvailable(Doctor doctor, LocalDateTime requestedTime, Long ignoredAppointmentId) {
		if (requestedTime == null || !requestedTime.isAfter(LocalDateTime.now())) return false;
		LocalDate date = requestedTime.toLocalDate();
		LocalTime requestedStart = requestedTime.toLocalTime().withSecond(0).withNano(0);
		boolean offered = doctor.getAvailableTimes().stream()
			.map(AppointmentService::slotStart)
			.anyMatch(requestedStart::equals);
		if (!offered) return false;

		LocalDateTime dayStart = date.atStartOfDay();
		LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
		return appointmentRepository.findByDoctor_IdAndAppointmentTimeBetweenAndStatus(
				doctor.getId(), dayStart, dayEnd, 0).stream()
			.filter(existing -> ignoredAppointmentId == null || !ignoredAppointmentId.equals(existing.getId()))
			.map(Appointment::getAppointmentTime)
			.filter(time -> time != null)
			.noneMatch(bookedTime -> requestedTime.isBefore(bookedTime.plusHours(1)) &&
				bookedTime.isBefore(requestedTime.plusHours(1)));
	}

	private static LocalTime slotStart(String slot) {
		try {
			return LocalTime.parse(slot.split("-", 2)[0].trim()).withSecond(0).withNano(0);
		} catch (RuntimeException error) {
			return null;
		}
	}

	private static AppointmentDTO toDto(Appointment appointment) {
		Doctor doctor = appointment.getDoctor();
		Patient patient = appointment.getPatient();
		return new AppointmentDTO(
			appointment.getId(),
			doctor == null ? null : doctor.getId(),
			doctor == null ? null : doctor.getName(),
			patient == null ? null : patient.getId(),
			patient == null ? null : patient.getName(),
			patient == null ? null : patient.getEmail(),
			patient == null ? null : patient.getPhone(),
			patient == null ? null : patient.getAddress(),
			appointment.getAppointmentTime(),
			appointment.getStatus()
		);
	}
// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
//    - Instruction: Add `@Service` above the class definition.

// 2. **Constructor Injection for Dependencies**:
//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
//    - These dependencies should be injected through the constructor.
//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

// 3. **Add @Transactional Annotation for Methods that Modify Database**:
//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

// 4. **Book Appointment Method**:
//    - Responsible for saving the new appointment to the database.
//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.

// 5. **Update Appointment Method**:
//    - This method is used to update an existing appointment based on its ID.
//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
//    - Instruction: Ensure proper validation and error handling is included for appointment updates.

// 6. **Cancel Appointment Method**:
//    - This method cancels an appointment by deleting it from the database.
//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.

// 7. **Get Appointments Method**:
//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.

// 8. **Change Status Method**:
//    - This method updates the status of an appointment by changing its value in the database.
//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.


}
