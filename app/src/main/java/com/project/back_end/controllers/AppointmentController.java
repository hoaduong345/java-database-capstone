package com.project.back_end.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

	private final AppointmentService appointmentService;
	private final Service service;

	public AppointmentController(AppointmentService appointmentService, Service service) {
		this.appointmentService = appointmentService;
		this.service = service;
	}

	@GetMapping("/{date}/{patientName}/{token}")
	public ResponseEntity<Map<String, Object>> getAppointments(
		@PathVariable("date") String date,
		@PathVariable("patientName") String patientName,
		@PathVariable("token") String token
	) {
		if (!service.validateToken(token, "doctor")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired doctor token.");
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("appointments", appointmentService.getAppointments(date, patientName, token));
			return ResponseEntity.ok(body);
		} catch (RuntimeException error) {
			return response(HttpStatus.BAD_REQUEST, "Invalid appointment date or request.");
		}
	}

	@PostMapping("/{token}")
	public ResponseEntity<Map<String, Object>> bookAppointment(
		@Valid @RequestBody Appointment appointment,
		@PathVariable("token") String token
	) {
		if (!service.validateToken(token, "patient")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired patient token.");
		int validation = service.validateAppointment(appointment);
		if (validation == -1) return response(HttpStatus.NOT_FOUND, "Doctor was not found.");
		if (validation == 0) return response(HttpStatus.CONFLICT, "The selected appointment time is unavailable.");

		int result = appointmentService.bookAppointment(appointment, token);
		if (result == 1) return response(HttpStatus.CREATED, "Appointment booked successfully.");
		if (result == -1) return response(HttpStatus.NOT_FOUND, "Doctor was not found.");
		return response(HttpStatus.CONFLICT, "Unable to book the selected appointment.");
	}

	@PutMapping("/{token}")
	public ResponseEntity<Map<String, Object>> updateAppointment(
		@Valid @RequestBody Appointment appointment,
		@PathVariable("token") String token
	) {
		if (!service.validateToken(token, "patient")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired patient token.");
		int result = appointmentService.updateAppointment(appointment, token);
		if (result == 1) return response(HttpStatus.OK, "Appointment updated successfully.");
		if (result == -1) return response(HttpStatus.NOT_FOUND, "Appointment or doctor was not found.");
		return response(HttpStatus.CONFLICT, "Unable to update the selected appointment.");
	}

	@DeleteMapping("/{appointmentId}/{token}")
	public ResponseEntity<Map<String, Object>> cancelAppointment(
		@PathVariable("appointmentId") Long appointmentId,
		@PathVariable("token") String token
	) {
		if (!service.validateToken(token, "patient")) return response(HttpStatus.UNAUTHORIZED, "Invalid or expired patient token.");
		int result = appointmentService.cancelAppointment(appointmentId, token);
		if (result == 1) return response(HttpStatus.OK, "Appointment cancelled successfully.");
		if (result == -1) return response(HttpStatus.NOT_FOUND, "Appointment was not found.");
		return response(HttpStatus.FORBIDDEN, "You cannot cancel this appointment.");
	}

	private static ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.


// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.


// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.


}
