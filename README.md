# International Student Management System — OGE, IIT Madras

A small full-stack app for the **Office of Global Engagement (OGE), IIT Madras**, for the **International Semester Exchange Programme (July–November 2026)**.

Institution-specific text (office name, hostels, office timings, form links) lives in `frontend/src/config/institution.js`. The required-document list lives in the database (migration `V5`) and a Lead can change it on **Settings**.

**Why:** international student information is currently collected through Google Forms/Sheets, which makes it hard to review and cross-check each student's details, hostel/room, documents and updates. This system keeps everything about each student in one place, with a review trail.

**Student side**
* Fill in and update personal, passport/visa/travel and academic information
* Enter hostel name and room number
* Upload the required documents
* See exactly what is missing or **needs correction** (with the Lead's comment) on the dashboard
* See which information has been **verified**
* View free appointment slots and book one

**International Lead (admin) side**
* All students and their complete information in one place, with search and filters
* Review each student on one page and mark the profile, hostel details and each document as **Verified** or **Needs correction** (with a comment)
* See uploaded vs. missing documents per student
* Update/verify hostel and room details directly
* Manage appointment slots and see scheduled appointments

| Part | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security + JWT, Spring Data JPA, JDBC, Gradle |
| Database | PostgreSQL, schema managed by Flyway migrations |
| Frontend | React 19 + React Router, built with Vite |

---

## 1. Project structure

```
build.gradle, settings.gradle          Gradle build (backend)
src/main/java/com/example/student_management_system/
  auth/          signup, login, /me, Lead creation      (AuthController, LeadAccountController, AuthService)
  user/          accounts (AppUser, Role), first-Lead bootstrap
  security/      JWT creation/checking, logged-in user (AuthUser)
  config/        SecurityConfig: URL rules, CORS, password encoder
  student/       profiles, hostel, Lead review (ReviewController/Service), student list/search/stats
  document/      document types, upload/download/delete, checklist
  appointment/   slots and bookings
  common/        error handling (ApiException, GlobalExceptionHandler), paging
  demo/          DemoDataLoader: fictional demo students (DEMO_DATA=true)
src/main/resources/
  application.properties               settings (DB, JWT, uploads) – secrets come from env variables
  db/migration/V1..V6__*.sql            tables, OGE document list, review workflow (run automatically by Flyway)
  demo-documents/*.pdf                 watermarked SAMPLE documents (no real data)
src/test/java/...                       unit tests
frontend/                               React app (see section 5)
frontend/src/config/institution.js      OGE / IIT Madras names, hostels, timings, links
scripts/api-smoke-test.mjs              end-to-end API test (see section 6)
```

Every module follows the same three layers:
**Controller** (HTTP in/out, DTOs) → **Service** (business rules + permission checks) → **Repository** (database).

## 2. Database design

```
users ──1:1── student_profiles          login data and profile data are kept apart
  │
  ├──1:n── documents ──n:1── document_types     one file per (student, type); types are configurable
  │
  ├──1:n── appointment_slots (lead_id)          a Lead's free time
  │              │
  └──1:n── appointments (student_id) ──n:1──┘   bookings, incl. cancelled ones (history)
```

Rules enforced **by the database itself**, not only by Java code:

| Rule | How |
|---|---|
| Email is unique | `UNIQUE (email)` |
| Student ID is unique | `UNIQUE (student_number)` |
| One file per document type per student | `UNIQUE (student_id, document_type_id)` |
| A Lead's slots never overlap | PostgreSQL exclusion constraint `ex_slots_no_overlap` (needs the `btree_gist` extension, included with PostgreSQL) |
| A slot can't be booked twice | partial unique index: one `BOOKED` appointment per slot |
| Allowed values (roles, statuses) | `CHECK` constraints |

Document completion and profile completion are **calculated** from the actual data every time; they are never typed in by hand.

### Review / verification workflow (migration `V6`)

| Item | Statuses | Set by |
|---|---|---|
| Profile (personal, passport/visa/travel, academic) | `PENDING` → `VERIFIED` or `NEEDS_CORRECTION` + comment | Lead |
| Hostel details | `PENDING` → `VERIFIED` or `NEEDS_CORRECTION` + comment | Lead (a Lead editing the hostel directly also verifies it) |
| Each document | `UPLOADED` (awaiting review) → `VERIFIED` or `NEEDS_CORRECTION` + comment | Lead |

* A correction request **must** include a comment; the student sees it on their dashboard and next to the item.
* When a student **changes** verified details (or uploads a new file), that item goes back to awaiting review. Saving without changes keeps it verified.
* An incomplete profile can't be marked verified.
* Each student also gets one summary state for the Lead's list: **Awaiting review**, **Needs correction**, **Incomplete** or **Fully verified**.

### Student intake fields

The real Google Form/Sheet wasn't available (privacy), so these fields are a best guess at an exchange-student intake form. All fields except those marked optional count toward profile completion.

| Group | Fields |
|---|---|
| Personal | full name, gender, date of birth, nationality, phone, address in India, emergency contact (optional) |
| Passport / visa / travel | passport number, passport expiry, visa valid until (optional), arrival date, departure date (optional) |
| Academic | home university, student category (exchange / degree), IIT Madras roll number, programme, host department, semester |
| Hostel | allocation status, hostel (Tamiraparani / Sabarmati), room number |

To match the real form later, change: the migration (new `V7__...sql`), `StudentProfile.java`, `ProfileUpdateRequest.java`, `ProfileResponse.java`, `ProfileCompletion.java`, `ProfileService.java` and `frontend/src/pages/student/ProfilePage.jsx`.

### OGE document checklist (migration `V5`)

| # | Document | Required for | Form |
|---|---|---|---|
| 1 | IIT Madras offer letter | all students | |
| 2 | Passport / citizenship proof | all students | |
| 3 | Visa details | all students | |
| 4 | Bonafide letter (home university) | all students | |
| 5 | Signed BIODATA form | all students | [iKollege](https://ikollege.iitm.ac.in/iitmhostel/studentRegistration) |
| 6 | Self-Declaration Addendum | all students | [Google Drive](https://drive.google.com/file/d/12nHAzLFJMCPFki6-V0J96BXka4_bg0b7/view) |
| 7 | Solemn Affirmation of Conformance to Rules and Regulations | all students | [Google Drive](https://drive.google.com/file/d/1br-VamYmC9J4QhiH59nuHctbYmNrY1-J/view) |
| 8 | Bank details proof | all students | |
| 9 | Passport-size colour photograph | all students | |
| 10 | Health insurance | all students | |
| 11 | Exchange nomination letter | semester exchange students | |
| 12 | Learning agreement | semester exchange students | |
| 13 | Previous degree certificate | full-time degree students | |

Students upload scans here **and** bring the signed originals / hard copies to the hostel office on arrival.

### Hostel allotment (shown on the student Hostel page)

* Male international students: **Tamiraparani Hostel**; female international students: **Sabarmati Hostel**
* Hostel office timings: Monday–Friday 10:00 AM – 6:00 PM, Saturday 10:30 AM – 1:00 PM
* Hostel fee payment: Temporary Accommodation Section, Office of Hostel Management, Ground Floor, Room No. 105 ([map](https://maps.app.goo.gl/nMPtqozebMNyzLR76))

## 3. Requirements

* JDK 17 or newer
* PostgreSQL 14 or newer
* Node.js 20.19+ or 22.12+ (for the React app)

## 4. Run the backend

1. Create the database once:
   ```
   psql -U postgres -c "CREATE DATABASE student_management;"
   ```
   (or create `student_management` in pgAdmin). You don't create tables yourself: Flyway runs `V1`–`V6` on startup.

   On Windows, if `psql` is "not recognized", use its full path, e.g.
   `& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -c "CREATE DATABASE student_management;"`

2. Set the first International Lead account and start the backend.

   **Windows PowerShell**
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"   # your JDK 17 folder
   $env:LEAD_EMAIL="lead@iitm.edu"
   $env:LEAD_PASSWORD="LeadPass123"
   .\gradlew bootRun
   ```
   **macOS / Linux**
   ```bash
   export LEAD_EMAIL=lead@iitm.edu LEAD_PASSWORD=LeadPass123
   ./gradlew bootRun
   ```

   On startup, check that the log shows `Successfully applied 6 migrations` (or fewer if some were already applied), then `Created bootstrap International Lead account lead@iitm.edu`.
   The backend listens on http://localhost:8080.

### Demo data and sample documents (no real student data)

Real student documents must not be used for testing. To try the system with realistic content, start the backend with demo data:

```powershell
$env:DEMO_DATA="true"
.\gradlew bootRun
```

This loads 8 **fictional** students once (emails `@example.com`, password `Student123`, change with `DEMO_PASSWORD`), each with watermarked SAMPLE PDFs and a different review state:

| Student (fictional) | Shows |
|---|---|
| elina.virtanen@example.com | fully verified |
| lukas.becker@example.com | everything submitted, awaiting review; has a booked appointment |
| sofia.martinez@example.com | passport sent back for correction; has a booked appointment |
| kenji.tanaka@example.com | profile unfinished, 3 documents awaiting review |
| amara.okafor@example.com | degree student; profile sent back for correction; documents missing |
| lucas.moreau@example.com | just signed up, nothing filled in |
| emma.johansson@example.com | half the documents verified, rest awaiting review |
| matteo.rossi@example.com | bank proof and hostel room sent back for correction |

It also creates a few appointment slots for the first Lead. For manual upload tests, use the sample PDFs in `src/main/resources/demo-documents/`. Every name and number in them is fictional.

### Settings (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/student_management` | database |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `admin` | database login |
| `JWT_SECRET` | dev-only value | **set your own (32+ characters) for anything shared** |
| `JWT_EXPIRATION_MINUTES` | `120` | how long a login lasts |
| `LEAD_EMAIL` / `LEAD_PASSWORD` / `LEAD_NAME` | – / – / `OGE International Lead` | first Lead account, created once if missing |
| `UPLOAD_DIR` | `./uploads` | where uploaded files are stored |
| `DEMO_DATA` / `DEMO_PASSWORD` | `false` / `Student123` | load the fictional demo students (see above) |
| `CORS_ORIGINS` | `http://localhost:5173` | frontend address(es), comma-separated |

### How Lead accounts are created (no self-registration)

* `POST /api/auth/signup` **always** creates a STUDENT. There is no role field; sending one is ignored.
* The first Lead is created from `LEAD_EMAIL` / `LEAD_PASSWORD` when the app starts.
* After that, a logged-in Lead creates more Leads on **Settings → Add an International Lead** (`POST /api/admin/leads`).

## 5. Run the frontend

In a second terminal:
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. Vite forwards every `/api/...` call to the backend on port 8080, so both must be running.

| Role | Pages |
|---|---|
| Common | Login, Signup |
| Student | Dashboard (to-do list of missing items and corrections), My profile, Hostel, Documents (upload / view / download / replace / delete with confirmation, review status and comments), Appointments (book, cancel, history) |
| Lead | Dashboard (review queue, statistics, missing documents, upcoming bookings), Students (search / review-status and other filters / sort / pages), Student review page (profile, hostel, documents, appointments with Verify / Request correction), Appointment slots (create, cancel, booking overview), Settings (document types, new Leads) |

`npm run build` creates a production build in `frontend/dist`.

## 6. Testing

### Automatic end-to-end test (recommended)

With the backend running:
```
node scripts/api-smoke-test.mjs
```
It runs about 85 checks that cover every scenario in the project brief: signup and login, role redirects, ownership checks such as student A trying to read student B's data, secure file upload, the review workflow (correction requests, re-review after changes), search, filter and sort, double-booking protection (two simultaneous bookings) and error codes. It finishes with `N passed, 0 failed`. If your Lead uses different credentials, set `LEAD_EMAIL` / `LEAD_PASSWORD` before running it.

### Unit tests
```
.\gradlew test        (Windows)      ./gradlew test        (macOS/Linux)
```
`DocumentRulesTest` and `ProfileCompletionTest` need no database. The default `contextLoads` test starts the whole app, so PostgreSQL must be running.

### Trying the API by hand (PowerShell)

```powershell
$api = "http://localhost:8080/api"
# sign up + log in
Invoke-RestMethod "$api/auth/signup" -Method Post -ContentType "application/json" -Body '{"fullName":"Asha Rao","email":"asha.rao@example.com","password":"Student123"}'
$s = Invoke-RestMethod "$api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"asha.rao@example.com","password":"Student123"}'
$h = @{ Authorization = "Bearer $($s.token)" }
# my profile and document checklist
Invoke-RestMethod "$api/students/me/profile" -Headers $h
(Invoke-RestMethod "$api/documents/required" -Headers $h).items | Format-Table name, uploaded
# upload a document (curl.exe ships with Windows 10+)
curl.exe -H "Authorization: Bearer $($s.token)" -F "documentTypeId=1" -F "file=@C:\path\passport.pdf" "$api/documents"
```
To see the HTTP status code of an error in PowerShell, use `curl.exe -i ...`, or wrap the call in `try { ... } catch { $_.Exception.Response.StatusCode }`.

### Manual UI checklist

- [ ] Sign up → log in → lands on **/student**; Lead login lands on **/lead**
- [ ] Student opening `/lead` is sent back to their own dashboard
- [ ] Wrong password shows "Invalid email or password"
- [ ] Profile save shows success; completion bar updates; email can't be edited
- [ ] Upload a PDF/JPG/PNG; a `.txt` or a file over 5 MB is refused with a message
- [ ] View, download and replace a document; **Delete** asks for confirmation first
- [ ] Lead: search, the four filters, column sorting and paging on **Students**
- [ ] Lead: open a student's profile and document checklist (uploaded vs. missing)
- [ ] Lead creates a slot; overlapping or past slots are refused
- [ ] Student books it; it disappears from "Available times"; cancel puts it back
- [ ] Lead: on a student's page, request a correction on a document with a comment; the student sees it on their dashboard
- [ ] Student uploads a corrected file; it shows "Awaiting review" again; Lead verifies it
- [ ] Lead verifies a profile; student changes a field; it goes back to "Awaiting review"
- [ ] Lead edits a student's room number; it shows as verified for the student
- [ ] Log out → the browser back button doesn't show protected pages

## 7. API reference

All endpoints except signup and login need the header `Authorization: Bearer <token>`.
Errors always look like `{"status":403,"error":"Forbidden","message":"...","fieldErrors":{}}`.

| Method & path | Who | Purpose |
|---|---|---|
| `POST /api/auth/signup` | anyone | create a student account → 201 |
| `POST /api/auth/login` | anyone | get a JWT + user (role) |
| `GET /api/auth/me` | logged in | current user |
| `POST /api/admin/leads` | Lead | create another Lead → 201 |
| `GET /api/students` | Lead | list; query: `search`, `reviewState` (`AWAITING_REVIEW`/`NEEDS_CORRECTION`/`INCOMPLETE`/`VERIFIED`), `profileComplete`, `documentsComplete`, `hostelStatus`, `missingDocumentTypeId`, `sort` (`name`/`studentNumber`/`review`/`profile`/`documents`), `direction`, `page`, `size` |
| `GET /api/students/stats` | Lead | dashboard numbers |
| `GET /api/students/by-student-number/{n}` | Lead | find by university student ID |
| `GET /api/students/{id}` | Lead, or that student | summary incl. completion |
| `GET /api/students/{id}/profile` | Lead, or that student | full profile |
| `GET /api/students/me/profile` | Student | own profile |
| `PUT /api/students/{id}/profile` | that student | update basic + academic info |
| `PUT /api/students/{id}/hostel` | that student, or Lead | update hostel info (a Lead's update counts as verified) |
| `PUT /api/students/{id}/profile/review` | Lead | `{"decision":"VERIFIED"}` or `{"decision":"NEEDS_CORRECTION","comment":"..."}` |
| `PUT /api/students/{id}/hostel/review` | Lead | same body, for the hostel details |
| `PUT /api/documents/{id}/review` | Lead | same body, for one document |
| `GET /api/documents/required` | Student | own checklist |
| `GET /api/documents/my` | Student | own uploaded documents |
| `POST /api/documents` | Student | upload/replace; multipart fields `documentTypeId`, `file` → 201 |
| `GET /api/documents/{id}` | owner or Lead | metadata |
| `GET /api/documents/{id}/file` | owner or Lead | the file (`?inline=true` to view) |
| `DELETE /api/documents/{id}` | owner | delete → 204 |
| `GET /api/students/{id}/documents` | Lead, or that student | checklist with completion |
| `GET/POST /api/admin/document-types`, `PUT /api/admin/document-types/{id}` | Lead | manage required documents |
| `GET /api/appointments/available` | Student | free future slots |
| `POST /api/appointments` | Student | book `{"slotId":1}` → 201 (409 if taken) |
| `GET /api/appointments/my` | Student | own bookings and history |
| `POST /api/appointments/{id}/cancel` | the student, or the slot's Lead | cancel a booking (slot becomes free) |
| `POST /api/appointments/slots` | Lead | create `{"date":"2026-11-02","startTime":"10:00","endTime":"10:30"}` → 201 |
| `GET /api/appointments/slots` | Lead | own slots with who booked them (`?upcomingOnly=true`) |
| `POST /api/appointments/slots/{id}/cancel` | Lead | cancel a slot and its booking |
| `GET /api/appointments/bookings` | Lead | all bookings on own slots |

Status codes used: 200, 201, 204, 400 (validation), 401 (not logged in / bad token), 403 (wrong role or someone else's data), 404, 409 (duplicate, overlap, already booked), 413 (file too large), 415 (file type not allowed).

## 8. Security notes

* Passwords are hashed with BCrypt; plain passwords are never stored or logged.
* JWTs are signed with `JWT_SECRET` and expire after `JWT_EXPIRATION_MINUTES`. Logging out deletes the token in the browser.
* Roles are checked on the server (`SecurityConfig` + `@PreAuthorize`). Ownership is checked in the services, so changing an id in the URL doesn't reveal someone else's data.
* Uploads: only PDF/JPG/PNG up to 5 MB. Both the extension **and** the file's first bytes are checked. Files are stored under random names outside the web root and are served only through the authenticated download endpoint. Server paths are never sent to the browser.
* Booking uses one atomic SQL `UPDATE ... WHERE status = 'AVAILABLE'`, and slots carry a `@Version` column. Together with the database constraints, two people can't book or change the same slot at the same time.

## 9. Troubleshooting

| Problem | Fix |
|---|---|
| `Found non-empty schema(s) "public" without schema history table` | The database already has tables from an earlier experiment. Drop and recreate `student_management`, then start again. |
| `password authentication failed for user "postgres"` | Set `DB_PASSWORD` to your PostgreSQL password. |
| `app.jwt.secret ... at least 32 characters` | `JWT_SECRET` is too short. |
| Frontend shows "Cannot reach the server" | Start the backend; it must be on port 8080 (or change `vite.config.js`). |
| Port 8080 already in use | Stop the other program, or run with `--args='--server.port=8081'` and update `vite.config.js`. |
| Lead login fails | `LEAD_EMAIL` / `LEAD_PASSWORD` only take effect the **first** time that email is created. If the account already exists with another password, delete that row from `users` and restart, or set a new BCrypt hash in `users.password_hash`. |
| `JAVA_HOME is set to an invalid directory` | Point `JAVA_HOME` at your JDK folder, e.g. `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` (not its `bin` subfolder). |
| `psql` is not recognized | Use the full path `C:\Program Files\PostgreSQL\18\bin\psql.exe`, or pgAdmin. |
| `Validate failed: Migration checksum mismatch` | A migration that already ran was edited. Undo the edit and put the change in a new `V6__...sql` file instead. |
