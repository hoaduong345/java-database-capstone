/*
  Import getAllAppointments to fetch appointments from the backend
  Import createPatientRow to generate a table row for each patient appointment


  Get the table body where patient rows will be added
  Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  Get the saved token from localStorage (used for authenticated API calls)
  Initialize patientName to null (used for filtering by name)


  Add an 'input' event listener to the search bar
  On each keystroke:
    - Trim and check the input value
    - If not empty, use it as the patientName for filtering
    - Else, reset patientName to "null" (as expected by backend)
    - Reload the appointments list with the updated filter


  Add a click listener to the "Today" button
  When clicked:
    - Set selectedDate to today's date
    - Update the date picker UI to match
    - Reload the appointments for today


  Add a change event listener to the date picker
  When the date changes:
    - Update selectedDate with the new value
    - Reload the appointments for that specific date


  Function: loadAppointments
  Purpose: Fetch and display appointments based on selected date and optional patient name

  Step 1: Call getAllAppointments with selectedDate, patientName, and token
  Step 2: Clear the table body content before rendering new rows

  Step 3: If no appointments are returned:
    - Display a message row: "No Appointments found for today."

  Step 4: If appointments exist:
    - Loop through each appointment and construct a 'patient' object with id, name, phone, and email
    - Call createPatientRow to generate a table row for the appointment
    - Append each row to the table body

  Step 5: Catch and handle any errors during fetch:
    - Show a message row: "Error loading appointments. Try again later."


  When the page is fully loaded (DOMContentLoaded):
    - Call renderContent() (assumes it sets up the UI layout)
    - Call loadAppointments() to display today's appointments by default
*/

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const patientTableBody = document.getElementById("patientTableBody");
const datePicker = document.getElementById("datePicker");
const searchBar = document.getElementById("searchBar");
const todayButton = document.getElementById("todayButton");
const token = localStorage.getItem("token");

let selectedDate = getLocalDateString();
let patientName = null;

function initializeDoctorDashboard() {
  if (!patientTableBody) return;

  if (datePicker) datePicker.value = selectedDate;
  searchBar?.addEventListener("input", () => {
    patientName = searchBar.value.trim() || null;
    loadAppointments();
  });
  todayButton?.addEventListener("click", () => {
    selectedDate = getLocalDateString();
    if (datePicker) datePicker.value = selectedDate;
    loadAppointments();
  });
  datePicker?.addEventListener("change", () => {
    if (!datePicker.value) return;
    selectedDate = datePicker.value;
    loadAppointments();
  });

  loadAppointments();
}

async function loadAppointments() {
  if (!patientTableBody) return;
  if (!token) {
    renderMessage("Your session has expired. Please sign in again.");
    return;
  }

  patientTableBody.setAttribute("aria-busy", "true");
  try {
    const response = await getAllAppointments(
      encodeURIComponent(selectedDate),
      encodeURIComponent(patientName || "null"),
      encodeURIComponent(token),
    );
    const appointments = Array.isArray(response)
      ? response
      : Array.isArray(response.appointments)
        ? response.appointments
        : [];

    patientTableBody.replaceChildren();
    if (!appointments.length) {
      renderMessage("No appointments found for this date.");
      return;
    }

    appointments.forEach((appointment) => {
      const patient = appointment.patient || {
        id: appointment.patientId,
        name: appointment.patientName,
        phone: appointment.patientPhone,
        email: appointment.patientEmail,
      };
      const appointmentId = appointment.id ?? appointment.appointmentId;
      const doctorId = appointment.doctorId ?? appointment.doctor?.id;
      patientTableBody.append(
        createPatientRow(patient, appointmentId, doctorId),
      );
    });
  } catch (error) {
    console.error("Failed to load appointments:", error);
    renderMessage("Error loading appointments. Try again later.");
  } finally {
    patientTableBody.removeAttribute("aria-busy");
  }
}

function renderMessage(message) {
  patientTableBody.replaceChildren();
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.className = "noPatientRecord";
  cell.colSpan = 5;
  cell.textContent = message;
  row.append(cell);
  patientTableBody.append(row);
}

function getLocalDateString() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializeDoctorDashboard, {
    once: true,
  });
} else {
  initializeDoctorDashboard();
}
