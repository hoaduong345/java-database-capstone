package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@Service
@Transactional(readOnly = true)
public class DoctorService {

	private final DoctorRepository doctorRepository;
	private final AppointmentRepository appointmentRepository;
	private final TokenService tokenService;

	public DoctorService(
		DoctorRepository doctorRepository,
		AppointmentRepository appointmentRepository,
		TokenService tokenService
	) {
		this.doctorRepository = doctorRepository;
		this.appointmentRepository = appointmentRepository;
		this.tokenService = tokenService;
	}

	public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
		// Load the doctor's offered slots and remove slots already occupied on this date.
		Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
		if (doctor == null || date == null) return List.of();

		List<String> offeredTimes = new ArrayList<>(doctor.getAvailableTimes());
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.plusDays(1).atStartOfDay();
		List<Appointment> appointments = appointmentRepository
			.findByDoctor_IdAndAppointmentTimeBetweenAndStatus(doctorId, start, end, 0);
		List<LocalTime> bookedStarts = appointments.stream()
			.map(Appointment::getAppointmentTime)
			.filter(time -> time != null)
			.map(LocalDateTime::toLocalTime)
			.toList();

		return offeredTimes.stream()
			.filter(slot -> {
				LocalTime slotStart = parseSlotStart(slot);
				return slotStart != null && !bookedStarts.contains(slotStart);
			})
			.toList();
	}

	@Transactional
	public int saveDoctor(Doctor doctor) {
		// Reject duplicate email addresses before creating a doctor record.
		try {
			if (doctor == null || doctorRepository.findByEmail(doctor.getEmail()) != null) return -1;
			doctorRepository.save(doctor);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	@Transactional
	public int updateDoctor(Doctor doctor) {
		// Update only an existing record and preserve its password when none is supplied.
		try {
			if (doctor == null || doctor.getId() == null) return -1;
			Doctor existing = doctorRepository.findById(doctor.getId()).orElse(null);
			if (existing == null) return -1;

			Doctor emailOwner = doctorRepository.findByEmail(doctor.getEmail());
			if (emailOwner != null && !emailOwner.getId().equals(existing.getId())) return -1;

			existing.setName(doctor.getName());
			existing.setSpecialty(doctor.getSpecialty());
			existing.setEmail(doctor.getEmail());
			existing.setPhone(doctor.getPhone());
			if (doctor.getPassword() != null && !doctor.getPassword().isBlank()) {
				existing.setPassword(doctor.getPassword());
			}
			existing.setAvailableTimes(doctor.getAvailableTimes());
			doctorRepository.save(existing);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	public List<Doctor> getDoctors() {
		// Initialize element collections before returning entities to the controller.
		List<Doctor> doctors = doctorRepository.findAll();
		doctors.forEach(doctor -> doctor.getAvailableTimes().size());
		return doctors;
	}

	@Transactional
	public int deleteDoctor(Long doctorId) {
		// Remove dependent appointments before deleting the doctor.
		try {
			if (doctorId == null || !doctorRepository.existsById(doctorId)) return -1;
			appointmentRepository.deleteAllByDoctor_Id(doctorId);
			doctorRepository.deleteById(doctorId);
			return 1;
		} catch (RuntimeException error) {
			return 0;
		}
	}

	public Map<String, Object> validateDoctor(Login login) {
		// Verify doctor credentials and issue a token for the authenticated email.
		Map<String, Object> result = new LinkedHashMap<>();
		try {
			Doctor doctor = login == null ? null : doctorRepository.findByEmail(login.getEmail());
			if (login == null || doctor == null || login.getPassword() == null ||
				!login.getPassword().equals(doctor.getPassword())) {
				result.put("message", "Invalid email or password.");
				return result;
			}
			result.put("message", "Login successful.");
			result.put("token", tokenService.generateToken(doctor.getEmail()));
		} catch (RuntimeException error) {
			result.put("message", "Unable to sign in right now.");
		}
		return result;
	}

	public List<Doctor> findDoctorByName(String name) {
		// Search doctor names without case sensitivity; blank input returns all doctors.
		String normalizedName = normalizeFilter(name);
		return normalizedName == null
			? getDoctors()
			: doctorRepository.findByNameContainingIgnoreCase(normalizedName);
	}

	public List<Doctor> filterDoctorsByNameSpecilityandTime(String name, String specialty, String time) {
		// Apply name and specialty in the repository, then filter the resulting slots by AM/PM.
		String normalizedName = normalizeFilter(name);
		String normalizedSpecialty = normalizeFilter(specialty);
		List<Doctor> candidates;

		if (normalizedName != null && normalizedSpecialty != null) {
			candidates = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
				normalizedName, normalizedSpecialty);
		} else if (normalizedName != null) {
			candidates = doctorRepository.findByNameContainingIgnoreCase(normalizedName);
		} else if (normalizedSpecialty != null) {
			candidates = doctorRepository.findBySpecialtyIgnoreCase(normalizedSpecialty);
		} else {
			candidates = getDoctors();
		}

		candidates.forEach(doctor -> doctor.getAvailableTimes().size());
		return filterDoctorByTime(candidates, time);
	}

	public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String time) {
		// Keep only doctors with at least one availability slot in the requested period.
		String normalizedTime = normalizeFilter(time);
		if (normalizedTime == null) return doctors;
		boolean morning = normalizedTime.equalsIgnoreCase("AM");
		boolean afternoon = normalizedTime.equalsIgnoreCase("PM");
		if (!morning && !afternoon) return doctors;

		return doctors.stream()
			.filter(doctor -> doctor.getAvailableTimes().stream()
				.map(DoctorService::parseSlotStart)
				.filter(slot -> slot != null)
				.anyMatch(slot -> morning ? slot.getHour() < 12 : slot.getHour() >= 12))
			.toList();
	}

	public List<Doctor> filterDoctorByNameAndTime(String name, String time) {
		return filterDoctorByTime(findDoctorByName(name), time);
	}

	public List<Doctor> filterDoctorByNameAndSpecility(String name, String specialty) {
		return filterDoctorsByNameSpecilityandTime(name, specialty, null);
	}

	public List<Doctor> filterDoctorByTimeAndSpecility(String time, String specialty) {
		return filterDoctorsByNameSpecilityandTime(null, specialty, time);
	}

	public List<Doctor> filterDoctorBySpecility(String specialty) {
		return filterDoctorsByNameSpecilityandTime(null, specialty, null);
	}

	public List<Doctor> filterDoctorsByTime(String time) {
		return filterDoctorByTime(getDoctors(), time);
	}

	private static String normalizeFilter(String value) {
		if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) return null;
		return value.trim();
	}

	private static LocalTime parseSlotStart(String slot) {
		if (slot == null || slot.isBlank()) return null;
		try {
			return LocalTime.parse(slot.split("-", 2)[0].trim());
		} catch (RuntimeException error) {
			return null;
		}
	}

// 1. **Add @Service Annotation**:
//    - This class should be annotated with `@Service` to indicate that it is a service layer class.
//    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
//    - Instruction: Add `@Service` above the class declaration.

// 2. **Constructor Injection for Dependencies**:
//    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
//    - These dependencies should be injected via the constructor for proper dependency management.
//    - Instruction: Ensure constructor injection is used for injecting dependencies into the service.

// 3. **Add @Transactional Annotation for Methods that Modify or Fetch Database Data**:
//    - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`, `filterDoctorsBy*` should be annotated with `@Transactional`.
//    - The `@Transactional` annotation ensures that database operations are consistent and wrapped in a single transaction.
//    - Instruction: Add the `@Transactional` annotation above the methods that perform database operations or queries.

// 4. **getDoctorAvailability Method**:
//    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
//    - The method fetches all appointments for the doctor on the given date and calculates the availability by comparing against booked slots.
//    - Instruction: Ensure that the time slots are properly formatted and the available slots are correctly filtered.

// 5. **saveDoctor Method**:
//    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
//    - If a doctor with the same email is found, it returns `-1` to indicate conflict; `1` for success, and `0` for internal errors.
//    - Instruction: Ensure that the method correctly handles conflicts and exceptions when saving a doctor.

// 6. **updateDoctor Method**:
//    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
//    - Instruction: Make sure that the doctor exists before attempting to save the updated record and handle any errors properly.

// 7. **getDoctors Method**:
//    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
//    - Instruction: Ensure that the collection is eagerly loaded, especially if dealing with lazy-loaded relationships (e.g., available times). 

// 8. **deleteDoctor Method**:
//    - Deletes a doctor from the system along with all appointments associated with that doctor.
//    - It first checks if the doctor exists. If not, it returns `-1`; otherwise, it deletes the doctor and their appointments.
//    - Instruction: Ensure the doctor and their appointments are deleted properly, with error handling for internal issues.

// 9. **validateDoctor Method**:
//    - Validates a doctor's login by checking if the email and password match an existing doctor record.
//    - It generates a token for the doctor if the login is successful, otherwise returns an error message.
//    - Instruction: Make sure to handle invalid login attempts and password mismatches properly with error responses.

// 10. **findDoctorByName Method**:
//    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
//    - This method is annotated with `@Transactional` to ensure that the database query and data retrieval are properly managed within a transaction.
//    - Instruction: Ensure that available times are eagerly loaded for the doctors.


// 11. **filterDoctorsByNameSpecilityandTime Method**:
//    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
//    - The method fetches doctors matching the name and specialty criteria, then filters them based on their availability during the specified time period.
//    - Instruction: Ensure proper filtering based on both the name and specialty as well as the specified time period.

// 12. **filterDoctorByTime Method**:
//    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
//    - This method processes a list of doctors and their available times to return those that fit the time criteria.
//    - Instruction: Ensure that the time filtering logic correctly handles both AM and PM time slots and edge cases.


// 13. **filterDoctorByNameAndTime Method**:
//    - Filters doctors based on their name and the specified time period (AM/PM).
//    - Fetches doctors based on partial name matching and filters the results to include only those available during the specified time period.
//    - Instruction: Ensure that the method correctly filters doctors based on the given name and time of day (AM/PM).

// 14. **filterDoctorByNameAndSpecility Method**:
//    - Filters doctors by name and specialty.
//    - It ensures that the resulting list of doctors matches both the name (case-insensitive) and the specified specialty.
//    - Instruction: Ensure that both name and specialty are considered when filtering doctors.


// 15. **filterDoctorByTimeAndSpecility Method**:
//    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
//    - Fetches doctors based on the specified specialty and filters them based on their available time slots for AM/PM.
//    - Instruction: Ensure the time filtering is accurately applied based on the given specialty and time period (AM/PM).

// 16. **filterDoctorBySpecility Method**:
//    - Filters doctors based on their specialty.
//    - This method fetches all doctors matching the specified specialty and returns them.
//    - Instruction: Make sure the filtering logic works for case-insensitive specialty matching.

// 17. **filterDoctorsByTime Method**:
//    - Filters all doctors based on their availability during a specific time period (AM/PM).
//    - The method checks all doctors' available times and returns those available during the specified time period.
//    - Instruction: Ensure proper filtering logic to handle AM/PM time periods.

   
}
