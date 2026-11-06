# Ecosphere Waste Pickup

## 1. Project Overview

Ecosphere is an Android application for household waste pickup booking. Customers can create an account, submit pickup requests, review their bookings, and track booking status. The repository includes the Android client and an Express backend for local assignment demonstrations.

The implemented platform is Android, built with Kotlin and XML. iOS is not implemented; a future iOS client could be developed with Swift and SwiftUI.

## 2. Features

- Register with a name, email, mobile number, and password; sign in with email or mobile number and password.
- View customer information, booking totals, and recent bookings on the dashboard.
- Create pickups for Plastic, Paper, E-waste, or General Waste, with an address, preferred date, and time.
- View booking history and full booking details.
- Track Pending, Confirmed, Completed, and Cancelled statuses. Customers can cancel eligible Pending or Confirmed bookings.
- Pull to refresh dashboard, history, and booking details from the API.
- Keep booking records in Room, scoped locally to the signed-in user.
- Show loading, success, error, and cancellation confirmation states.
- Display a readable booking reference while retaining the full UUID for navigation and API operations.

## 3. Screenshots

These are the existing screenshots for the six main Android screens. Their image files and paths are preserved.

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

## 4. Technology Stack

- Android: Kotlin, XML layouts, ViewBinding, Material Components, Navigation Component
- Application architecture: feature-first MVVM, ViewModels, StateFlow, and coroutines
- Networking: Retrofit, OkHttp, and Gson
- Local booking storage: Room
- Session token storage: Android Keystore-backed AES-GCM encryption with app-private SharedPreferences
- Backend: Node.js, Express, JWT authentication, bcrypt password hashing, and a local JSON data file
- Dependency wiring: manual `AppContainer` and ViewModel factories

## 5. Project Requirements

- Android Studio with the Android SDK required by the project (compile SDK 36) and JDK 21
- Node.js 20 or newer and npm
- An Android Studio emulator or a physical Android device
- A network connection between the Android app and the backend

## 6. Repository Structure

```text
app/src/main/java/com/ecosphere/wastepickup/
  auth/       Registration and login UI, ViewModels, API models, repository
  booking/    Booking creation, history, details, shared list adapter, Room, API
  home/       Dashboard UI and ViewModel
  core/       Network, database, token security, manual dependency injection
backend/     Express API and environment template
 docs/       Existing application screenshots
```

## 7. Backend Setup

From the repository root, use PowerShell:

```powershell
cd backend
npm install
Copy-Item .env.example .env
notepad .env
```

Set separate private values for `JWT_SECRET` and `ADMIN_API_KEY`. `JWT_SECRET` signs and verifies customer session tokens. `ADMIN_API_KEY` authorizes operator booking status changes. Use placeholders only in documentation; do not share or commit real values.

Start the backend and leave the terminal open:

```powershell
npm start
```

The server listens on port 3000 and binds to all network interfaces. Check its health endpoint from another terminal:

```powershell
Invoke-RestMethod http://localhost:3000/api/health
```

The backend loads and saves users and bookings as JSON in `backend/data/store.json` by default. Set `ECOSPHERE_DATA_FILE` to use a different path. Writes use a temporary file followed by a rename. Passwords are stored as bcrypt hashes. This local JSON store is intended for the assignment demonstration, not production or multi-instance deployment.

The backend ignores `.env`, `data/`, and `node_modules/`. Keep `.env` and generated private data files out of Git.

## 8. API Base URL Configuration

The API URL is configured in [`app/src/main/java/com/ecosphere/wastepickup/core/network/ApiClient.kt`](app/src/main/java/com/ecosphere/wastepickup/core/network/ApiClient.kt), in the `BASE_URL` constant. Keep the trailing `/api/` path.

**Android Studio emulator:** use:

```text
http://10.0.2.2:3000/api/
```

`10.0.2.2` is a special address used by the standard Android Studio emulator to reach the development computer. It is not the computer's LAN IP address.

**Physical Android device:**

1. Connect the phone and development computer to the same Wi-Fi network.
2. On Windows, run `ipconfig` in PowerShell or Command Prompt.
3. Find the active network adapter and its IPv4 address.
4. Replace `10.0.2.2` in `ApiClient.kt` with that reachable LAN address, keeping the port and `/api/` suffix. For example:

   ```text
   http://192.168.1.8:3000/api/
   ```

   This is only an example; use the computer's own reachable IPv4 address. Do not publish a private LAN address in a shared README or APK instructions.

The backend must be running on port 3000 and accept connections from the phone. Allow incoming connections on port 3000 through Windows Firewall if required. From the phone's browser, check:

```text
http://YOUR_COMPUTER_IP:3000/api/health
```

A private address such as `192.168.x.x` works only while the phone can reach the computer on that network and the backend is running. An APK configured with a developer's private LAN address will not work for an evaluator on an unrelated network. Independent remote evaluation requires deploying the backend to publicly accessible hosting with HTTPS, then rebuilding the APK with that deployed API URL.

## 9. Running the Android Application

1. Start the backend as described above.
2. Set `BASE_URL` in `ApiClient.kt` for the chosen emulator or physical device network.
3. Open the repository root in Android Studio and allow Gradle sync to complete.
4. Start an emulator or connect a device with USB debugging enabled, then run the `app` configuration.

The app uses cleartext HTTP for local development. See [Assignment Scope and Known Limitations](#16-assignment-scope-and-known-limitations).

## 10. Building the Android APK

Configure the API base URL for the target environment before building. From the repository root in PowerShell, run:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

This debug APK is for assignment demonstration and testing, not a production Play Store release. A locally configured LAN URL will only work on a network that can reach that development computer.

## 11. Typical Application Flow

1. Register an account, then sign in with email or mobile number and password.
2. Create a booking with its waste category, pickup address, date, and time.
3. Open a booking from the dashboard or Booking History to view its details.
4. Pull down on the Dashboard, History, or Details screen to fetch current server data.
5. Cancel an eligible Pending or Confirmed booking from its Details screen.

## 12. Booking Statuses and Operator Status Updates

New bookings start as `Pending`. An operator can move a booking from `Pending` to `Confirmed`, then from `Confirmed` to `Completed`. Customers cannot set operator-controlled statuses. Eligible customers can cancel Pending or Confirmed bookings; Completed and Cancelled bookings cannot be cancelled.

For a demonstration, sign in to the API to obtain the customer's booking ID (the full internal UUID):

```powershell
$credentials = @{ emailOrMobile = "customer@example.com"; password = "<PASSWORD>" } | ConvertTo-Json
$session = Invoke-RestMethod -Method Post -Uri "http://localhost:3000/api/auth/login" -ContentType "application/json" -Body $credentials
$headers = @{ Authorization = "Bearer $($session.token)" }
$bookings = Invoke-RestMethod -Method Get -Uri "http://localhost:3000/api/bookings" -Headers $headers
$bookingId = $bookings[0].id
```

Use the private `ADMIN_API_KEY` from the backend `.env` file to update the status. Run the request first with `Confirmed`, then with `Completed`:

```powershell
$adminHeaders = @{ "x-admin-api-key" = "<ADMIN_API_KEY>" }
$body = @{ status = "Confirmed" } | ConvertTo-Json
Invoke-RestMethod -Method Patch -Uri "http://localhost:3000/api/admin/bookings/$bookingId/status" -Headers $adminHeaders -ContentType "application/json" -Body $body
```

For the second transition, set `$body` to `@{ status = "Completed" } | ConvertTo-Json` and send the same request again. The endpoint rejects unsupported statuses and invalid transitions. Pull to refresh in the app to see the updated status.

## 13. Architecture and Data Handling

The Android client follows the existing feature-first structure and MVVM flow:

- Fragments render UI state, collect user input, and handle navigation.
- ViewModels own screen state, validation, and UI actions.
- Repositories coordinate remote API calls and local Room data.
- Retrofit declares REST endpoints; OkHttp configures the client, timeouts, and basic request/response logging; Gson parses JSON.
- Room stores booking UUIDs, owner IDs, pickup and contact details, status, sync state, and creation time. The database includes a migration that adds the owner ID column.
- The Android session token is encrypted using AES-GCM with a key held in Android Keystore. Encrypted token data and session profile fields are kept in app-private SharedPreferences.

The backend issues JWTs with a user ID subject. Protected booking endpoints verify the token and scope list, detail, creation, and cancellation operations to that authenticated user. The client also filters local Room records by the signed-in user ID. Operator status changes use a separate administrator API key and are not exposed as a customer action in the Android app.

The Express backend handles authentication, booking validation, ownership checks, and status transitions. It persists user and booking data in the local JSON file described above.

Booking references such as `ECO-A12B34C5` are customer-facing display values. The full UUID remains the internal database key and is used for API requests and navigation.

## 14. API Endpoints

All routes are relative to `http://HOST:3000/api/`. Customer booking routes require `Authorization: Bearer <JWT>`. The operator status route requires the `x-admin-api-key` header; it does not use a customer JWT.

| Method | Route | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/health` | None | Check backend availability |
| `POST` | `/auth/register` | None | Register a customer |
| `POST` | `/auth/login` | None | Sign in and receive a JWT |
| `POST` | `/bookings` | Customer JWT | Create an authenticated user's booking |
| `GET` | `/bookings` | Customer JWT | List the authenticated user's bookings |
| `GET` | `/bookings/:id` | Customer JWT | Get one of the authenticated user's bookings |
| `PATCH` | `/bookings/:id/cancel` | Customer JWT | Cancel an eligible booking owned by the user |
| `PATCH` | `/admin/bookings/:id/status` | `x-admin-api-key` | Operator status transition to Confirmed or Completed |

## 15. Troubleshooting

- **The app reports a network, internet, or server error:** Confirm the backend is running and check `/api/health` from the device. Verify that the configured URL uses port 3000 and ends in `/api/`.
- **Registration or login fails:** Check backend output and confirm `JWT_SECRET` is set in `backend/.env`. Registration requires a valid name, email, 10–15 digit mobile number (optionally prefixed with `+`), and a password of at least eight characters. An email or mobile number already in use cannot be registered again.
- **The emulator works but a physical device does not:** Replace the emulator host with the computer's active LAN IPv4 address. Ensure both devices are on the same reachable Wi-Fi network and test the phone's browser against `/api/health`.
- **The API base URL is incorrect:** Update `BASE_URL` in `ApiClient.kt`, including `http://`, port `3000`, and `/api/`. Rebuild/reinstall the app after changing the source.
- **The backend is not running:** From `backend/`, run `npm start`; keep the process open. If dependencies are missing, run `npm install` first.
- **Windows Firewall blocks port 3000:** Allow Node.js or inbound TCP traffic on port 3000 for the private network profile, following local machine policy.
- **The computer's IP address changes:** Run `ipconfig` again and update `BASE_URL` in `ApiClient.kt` to the active adapter's current IPv4 address.

## 16. Assignment Scope and Known Limitations

- Android is implemented using Kotlin and XML. iOS is not implemented; the proposed iOS client would use Swift and SwiftUI.
- The backend uses a local JSON data store for the assignment demonstration. Public backend deployment is not included.
- Cleartext HTTP is for local development only. Production use requires HTTPS, managed database storage, and proper administrator authentication and authorization.
- Automated UI/instrumentation test coverage and published Play Store or iOS releases are not included.
- A debug APK is suitable for demonstration and testing, not production distribution.

## 17. Notes for Evaluators

- Run the backend and set its two local environment values before registering or signing in.
- Use `10.0.2.2` for the standard Android Studio emulator. For a physical device, use a reachable computer LAN IP and verify `/api/health` from the phone.
- The app's API URL is a source constant, so an APK must be built with a URL reachable from the evaluator's network. A private development LAN address cannot serve unrelated networks.
- Use the operator status procedure above to demonstrate Confirmed and Completed states; refresh the app to fetch the changes.
- Local Android Room data remains on the device, while backend demo data is stored separately in `backend/data/store.json`.
