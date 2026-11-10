# Ecosphere — Waste Pickup Booking

Ecosphere is an Android application for household waste pickup bookings. Customers can register, schedule pickups, track booking status, and cancel eligible bookings.

**Platform:** Android, implemented with Kotlin and XML. iOS is not implemented; a future iOS client could use Swift and SwiftUI.

## Live Demo

- **Backend health:** [ecosphere-backend-7tlm.onrender.com/health](https://ecosphere-backend-7tlm.onrender.com/health)
- **Android API:** `https://ecosphere-backend-7tlm.onrender.com/api/`
- **Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` (build instructions below)

The backend stores shared user and booking records in MongoDB Atlas. Android also caches bookings locally in Room.

## Features

- Register with name, email, mobile number, and password; sign in with email or mobile number.
- View customer information, booking totals, and recent bookings.
- Book Plastic, Paper, E-waste, or General Waste pickups with an address, date, and time.
- View booking history and details, with readable booking references.
- Track Pending, Confirmed, Completed, and Cancelled statuses.
- Cancel eligible Pending or Confirmed bookings.
- Refresh the dashboard, history, and booking details from the backend.

## Screenshots

<table>
  <tr>
    <td align="center"><strong>Login</strong><br><img src="docs/screenshots/login_screen.png" width="260" alt="Ecosphere login screen"></td>
    <td align="center"><strong>Registration</strong><br><img src="docs/screenshots/registration_screen.png" width="260" alt="Ecosphere registration screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Dashboard</strong><br><img src="docs/screenshots/dashboard_screen.png" width="260" alt="Ecosphere dashboard"></td>
    <td align="center"><strong>Create booking</strong><br><img src="docs/screenshots/booking_waste_pickup_screen.png" width="260" alt="Ecosphere create booking screen"></td>
  </tr>
  <tr>
    <td align="center"><strong>Booking history</strong><br><img src="docs/screenshots/booking_history_screen.png" width="260" alt="Ecosphere booking history"></td>
    <td align="center"><strong>Booking details</strong><br><img src="docs/screenshots/booking_details_screen.png" width="260" alt="Ecosphere booking details"></td>
  </tr>
</table>

## Technology and Architecture

- **Android:** Kotlin, XML, ViewBinding, Material Components, Navigation Component, MVVM, StateFlow, and manual dependency injection.
- **Local data and networking:** Room, Retrofit, OkHttp, and Gson.
- **Backend:** Node.js, Express, Mongoose, JWT authentication, and bcrypt password hashing.
- **Database and hosting:** MongoDB Atlas and Render.

```text
Android app (Kotlin/XML, Retrofit, Room)
                  |
                  | HTTPS REST API
                  v
          Render / Express API
                  |
                  | Mongoose
                  v
            MongoDB Atlas
```

Fragments render UI state and handle navigation. ViewModels own screen state and actions. Repositories coordinate the API and Room. The backend validates requests, verifies JWTs, checks booking ownership, and persists records in Atlas.

### Data model

The Atlas database is intended to be `ecosphere_db`, with two collections:

- **`users`:** UUID, name, email, mobile number, and bcrypt password hash. Email, mobile, and UUID have unique indexes.
- **`bookings`:** UUID, owner UUID, customer/contact details, waste category, pickup address/date/time, status, and creation timestamp. A compound index supports owner-scoped, newest-first history.

MongoDB `_id` values are internal. The API continues to use the existing UUID strings, and password hashes are never included in API responses.

## API Overview

Customer routes use `Authorization: Bearer <JWT>`. The operator status route uses a private `x-admin-api-key`, which is not included in Android.

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/health` | Backend and database readiness |
| `POST` | `/api/auth/register` | Create a customer account |
| `POST` | `/api/auth/login` | Authenticate a customer |
| `POST` | `/api/bookings` | Create a booking |
| `GET` | `/api/bookings` | List the signed-in customer's bookings |
| `GET` | `/api/bookings/:id` | View an owned booking |
| `PATCH` | `/api/bookings/:id/cancel` | Cancel an eligible booking |
| `PATCH` | `/api/admin/bookings/:id/status` | Operator transition to Confirmed or Completed |

New bookings start as Pending. Operators can move them from Pending to Confirmed, then Confirmed to Completed. Customers can cancel Pending or Confirmed bookings; they cannot set operator-controlled statuses.

## Run the Android App

Requirements: Android Studio, Android SDK 36, and JDK 21.

1. Open this repository in Android Studio and allow Gradle sync to finish.
2. Run the `app` configuration on an emulator or Android device. The app is configured to use the live HTTPS API.

To build a debug APK from PowerShell at the repository root:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. A physical-device flow has been manually verified: registration, login, booking creation, booking history, and booking details.

## Backend Checks

From the `backend/` directory:

```powershell
npm ci
npm test
```

`npm test` runs the backend model validation tests. `npm run smoke` runs an Atlas-backed API and persistence check; it requires a local `.env` configured for the database and removes only the uniquely generated records created by that test.

## Scope

- Android is implemented; iOS is not included.
- Automated Android UI/instrumentation tests and Play Store releases are not included.
- The committed APK path is a build output location; generate the APK locally with the command above.
