
# Smart Clinic Management System – Schema Design

## MySQL Database Design

MySQL stores the core, structured and interrelated operational data of the clinic (users, appointments, payments).

General conventions:
- Every table uses a surrogate `id` (BIGINT, AUTO_INCREMENT) as Primary Key.
- Passwords are never stored in plain text, only as a hash (BCrypt).
- Email / phone format validation is done in the application layer (Spring `@Email`, `@Pattern`), the DB only enforces `NOT NULL` / `UNIQUE`.
- Medical history is retained: patients and doctors are soft-deleted (`is_active = false`) instead of being physically deleted, so past appointments are never lost.

### Table: admin
id: BIGINT, Primary Key, Auto Increment

username: VARCHAR(50), Not Null, Unique

password_hash: VARCHAR(255), Not Null

email: VARCHAR(100), Not Null, Unique

created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

<!-- Admin is independent: no foreign keys. Admins manage doctors and system configuration. -->

### Table: clinic_locations
id: BIGINT, Primary Key, Auto Increment

name: VARCHAR(100), Not Null

address: VARCHAR(255), Not Null

phone: VARCHAR(20), Not Null

<!-- A location cannot be deleted while doctors are still assigned to it (ON DELETE RESTRICT). -->

### Table: doctors
id: BIGINT, Primary Key, Auto Increment

first_name: VARCHAR(50), Not Null

last_name: VARCHAR(50), Not Null

email: VARCHAR(100), Not Null, Unique

phone: VARCHAR(20), Not Null

password_hash: VARCHAR(255), Not Null

specialization: VARCHAR(100), Not Null

clinic_location_id: BIGINT, Foreign Key → clinic_locations(id), Not Null, ON DELETE RESTRICT

is_active: BOOLEAN, Not Null, Default TRUE

created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

<!-- Phone format (e.g. 10 digits) is validated in code, not in the DB. -->

### Table: doctor_available_slots
id: BIGINT, Primary Key, Auto Increment

doctor_id: BIGINT, Foreign Key → doctors(id), Not Null, ON DELETE CASCADE

day_of_week: TINYINT, Not Null (1 = Monday … 7 = Sunday)

start_time: TIME, Not Null

end_time: TIME, Not Null

Unique constraint: (doctor_id, day_of_week, start_time)

<!-- Yes, each doctor has their own available time slots. Slots are configuration data, so they
     are deleted together with the doctor (CASCADE). Rule: start_time < end_time (checked in code / CHECK constraint). -->

### Table: patients
id: BIGINT, Primary Key, Auto Increment

first_name: VARCHAR(50), Not Null

last_name: VARCHAR(50), Not Null

email: VARCHAR(100), Not Null, Unique

password_hash: VARCHAR(255), Not Null

phone: VARCHAR(20), Not Null

date_of_birth: DATE, Nullable

gender: VARCHAR(10), Nullable

address: VARCHAR(255), Nullable

is_active: BOOLEAN, Not Null, Default TRUE

created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

<!-- Patients are soft-deleted (is_active = false). Their appointment history is retained for
     medical/legal reasons, so appointments are NOT cascade-deleted. -->

### Table: appointments
id: BIGINT, Primary Key, Auto Increment

patient_id: BIGINT, Foreign Key → patients(id), Not Null, ON DELETE RESTRICT

doctor_id: BIGINT, Foreign Key → doctors(id), Not Null, ON DELETE RESTRICT

appointment_time: DATETIME, Not Null

duration_minutes: INT, Not Null, Default 60

status: TINYINT, Not Null, Default 0 (0 = Scheduled, 1 = Completed, 2 = Cancelled)

reason: TEXT, Nullable

created_at: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

Unique constraint: (doctor_id, appointment_time)

<!-- A doctor must NOT have overlapping appointments. The unique key blocks identical start times;
     full overlap checking (start + duration) is done in the service layer before saving.
     appointment_time must be in the future when booking (validated in code).
     Past appointments are kept forever as medical history (status changes instead of deleting). -->

### Table: payments
id: BIGINT, Primary Key, Auto Increment

appointment_id: BIGINT, Foreign Key → appointments(id), Not Null, Unique, ON DELETE RESTRICT

amount: DECIMAL(10,2), Not Null

status: VARCHAR(30), Not Null (PENDING, PAID, REFUNDED, FAILED)

payment_method: VARCHAR(50), Nullable (CASH, CARD, BANK_TRANSFER)

transaction_date: TIMESTAMP, Not Null, Default CURRENT_TIMESTAMP

<!-- One payment per appointment (UNIQUE appointment_id). DECIMAL is used instead of FLOAT to avoid rounding errors.
     Financial records must never be deleted, hence RESTRICT. -->

### Relationships Summary
- clinic_locations 1 — N doctors
- doctors 1 — N doctor_available_slots
- doctors 1 — N appointments
- patients 1 — N appointments
- appointments 1 — 1 payments
- appointments 1 — N prescriptions (stored in MongoDB, linked by `appointmentId`)

<!-- Prescriptions are tied to a specific appointment: a doctor writes a prescription as the outcome
     of a visit. They live in MongoDB because their content (medications list, notes, metadata) is flexible. -->

## MongoDB Collection Design

MongoDB stores flexible, semi-structured data that does not fit rigid tables: prescriptions, free-form notes, feedback, logs.

### Collection: prescriptions

```json
{
  "_id": "ObjectId('6745a1c2e4b0f3a1d2c3b4a5')",
  "appointmentId": 51,
  "patientId": 12,
  "doctorId": 3,
  "patientName": "John Smith",
  "issuedAt": "2026-11-02T10:30:00Z",
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "durationDays": 5,
      "instructions": "Take after meals"
    },
    {
      "name": "Vitamin C",
      "dosage": "1000mg",
      "frequency": "Once a day",
      "durationDays": 10
    }
  ],
  "doctorNotes": "Mild fever and sore throat. Drink plenty of water and rest.",
  "refillCount": 1,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  },
  "tags": ["fever", "follow-up"],
  "attachments": [
    {
      "fileName": "blood_test.pdf",
      "url": "/files/prescriptions/51/blood_test.pdf",
      "uploadedAt": "2026-11-02T10:35:00Z"
    }
  ],
  "metadata": {
    "createdBy": "doctor",
    "schemaVersion": 1,
    "lastUpdated": "2026-11-02T10:40:00Z"
  }
}
```

Design notes:
- **IDs instead of full objects:** the document stores only `patientId`, `doctorId`, `appointmentId` (references to MySQL). The full patient/doctor objects are not embedded to avoid duplicated data getting out of sync. `patientName` is a small denormalized field kept for fast display.
- **Embedded arrays:** `medications` and `attachments` are embedded because they belong only to this prescription and are always read together with it.
- **Schema evolution:** `metadata.schemaVersion` lets the application handle old and new document shapes. New optional fields (e.g. `pharmacy`, `tags`) can be added without migrations; old documents simply don't have them.

### Collection: messages (example of a chat message)

```json
{
  "_id": "ObjectId('6745b2d3f5c1a4b2e3d4c5b6')",
  "conversationId": "patient-12_doctor-3",
  "sender": { "role": "patient", "id": 12 },
  "receiver": { "role": "doctor", "id": 3 },
  "content": "Doctor, can I take the medicine before sleeping?",
  "sentAt": "2026-11-02T20:15:00Z",
  "read": false,
  "attachments": [],
  "relatedAppointmentId": 51
}
```

<!-- Messages are append-only and high-volume, which suits MongoDB. Index on (conversationId, sentAt) for fast history loading. -->