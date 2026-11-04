/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/

import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = `${API_BASE_URL}/doctor`;

async function readResponse(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

export async function getDoctors() {
  try {
    const response = await fetch(DOCTOR_API);
    const data = await readResponse(response);
    if (!response.ok)
      throw new Error(data.message || "Failed to load doctors.");
    return Array.isArray(data.doctors) ? data.doctors : [];
  } catch (error) {
    console.error("Failed to fetch doctors:", error);
    return [];
  }
}

export async function deleteDoctor(doctorId, token) {
  try {
    const response = await fetch(
      `${DOCTOR_API}/${encodeURIComponent(doctorId)}/${encodeURIComponent(token)}`,
      {
        method: "DELETE",
      },
    );
    const data = await readResponse(response);
    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok
          ? "Doctor deleted successfully."
          : "Failed to delete doctor."),
    };
  } catch (error) {
    console.error("Failed to delete doctor:", error);
    return { success: false, message: "Network error. Please try again." };
  }
}

export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${encodeURIComponent(token)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });
    const data = await readResponse(response);
    return {
      success: response.ok,
      message:
        data.message ||
        (response.ok ? "Doctor saved successfully." : "Failed to save doctor."),
    };
  } catch (error) {
    console.error("Failed to save doctor:", error);
    return { success: false, message: "Network error. Please try again." };
  }
}

export async function filterDoctors(
  name = null,
  time = null,
  specialty = null,
) {
  const criteria = [name, time, specialty].map((value) =>
    encodeURIComponent(value == null || value === "" ? "null" : value),
  );

  try {
    const response = await fetch(`${DOCTOR_API}/filter/${criteria.join("/")}`);
    const data = await readResponse(response);
    if (!response.ok)
      throw new Error(data.message || "Failed to filter doctors.");
    return {
      ...data,
      doctors: Array.isArray(data.doctors) ? data.doctors : [],
    };
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    return { doctors: [] };
  }
}
