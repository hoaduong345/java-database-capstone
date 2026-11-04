/*
  Import the openModal function to handle showing login popups/modals
  Import the base API URL from the config file
  Define constants for the admin and doctor login API endpoints using the base URL

  Use the window.onload event to ensure DOM elements are available after page load
  Inside this function:
    - Select the "adminLogin" and "doctorLogin" buttons using getElementById
    - If the admin login button exists:
        - Add a click event listener that calls openModal('adminLogin') to show the admin login modal
    - If the doctor login button exists:
        - Add a click event listener that calls openModal('doctorLogin') to show the doctor login modal


  Define a function named adminLoginHandler on the global window object
  This function will be triggered when the admin submits their login credentials

  Step 1: Get the entered username and password from the input fields
  Step 2: Create an admin object with these credentials

  Step 3: Use fetch() to send a POST request to the ADMIN_API endpoint
    - Set method to POST
    - Add headers with 'Content-Type: application/json'
    - Convert the admin object to JSON and send in the body

  Step 4: If the response is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('admin') to proceed with admin-specific behavior

  Step 5: If login fails or credentials are invalid:
    - Show an alert with an error message

  Step 6: Wrap everything in a try-catch to handle network or server errors
    - Show a generic error message if something goes wrong


  Define a function named doctorLoginHandler on the global window object
  This function will be triggered when a doctor submits their login credentials

  Step 1: Get the entered email and password from the input fields
  Step 2: Create a doctor object with these credentials

  Step 3: Use fetch() to send a POST request to the DOCTOR_API endpoint
    - Include headers and request body similar to admin login

  Step 4: If login is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('doctor') to proceed with doctor-specific behavior

  Step 5: If login fails:
    - Show an alert for invalid credentials

  Step 6: Wrap in a try-catch block to handle errors gracefully
    - Log the error to the console
    - Show a generic error message
*/

import { API_BASE_URL } from "../config/config.js";
import { openModal } from "../components/modals.js";

const ADMIN_API = `${API_BASE_URL}/admin/login`;
const DOCTOR_API = `${API_BASE_URL}/doctor/login`;

async function readResponse(response) {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

function initializeRoleLoginButtons() {
  document
    .getElementById("adminLogin")
    ?.addEventListener("click", () => openModal("adminLogin"));
  document
    .getElementById("doctorLogin")
    ?.addEventListener("click", () => openModal("doctorLogin"));
}

async function submitLogin(endpoint, credentials, role, buttonId) {
  const submitButton = document.getElementById(buttonId);
  if (!credentials.username && role === "admin") {
    window.alert("Enter your username.");
    return;
  }
  if (!credentials.email && role === "doctor") {
    window.alert("Enter your email.");
    return;
  }
  if (!credentials.password) {
    window.alert("Enter your password.");
    return;
  }

  if (submitButton) submitButton.disabled = true;

  try {
    // Send credentials to the role-specific endpoint and validate its response.
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });
    const data = await readResponse(response);

    if (!response.ok || !data.token) {
      window.alert(data.message || "Invalid credentials.");
      return;
    }

    localStorage.setItem("token", data.token);
    localStorage.setItem("userRole", role);
    if (typeof window.selectRole === "function") {
      window.selectRole(role);
    } else {
      const destination =
        role === "admin" ? "adminDashboard" : "doctorDashboard";
      window.location.assign(
        `/${destination}/${encodeURIComponent(data.token)}`,
      );
    }
  } catch (error) {
    console.error(`${role} login failed:`, error);
    window.alert("Unable to sign in right now. Please try again.");
  } finally {
    if (submitButton?.isConnected) submitButton.disabled = false;
  }
}

window.adminLoginHandler = async function adminLoginHandler() {
  const username = document.getElementById("username")?.value.trim() || "";
  const password = document.getElementById("password")?.value || "";
  await submitLogin(
    ADMIN_API,
    { username, password },
    "admin",
    "adminLoginBtn",
  );
};

window.doctorLoginHandler = async function doctorLoginHandler() {
  const email = document.getElementById("email")?.value.trim() || "";
  const password = document.getElementById("password")?.value || "";
  await submitLogin(
    DOCTOR_API,
    { email, password },
    "doctor",
    "doctorLoginBtn",
  );
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeRoleLoginButtons, {
    once: true,
  });
} else {
  initializeRoleLoginButtons();
}
