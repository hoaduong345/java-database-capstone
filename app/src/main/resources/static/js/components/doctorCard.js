/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/

import { showBookingOverlay } from "../loggedPatient.js";
import { getPatientData } from "../services/patientServices.js";
import { deleteDoctor } from "../services/doctorServices.js";

export function createDoctorCard(doctor) {
  const card = document.createElement("article");
  card.className = "doctor-card";

  const info = document.createElement("div");
  info.className = "doctor-info";

  const name = document.createElement("h3");
  name.textContent = doctor.name || "Doctor";

  const specialty = document.createElement("p");
  specialty.textContent = `Specialty: ${doctor.specialty || doctor.specialization || doctor.speciality || "Not listed"}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${doctor.email || "Not listed"}`;

  const availability = document.createElement("p");
  const availableTimes = Array.isArray(doctor.availableTimes)
    ? doctor.availableTimes
    : [];
  availability.textContent = availableTimes.length
    ? `Available: ${availableTimes.join(", ")}`
    : "Availability not listed";

  info.append(name, specialty, email, availability);
  card.append(info);

  const role = localStorage.getItem("userRole");
  const actions = document.createElement("div");
  actions.className = "card-actions";

  if (role === "admin") {
    const removeButton = createActionButton("Delete doctor");
    removeButton.addEventListener("click", async () => {
      if (
        !doctor.id ||
        !window.confirm(`Delete ${doctor.name || "this doctor"}?`)
      )
        return;

      const token = localStorage.getItem("token");
      if (!token) {
        window.alert("Your session has expired. Please sign in again.");
        window.location.assign("/");
        return;
      }

      removeButton.disabled = true;
      const result = await deleteDoctor(doctor.id, token);
      if (result.success) {
        card.remove();
        window.alert(result.message || "Doctor deleted successfully.");
      } else {
        removeButton.disabled = false;
        window.alert(result.message || "Could not delete this doctor.");
      }
    });
    actions.append(removeButton);
  } else if (role === "patient") {
    const bookButton = createActionButton("Book appointment");
    bookButton.addEventListener("click", () => {
      window.alert("Please log in before booking an appointment.");
    });
    actions.append(bookButton);
  } else if (role === "loggedPatient") {
    const bookButton = createActionButton("Book appointment");
    bookButton.addEventListener("click", async (event) => {
      const token = localStorage.getItem("token");
      if (!token) {
        window.alert("Your session has expired. Please log in again.");
        window.location.assign("/pages/patientDashboard.html");
        return;
      }

      bookButton.disabled = true;
      const patient = await getPatientData(token);
      bookButton.disabled = false;

      if (!patient) {
        window.alert(
          "Unable to load your patient profile. Please sign in again.",
        );
        return;
      }

      showBookingOverlay(event, doctor, patient);
    });
    actions.append(bookButton);
  }

  if (actions.childElementCount > 0) card.append(actions);
  return card;
}

function createActionButton(label) {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = label;
  return button;
}
