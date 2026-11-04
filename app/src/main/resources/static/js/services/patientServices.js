import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = `${API_BASE_URL}/patient`;

async function readResponse(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

function encodePathValue(value) {
  return encodeURIComponent(
    value == null || value === "" ? "null" : String(value),
  );
}

export async function patientSignup(patient) {
  try {
    // Send the registration data and normalize the server response.
    const response = await fetch(PATIENT_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patient),
    });
    const data = await readResponse(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Account created." : "Could not create account."),
    };
  } catch (error) {
    console.error("Patient signup failed:", error);
    return { success: false, message: "Network error. Please try again." };
  }
}

export async function patientLogin(credentials) {
  try {
    // Authenticate the patient and return the token without exposing Response objects to the UI.
    const response = await fetch(`${PATIENT_API}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });
    const data = await readResponse(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Signed in successfully." : "Invalid credentials."),
      token: data.token || null,
    };
  } catch (error) {
    console.error("Patient login failed:", error);
    return {
      success: false,
      message: "Network error. Please try again.",
      token: null,
    };
  }
}

export async function getPatientData(token) {
  try {
    // Fetch the profile associated with the current patient token.
    const response = await fetch(`${PATIENT_API}/${encodePathValue(token)}`);
    const data = await readResponse(response);

    return {
      success: response.ok && Boolean(data.patient),
      message:
        data.message ||
        (response.ok
          ? "Patient profile loaded."
          : "Could not load patient profile."),
      patient: data.patient || null,
    };
  } catch (error) {
    console.error("Failed to fetch patient profile:", error);
    return {
      success: false,
      message: "Network error. Please try again.",
      patient: null,
    };
  }
}

export async function getPatientAppointments(id, token, user) {
  try {
    // Request appointments for the requested patient and role.
    const path = [id, user, token].map(encodePathValue).join("/");
    const response = await fetch(`${PATIENT_API}/${path}`);
    const data = await readResponse(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Appointments loaded." : "Could not load appointments."),
      appointments: Array.isArray(data.appointments) ? data.appointments : [],
    };
  } catch (error) {
    console.error("Failed to fetch patient appointments:", error);
    return {
      success: false,
      message: "Network error. Please try again.",
      appointments: [],
    };
  }
}

export async function filterAppointments(condition, name, token) {
  try {
    // Apply optional filters while preserving the API's path-parameter contract.
    const path = [condition, name, token].map(encodePathValue).join("/");
    const response = await fetch(`${PATIENT_API}/filter/${path}`);
    const data = await readResponse(response);

    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok
          ? "Appointments loaded."
          : "Could not filter appointments."),
      appointments: Array.isArray(data.appointments) ? data.appointments : [],
    };
  } catch (error) {
    console.error("Failed to filter patient appointments:", error);
    return {
      success: false,
      message: "Network error. Please try again.",
      appointments: [],
    };
  }
}
