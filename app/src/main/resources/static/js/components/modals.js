// modals.js
export function openModal(type) {
  const modal = document.getElementById("modal");
  const modalBody = document.getElementById("modal-body");
  if (!modal || !modalBody) return false;

  let modalContent = "";
  if (type === "addDoctor") {
    modalContent = `
         <h2>Add Doctor</h2>
         <input type="text" id="doctorName" placeholder="Doctor Name" class="input-field">
         <select id="specialization" class="input-field select-dropdown">
             <option value="">Specialization</option>
                        <option value="cardiologist">Cardiologist</option>
                        <option value="dermatologist">Dermatologist</option>
                        <option value="neurologist">Neurologist</option>
                        <option value="pediatrician">Pediatrician</option>
                        <option value="orthopedic">Orthopedic</option>
                        <option value="gynecologist">Gynecologist</option>
                        <option value="psychiatrist">Psychiatrist</option>
                        <option value="dentist">Dentist</option>
                        <option value="ophthalmologist">Ophthalmologist</option>
                        <option value="ent">ENT Specialist</option>
                        <option value="urologist">Urologist</option>
                        <option value="oncologist">Oncologist</option>
                        <option value="gastroenterologist">Gastroenterologist</option>
                        <option value="general">General Physician</option>

        </select>
        <input type="email" id="doctorEmail" placeholder="Email" class="input-field">
        <input type="password" id="doctorPassword" placeholder="Password" class="input-field">
        <input type="text" id="doctorPhone" placeholder="Mobile No." class="input-field">
        <div class="availability-container">
        <label class="availabilityLabel">Select Availability:</label>
          <div class="checkbox-group">
              <label><input type="checkbox" name="availability" value="09:00-10:00"> 9:00 AM - 10:00 AM</label>
              <label><input type="checkbox" name="availability" value="10:00-11:00"> 10:00 AM - 11:00 AM</label>
              <label><input type="checkbox" name="availability" value="11:00-12:00"> 11:00 AM - 12:00 PM</label>
              <label><input type="checkbox" name="availability" value="12:00-13:00"> 12:00 PM - 1:00 PM</label>
          </div>
        </div>
        <button class="dashboard-btn" id="saveDoctorBtn">Save</button>
      `;
  } else if (type === "patientLogin") {
    modalContent = `
        <h2>Patient Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="loginBtn">Login</button>
      `;
  } else if (type === "patientSignup") {
    modalContent = `
      <h2>Patient Signup</h2>
      <input type="text" id="name" placeholder="Name" class="input-field">
      <input type="email" id="email" placeholder="Email" class="input-field">
      <input type="password" id="password" placeholder="Password" class="input-field">
      <input type="text" id="phone" placeholder="Phone" class="input-field">
      <input type="text" id="address" placeholder="Address" class="input-field">
      <button class="dashboard-btn" id="signupBtn">Signup</button>
    `;
  } else if (type === "adminLogin") {
    modalContent = `
        <h2>Admin Login</h2>
        <input type="text" id="username" name="username" placeholder="Username" class="input-field">
        <input type="password" id="password" name="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="adminLoginBtn" >Login</button>
      `;
  } else if (type === "doctorLogin") {
    modalContent = `
        <h2>Doctor Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="doctorLoginBtn" >Login</button>
      `;
  }

  if (!modalContent) return false;

  modalBody.innerHTML = modalContent;
  modalBody.querySelector("h2")?.setAttribute("id", "modal-title");
  modal.style.display = "block";

  const closeButton = document.getElementById("closeModal");
  if (closeButton) {
    closeButton.onclick = () => {
      modal.style.display = "none";
    };
  }

  const handlers = {
    patientSignup: ["signupBtn", window.signupPatient],
    patientLogin: ["loginBtn", window.loginPatient],
    addDoctor: ["saveDoctorBtn", window.adminAddDoctor],
    adminLogin: ["adminLoginBtn", window.adminLoginHandler],
    doctorLogin: ["doctorLoginBtn", window.doctorLoginHandler],
  };

  const [buttonId, handler] = handlers[type];
  const actionButton = document.getElementById(buttonId);
  if (actionButton && typeof handler === "function") {
    actionButton.addEventListener("click", handler);
  } else if (actionButton) {
    actionButton.disabled = true;
    actionButton.title = "This action is not available yet.";
  }

  return true;
}
