# User Stories

## 🛠️ Admin User Stories

### User Story 1

**Title:**
> _As an **Admin**, I want to **create, edit, and deactivate user accounts**, so that **only authorized personnel and valid users have system access**._

**Acceptance Criteria:**
1. Admin can search and select any user profile (Doctor or Patient) to edit their details or status.
2. Deactivating a Doctor account automatically flags their upcoming unfulfilled appointments for rescheduling.
3. System logs the timestamp and Admin ID whenever an account role or status is modified.

**Priority:** High  
**Story Points:** 5

**Notes:**
- Hard deletion of user accounts is disabled to preserve historical medical data integrity.

---

### User Story 2

**Title:**
> _As an **Admin**, I want to **manage doctor profiles and their medical specializations**, so that **patients can search for accurate clinic directory information**._

**Acceptance Criteria:**
1. Admin can add new specializations or update existing ones in the system catalog.
2. Admin can assign one or more specializations and licensing credentials to a Doctor’s profile.
3. Profile changes reflect immediately on the patient-facing search modules.

**Priority:** High  
**Story Points:** 3

**Notes:**
- Validations should prevent saving a profile without a valid medical license registration number.

---

### User Story 3

**Title:**
> _As an **Admin**, I want to **view system audit logs and administrative actions**, so that **I can track platform security, user activity, and system integrity**._

**Acceptance Criteria:**
1. Admin can filter audit logs by date range, action type, user role, or target module.
2. System records login attempts, role changes, data modifications, and failed authorization events.
3. Export functionality allows downloading filtered log reports in CSV or JSON format.

**Priority:** Medium  
**Story Points:** 5

**Notes:**
- Ensure audit logs are read-only and stored securely to prevent tampering.

---

### User Story 4

**Title:**
> _As an **Admin**, I want to **configure global appointment booking parameters**, so that **the system enforces standard clinic operating rules**._

**Acceptance Criteria:**
1. Admin can define global rules such as maximum advance booking days and minimum cancellation notice windows.
2. Admin can set default consultation duration slots (e.g., 15 min, 30 min, 60 min).
3. Changes apply to all future appointment bookings while leaving confirmed bookings unaffected.

**Priority:** Medium  
**Story Points:** 3

**Notes:**
- **Edge case:** Handle conflicts when global operating hours are shortened while appointments are already booked.

---

### User Story 5

**Title:**
> _As an **Admin**, I want to **monitor system usage metrics and active user sessions**, so that **I can ensure server health and resource allocation**._

**Acceptance Criteria:**
1. Dashboard displays real-time metrics for active sessions, total daily bookings, and API error rates.
2. Admin receives alerts if database connection pools or API response latency exceed thresholds.
3. Admin has the ability to terminate active sessions manually in emergency situations.

**Priority:** Low  
**Story Points:** 5

**Notes:**
- Integrate with Spring Boot Actuator endpoints for system metrics monitoring.

---

## 🩺 Doctor User Stories

### User Story 6

**Title:**
> _As a **Doctor**, I want to **set and update my recurring weekly availability**, so that **patients can only schedule appointments during my working hours**._

**Acceptance Criteria:**
1. Doctor can define weekly recurring shift slots (e.g., Mon–Fri, 09:00–17:00) with custom slot durations.
2. Doctor can add break times (e.g., lunch breaks) within their active working window.
3. Real-time availability updates are published to the patient booking calendar instantly.

**Priority:** High  
**Story Points:** 5

**Notes:**
- Slot generation logic must prevent overlapping time blocks.

---

### User Story 7

**Title:**
> _As a **Doctor**, I want to **view my daily and weekly appointment schedule**, so that **I can prepare for upcoming patient consultations efficiently**._

**Acceptance Criteria:**
1. Doctor can view appointments in daily list view or weekly calendar grid view.
2. Clicking an appointment displays key patient info, contact details, and chief complaint.
3. System marks past, ongoing, and upcoming appointments with clear visual indicators.

**Priority:** High  
**Story Points:** 3

**Notes:**
- UI rendered via Thymeleaf dashboard for fast internal workflow access.

---

### User Story 8

**Title:**
> _As a **Doctor**, I want to **issue digital prescriptions and store them in the patient's record**, so that **treatment plans are securely documented and accessible**._

**Acceptance Criteria:**
1. Doctor can select medication, dosage, frequency, duration, and special instructions.
2. Submitted prescriptions are persisted into MongoDB as structured document records linked to the patient ID.
3. Doctor can preview and finalize the digital prescription before signing off.

**Priority:** High  
**Story Points:** 8

**Notes:**
- MongoDB document structure allows variable numbers of prescribed items per document without rigid relational schemas.

---

### User Story 9

**Title:**
> _As a **Doctor**, I want to **access a patient's historical medical records and past prescriptions**, so that **I can make informed diagnostic decisions**._

**Acceptance Criteria:**
1. Doctor can search and open a patient's timeline showing past consultations and diagnoses.
2. Doctor can view previous MongoDB prescription records in chronological order.
3. Patient medical history access is restricted to doctors who have or had active appointments with that patient.

**Priority:** High  
**Story Points:** 5

**Notes:**
- Enforce strict role-based access control (RBAC) to comply with data privacy policies.

---

### User Story 10

**Title:**
> _As a **Doctor**, I want to **block out temporary leave or emergency time off**, so that **no new appointments can be booked during my absence**._

**Acceptance Criteria:**
1. Doctor can select a single date or date range to mark as "Unavailable".
2. System prevents new bookings on blocked-out dates.
3. If existing appointments conflict with the blocked period, the system prompts the doctor to trigger automated cancellation/rescheduling notifications.

**Priority:** Medium  
**Story Points:** 3

**Notes:**
- Needs handling for mass-sending notifications to affected patients via API events.

---

## 👤 Patient User Stories

### User Story 11

**Title:**
> _As a **Patient**, I want to **search for doctors by specialty and view their available time slots**, so that **I can select a suitable time for my consultation**._

**Acceptance Criteria:**
1. Patient can filter doctors by specialization, rating, or name.
2. Selecting a doctor presents an interactive calendar of open slots fetched from the backend.
3. Only currently unreserved slots within the doctor's set availability are selectable.

**Priority:** High  
**Story Points:** 5

**Notes:**
- API returns open slots formatted as JSON for mobile and web frontend consumers.

---

### User Story 12

**Title:**
> _As a **Patient**, I want to **book an appointment with a chosen doctor**, so that **I can secure my consultation slot**._

**Acceptance Criteria:**
1. Patient confirms slot choice, submits reason for visit, and completes booking request.
2. System creates a pending/confirmed appointment record in MySQL using Spring Data JPA.
3. Patient receives an immediate booking confirmation response and email/SMS notification.

**Priority:** High  
**Story Points:** 5

**Notes:**
- Implement optimistic locking in MySQL to handle concurrent booking attempts on the same slot.

---

### User Story 13

**Title:**
> _As a **Patient**, I want to **reschedule or cancel an upcoming appointment**, so that **I can manage changes in my personal schedule**._

**Acceptance Criteria:**
1. Patient can view active bookings and select "Reschedule" or "Cancel".
2. Cancellations release the reserved slot back to the doctor's available inventory immediately.
3. Rescheduling allows picking a new available slot without needing to cancel and re-create from scratch.

**Priority:** High  
**Story Points:** 5

**Notes:**
- Check cancellation window policies (e.g., must cancel at least 2 hours prior).

---

### User Story 14

**Title:**
> _As a **Patient**, I want to **view and download my digital prescriptions**, so that **I can easily purchase prescribed medications**._

**Acceptance Criteria:**
1. Patient can navigate to "My Prescriptions" to see a list of issued documents.
2. Clicking a prescription opens details retrieved directly from the MongoDB collection.
3. Patient can export the prescription as a downloadable PDF file.

**Priority:** Medium  
**Story Points:** 5

**Notes:**
- REST API endpoint fetches prescription document from MongoDB and formats payload for rendering.

---

### User Story 15

**Title:**
> _As a **Patient**, I want to **view my past consultation history and doctor notes**, so that **I can track my personal healthcare history over time**._

**Acceptance Criteria:**
1. Patient dashboard displays a timeline of all completed, canceled, and missed appointments.
2. Patient can view doctor's public notes and recommendations for completed consultations.
3. List supports pagination and filtering by date range or doctor name.

**Priority:** Medium  
**Story Points:** 3

**Notes:**
- Ensure sensitive internal doctor notes (private clinical notes) are stripped out before returning payload to patient client.