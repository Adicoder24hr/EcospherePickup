# Ecosphere Waste Pickup

Ecosphere is a mobile app for booking household waste pickups and tracking their status. This repository contains the Android application and a local Express API used for the assignment demo.

Android is implemented with Kotlin and XML. iOS is not implemented; an iOS version would use Swift and SwiftUI.

## Features

- Register with a name, email address, mobile number, and password. Sign in with email or mobile number.
- View the customer dashboard, booking totals, and recent bookings.
- Create pickups for Plastic, Paper, E-waste, or General Waste.
- View booking history and details, including the pickup date, time, address, and customer contact information.
- Track Pending, Confirmed, Completed, and Cancelled statuses. Customers can cancel Pending or Confirmed bookings.
- Refresh the dashboard, history, and booking details from the API.
- Store bookings in Room and associate local records with the signed-in user.
- Show consistent loading, success, error, and confirmation dialogs.

## Technology

- Android: Kotlin, XML layouts, ViewBinding, Material Components, Navigation Component, MVVM, StateFlow, coroutines
- Network: Retrofit, OkHttp, Gson, JWT authentication
- Local storage: Room; the JWT is encrypted using Android Keystore-backed AES-GCM
- Backend: Node.js, Express, bcrypt password hashes, and a local JSON data file
- Dependency injection: the project's manual `AppContainer` and ViewModel factories

## Requirements

- Android Studio with Android SDK 36 (including API 36.1 support) and JDK 21
- Node.js 20 or newer and npm
- An Android emulator or device

## Run the backend first

The Android app needs the API to be running for registration, login, and remote booking synchronization. Start the backend before launching the app.

In a PowerShell terminal at the repository root:

```powershell
cd backend
npm install
Copy-Item .env.example .env
notepad .env
```

Set `JWT_SECRET` and `ADMIN_API_KEY` in `.env` to separate, private values. Keep this file local; it is ignored by Git. Then start the server and leave this terminal open:

```powershell
npm start
```

The server listens on port 3000 by default. Confirm it is running from another terminal:

```powershell
Invoke-RestMethod http://localhost:3000/api/health
```

The mock API stores account records and bookings in `backend/data/store.json`. Passwords are stored as bcrypt hashes. This file is created when data is saved and is excluded from Git.

## Run the Android app

1. Start the backend using the steps above.
2. Open the repository root in Android Studio and allow Gradle sync to finish.
3. Select an emulator or connected device and run the `app` configuration.

The default API URL in `app/src/main/java/com/ecosphere/wastepickup/core/network/ApiClient.kt` is `http://10.0.2.2:3000/api/`, which reaches the development computer from the standard Android Studio emulator. For a physical device, change it to the development computer's reachable LAN address, keeping the `/api/` suffix. The device and computer must be able to reach each other over the network.

To build a debug APK from PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Typical app flow

1. Create an account on Registration, then sign in from Login.
2. Create a booking with its waste category, pickup address, date, and time.
3. Open the booking from the Dashboard or Booking History to view its details.
4. Pull down on the Dashboard, History, or Details screen to refresh from the API.
5. Cancel a Pending or Confirmed booking from its Details screen if needed.

Booking references such as `ECO-A12B34C5` are for display. The app and API retain the full UUID as the booking identifier.

## Booking statuses for the demo

New bookings start as `Pending`. The service operator can move a booking from Pending to Confirmed and from Confirmed to Completed using the administrator endpoint. Customers cannot set these statuses themselves. The endpoint requires the `ADMIN_API_KEY` from the backend's local `.env` file.

After signing in, list that customer's bookings to get the internal booking UUID:

```powershell
$credentials = @{ emailOrMobile = "customer@example.com"; password = "<PASSWORD>" } | ConvertTo-Json
$session = Invoke-RestMethod -Method Post -Uri "http://localhost:3000/api/auth/login" -ContentType "application/json" -Body $credentials
$headers = @{ Authorization = "Bearer $($session.token)" }
$bookings = Invoke-RestMethod -Method Get -Uri "http://localhost:3000/api/bookings" -Headers $headers
$bookingId = $bookings[0].id
```

Update the status using the operator key. Run this once with `Confirmed`, then again with `Completed`:

```powershell
$adminHeaders = @{ "x-admin-api-key" = "<ADMIN_API_KEY>" }
$body = @{ status = "Confirmed" } | ConvertTo-Json
Invoke-RestMethod -Method Patch -Uri "http://localhost:3000/api/admin/bookings/$bookingId/status" -Headers $adminHeaders -ContentType "application/json" -Body $body
```

Pull to refresh in the app to see the updated status. The API rejects unsupported status transitions.

## Architecture and data

The Android code uses a feature-first structure:

- `auth/` contains the login and registration UI, ViewModels, API models, and repository.
- `home/` contains the Dashboard UI and ViewModel.
- `booking/` contains booking creation, history, details, the shared RecyclerView adapter, API models, Room data, and repository.
- `core/` contains network setup, Room configuration, token security, shared dialogs, and manual dependency injection.
- `backend/` contains the Express API.

Fragments render state and handle input/navigation. ViewModels own screen state and validation. Repositories coordinate Retrofit and Room. The backend scopes booking requests to the authenticated JWT user, and Android filters Room records by the signed-in user ID.

Room stores each booking's full UUID, owner ID, contact and pickup details, status, sync state, and creation time. A safe Room migration adds the owner field to existing databases. The API's JSON store keeps users and bookings across backend restarts for local demonstration; it is not intended for production or multi-server use.

## API routes

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/health` | Check the backend |
| `POST` | `/api/auth/register` | Create an account |
| `POST` | `/api/auth/login` | Sign in and receive a JWT |
| `POST` | `/api/bookings` | Create an authenticated user's booking |
| `GET` | `/api/bookings` | List the authenticated user's bookings |
| `GET` | `/api/bookings/:id` | Get one of the authenticated user's bookings |
| `PATCH` | `/api/bookings/:id/cancel` | Cancel an eligible booking |
| `PATCH` | `/api/admin/bookings/:id/status` | Operator status update using `ADMIN_API_KEY` |

## Assignment scope and limitations

- The Android application is the completed platform. The assignment permits one platform when time is limited; iOS would be approached with Swift and SwiftUI.
- The Express API uses a local JSON file to support this demo. A production service should use a managed database, HTTPS, and a full administrator identity/authorization system.
- Accounts created with an earlier in-memory version of the backend cannot be recovered if that server process has already restarted; register those accounts again once.
- Cleartext HTTP is enabled for local development only. Do not deploy this configuration as a public service.
- Automated UI coverage and a published Play Store/iOS release are not included.
- Screenshots or a demonstration video are not included; capture these from the running app for the assignment submission.
