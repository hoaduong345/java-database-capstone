package com.project.back_end.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.PatientRepository;

@org.springframework.stereotype.Service
public class Service {
	private final TokenService tokenService;
	private final AdminRepository adminRepository;
	private final PatientRepository patientRepository;
	private final DoctorService doctorService;
	private final PatientService patientService;
	private final AppointmentService appointmentService;

	public Service(
		TokenService tokenService,
		AdminRepository adminRepository,
		PatientRepository patientRepository,
		DoctorService doctorService,
		PatientService patientService,
		AppointmentService appointmentService
	) {
		this.tokenService = tokenService;
		this.adminRepository = adminRepository;
		this.patientRepository = patientRepository;
		this.doctorService = doctorService;
		this.patientService = patientService;
		this.appointmentService = appointmentService;
	}

	public boolean validateToken(String token, String userType) {
		return tokenService.validateToken(token, userType);
	}

	public ResponseEntity<Map<String, Object>> validateAdmin(Admin credentials) {
		// Verify the admin username/password pair and issue an authenticated token.
		if (credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
			return response(HttpStatus.BAD_REQUEST, "Username and password are required.");
		}
		try {
			Admin admin = adminRepository.findByUsername(credentials.getUsername());
			if (admin == null || !credentials.getPassword().equals(admin.getPassword())) {
				return response(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
			}
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("message", "Login successful.");
			body.put("token", tokenService.generateToken(admin.getUsername()));
			return ResponseEntity.ok(body);
		} catch (RuntimeException error) {
			return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign in right now.");
		}
	}

	public List<Doctor> filterDoctor(String name, String time, String specialty) {
		// Delegate the combined doctor filters to the doctor domain service.
		return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
	}

	public int validateAppointment(Appointment appointment) {
		// Delegate availability and overlap checks to the appointment service.
		return appointmentService.validateAppointment(appointment);
	}

	public boolean validatePatient(Patient patient) {
		// New patient records must not duplicate an existing email address or phone number.
		if (patient == null || patient.getEmail() == null || patient.getPhone() == null) return false;
		return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
	}

	public ResponseEntity<Map<String, Object>> validatePatientLogin(Login credentials) {
		// Verify patient credentials and return a JWT whose subject is the patient's email.
		if (credentials == null || credentials.getEmail() == null || credentials.getPassword() == null) {
			return response(HttpStatus.BAD_REQUEST, "Email and password are required.");
		}
		try {
			Patient patient = patientRepository.findByEmail(credentials.getEmail());
			if (patient == null || !credentials.getPassword().equals(patient.getPassword())) {
				return response(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
			}
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("message", "Login successful.");
			body.put("token", tokenService.generateToken(patient.getEmail()));
			return ResponseEntity.ok(body);
		} catch (RuntimeException error) {
			return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign in right now.");
		}
	}

	public Map<String, Object> filterPatient(String condition, String name, String token) {
		// Resolve the patient from the token before returning any appointment data.
		Map<String, Object> body = new LinkedHashMap<>();
		if (!validateToken(token, "patient")) {
			body.put("message", "Invalid or expired patient token.");
			body.put("appointments", List.of());
			return body;
		}

		String email = tokenService.extractEmail(token);
		Patient patient = email == null ? null : patientRepository.findByEmail(email);
		if (patient == null) {
			body.put("message", "Patient account was not found.");
			body.put("appointments", List.of());
			return body;
		}

		List<AppointmentDTO> appointments;
		boolean hasName = name != null && !name.isBlank() && !name.equalsIgnoreCase("null");
		boolean hasCondition = condition != null && !condition.isBlank() && !condition.equalsIgnoreCase("null")
			&& !condition.equalsIgnoreCase("allAppointments");
		if (hasName && hasCondition) {
			appointments = patientService.filterByDoctorAndCondition(patient.getId(), name, condition);
		} else if (hasName) {
			appointments = patientService.filterByDoctor(patient.getId(), name);
		} else {
			appointments = patientService.filterByCondition(patient.getId(), condition);
		}
		body.put("appointments", appointments);
		return body;
	}

	private static ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}

// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.


}
