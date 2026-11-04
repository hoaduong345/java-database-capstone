package com.project.back_end.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patient")
public class PatientController {

	private final PatientService patientService;
	private final Service service;

	public PatientController(PatientService patientService, Service service) {
		this.patientService = patientService;
		this.service = service;
	}

	@GetMapping("/{token}")
	public ResponseEntity<Map<String, Object>> getPatient(@PathVariable("token") String token) {
		if (!service.validateToken(token, "patient")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired patient token.");
		Patient patient = patientService.getPatientDetails(token);
		if (patient == null) return response(HttpStatus.NOT_FOUND, "Patient account was not found.");
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("patient", patient);
		return ResponseEntity.ok(body);
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createPatient(@Valid @RequestBody Patient patient) {
		if (!service.validatePatient(patient)) return response(HttpStatus.CONFLICT, "Email or phone number is already registered.");
		if (patientService.createPatient(patient) == 1) return response(HttpStatus.CREATED, "Account created successfully.");
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create patient account.");
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody Login login) {
		return service.validatePatientLogin(login);
	}

	@GetMapping("/{patientId}/{user}/{token}")
	public ResponseEntity<Map<String, Object>> getPatientAppointment(
		@PathVariable("patientId") Long patientId,
		@PathVariable("user") String user,
		@PathVariable("token") String token
	) {
		if (!user.equals("patient") && !user.equals("doctor")) {
			return response(HttpStatus.FORBIDDEN, "Unsupported user role.");
		}
		if (!service.validateToken(token, user)) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
		if (user.equals("patient")) {
			Patient authenticatedPatient = patientService.getPatientDetails(token);
			if (authenticatedPatient == null || !patientId.equals(authenticatedPatient.getId())) {
				return response(HttpStatus.FORBIDDEN, "You can only view your own appointments.");
			}
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appointments", patientService.getPatientAppointment(patientId));
		return ResponseEntity.ok(body);
	}

	@GetMapping("/filter/{condition}/{name}/{token}")
	public ResponseEntity<Map<String, Object>> filterPatientAppointment(
		@PathVariable("condition") String condition,
		@PathVariable("name") String name,
		@PathVariable("token") String token
	) {
		if (!service.validateToken(token, "patient")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired patient token.");
		try {
			return ResponseEntity.ok(service.filterPatient(condition, name, token));
		} catch (IllegalArgumentException error) {
			return response(HttpStatus.BAD_REQUEST, "Unsupported appointment filter.");
		}
	}

	private static ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.



}


