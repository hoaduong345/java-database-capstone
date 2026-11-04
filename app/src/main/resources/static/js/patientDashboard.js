// patientDashboard.js
import { getDoctors } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";
import { filterDoctors } from "./services/doctorServices.js";
import { patientSignup, patientLogin } from "./services/patientServices.js";

function initializePatientDashboard() {
  loadDoctorCards();
  document
    .getElementById("searchBar")
    ?.addEventListener("input", filterDoctorsOnChange);
  document
    .getElementById("filterTime")
    ?.addEventListener("change", filterDoctorsOnChange);
  document
    .getElementById("filterSpecialty")
    ?.addEventListener("change", filterDoctorsOnChange);
}

async function loadDoctorCards() {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  contentDiv.setAttribute("aria-busy", "true");
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Failed to load doctors:", error);
    contentDiv.textContent = "Unable to load doctors right now.";
  } finally {
    contentDiv.removeAttribute("aria-busy");
  }
}

async function filterDoctorsOnChange() {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  const name = document.getElementById("searchBar")?.value.trim() || null;
  const time = document.getElementById("filterTime")?.value || null;
  const specialty = document.getElementById("filterSpecialty")?.value || null;

  contentDiv.setAttribute("aria-busy", "true");
  try {
    const response = await filterDoctors(name, time, specialty);
    renderDoctorCards(response.doctors);
  } catch (error) {
    console.error("Failed to filter doctors:", error);
    contentDiv.textContent = "Unable to filter doctors right now.";
  } finally {
    contentDiv.removeAttribute("aria-busy");
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  contentDiv.replaceChildren();
  if (!doctors.length) {
    const emptyMessage = document.createElement("p");
    emptyMessage.textContent = "No doctors found with the selected filters.";
    contentDiv.append(emptyMessage);
    return;
  }

  doctors.forEach((doctor) => contentDiv.append(createDoctorCard(doctor)));
}

window.signupPatient = async function () {
  try {
    const name = document.getElementById("name").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const phone = document.getElementById("phone").value;
    const address = document.getElementById("address").value;

    const data = { name, email, password, phone, address };
    const { success, message } = await patientSignup(data);
    if (success) {
      alert(message);
      document.getElementById("modal").style.display = "none";
      window.location.reload();
    } else alert(message);
  } catch (error) {
    console.error("Signup failed:", error);
    alert("❌ An error occurred while signing up.");
  }
};

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initializePatientDashboard, {
    once: true,
  });
} else {
  initializePatientDashboard();
}

window.loginPatient = async function () {
  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const data = {
      email,
      password,
    };
    const result = await patientLogin(data);
    if (result.success && result.token) {
      localStorage.setItem("token", result.token);
      selectRole("loggedPatient");
      window.location.href = "/pages/loggedPatientDashboard.html";
    } else {
      alert(result.message || "Invalid credentials.");
    }
  } catch (error) {
    alert("Failed to log in. Please try again.");
    console.error("Patient login failed:", error);
  }
};
