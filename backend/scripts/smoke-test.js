require("dotenv").config();

const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const net = require("node:net");
const { randomUUID } = require("node:crypto");
const mongoose = require("mongoose");
const { connectDatabase } = require("../config/database");
const User = require("../models/User");
const Booking = require("../models/Booking");

const adminKey = randomUUID();
const customerId = randomUUID();
const email = `smoke-${customerId}@example.invalid`;
const mobile = `+1${customerId.replace(/\D/g, "").slice(0, 10).padEnd(10, "7")}`;
const password = `Smoke-${randomUUID()}-A1`;
const firstBookingId = randomUUID();
const secondBookingId = randomUUID();
let serverProcess;
let port;
let testUserId;

async function findAvailablePort() {
    const server = net.createServer();
    await new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    const availablePort = server.address().port;
    await new Promise((resolve) => server.close(resolve));
    return availablePort;
}

function startServer() {
    serverProcess = spawn(process.execPath, ["server.js"], {
        cwd: process.cwd(),
        env: { ...process.env, PORT: String(port), ADMIN_API_KEY: adminKey },
        stdio: "ignore"
    });
    serverProcess.once("error", (error) => {
        console.error("Smoke test could not start the server:", error.name);
    });
}

async function stopServer() {
    if (!serverProcess || serverProcess.exitCode !== null) return;
    const child = serverProcess;
    await new Promise((resolve) => {
        child.once("exit", resolve);
        child.kill();
    });
    serverProcess = undefined;
}

async function waitUntilReady() {
    const deadline = Date.now() + 20000;
    while (Date.now() < deadline) {
        if (serverProcess.exitCode !== null) throw new Error("Backend exited before becoming ready");
        try {
            const response = await fetch(`http://127.0.0.1:${port}/health`);
            if (response.ok && (await response.json()).database === "connected") return;
        } catch (_) {
            await new Promise((resolve) => setTimeout(resolve, 250));
        }
    }
    throw new Error("Backend did not become ready in time");
}

async function request(route, { method = "GET", body, token, admin = false, expectedStatus = 200 } = {}) {
    const headers = { "content-type": "application/json" };
    if (token) headers.authorization = `Bearer ${token}`;
    if (admin) headers["x-admin-api-key"] = adminKey;
    const response = await fetch(`http://127.0.0.1:${port}${route}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    const result = await response.json();
    assert.equal(response.status, expectedStatus, `${method} ${route} returned an unexpected status`);
    return result;
}

async function createBooking(token, id) {
    return request("/api/bookings", {
        method: "POST",
        token,
        expectedStatus: 201,
        body: {
            id,
            customerName: "Smoke Test Customer",
            mobile,
            category: "Paper",
            address: "1 Smoke Test Road",
            pickupDate: "2026-11-12",
            pickupTime: "10:00 AM"
        }
    });
}

async function runSmokeTest() {
    if (!process.env.MONGODB_URI && !process.env.MONGO_URI) {
        throw new Error("Set MONGODB_URI in backend/.env before running this test");
    }

    port = await findAvailablePort();
    startServer();
    await waitUntilReady();

    const registration = await request("/api/auth/register", {
        method: "POST",
        expectedStatus: 201,
        body: { name: "Smoke Test Customer", email, mobile, password }
    });
    const token = registration.token;
    testUserId = registration.user.id;
    assert.ok(registration.user.id);
    assert.equal(registration.user.email, email);

    const login = await request("/api/auth/login", {
        method: "POST",
        expectedStatus: 200,
        body: { emailOrMobile: email, password }
    });
    assert.equal(login.user.id, registration.user.id);

    const firstBooking = await createBooking(token, firstBookingId);
    assert.equal(firstBooking.id, firstBookingId);
    assert.equal(firstBooking.userId, registration.user.id);
    assert.equal(firstBooking.status, "Pending");
    assert.equal((await request("/api/bookings", { token })).length, 1);
    assert.equal((await request(`/api/bookings/${firstBookingId}`, { token })).id, firstBookingId);

    const cancelled = await request(`/api/bookings/${firstBookingId}/cancel`, {
        method: "PATCH", token
    });
    assert.equal(cancelled.status, "Cancelled");

    await createBooking(token, secondBookingId);
    assert.equal((await request(`/api/admin/bookings/${secondBookingId}/status`, {
        method: "PATCH", admin: true, body: { status: "Confirmed" }
    })).status, "Confirmed");
    assert.equal((await request(`/api/admin/bookings/${secondBookingId}/status`, {
        method: "PATCH", admin: true, body: { status: "Completed" }
    })).status, "Completed");
    await request(`/api/admin/bookings/${secondBookingId}/status`, {
        method: "PATCH", admin: true, body: { status: "Confirmed" }, expectedStatus: 409
    });

    await stopServer();
    startServer();
    await waitUntilReady();
    const persisted = await request(`/api/bookings/${secondBookingId}`, { token });
    assert.equal(persisted.status, "Completed");
    assert.equal((await request("/api/bookings", { token })).length, 2);

    console.log("API smoke test passed: authentication, booking CRUD, cancellation, admin transitions, and persistence after restart.");
}

runSmokeTest()
    .catch((error) => {
        console.error("API smoke test failed:", error.message);
        process.exitCode = 1;
    })
    .finally(async () => {
        await stopServer();
        try {
            await connectDatabase();
            await Booking.deleteMany({ id: { $in: [firstBookingId, secondBookingId] } });
            if (testUserId) await User.deleteOne({ id: testUserId });
        } catch (error) {
            console.error("Smoke test cleanup failed:", error.name || "Error", error.code || "");
            process.exitCode = 1;
        } finally {
            await mongoose.disconnect();
        }
    });
