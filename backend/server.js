require("dotenv").config();

const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { randomUUID, timingSafeEqual } = require("crypto");
const mongoose = require("mongoose");
const { connectDatabase } = require("./config/database");
const User = require("./models/User");
const Booking = require("./models/Booking");

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET;

app.use(express.json());

const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const mobileRegex = /^\+?[0-9]{10,15}$/;
const validCategories = ["Plastic", "Paper", "E-waste", "General Waste"];

function generateToken(user) {
    return jwt.sign({ sub: user.id }, JWT_SECRET, { expiresIn: "7d" });
}

function publicUser(user) {
    return {
        id: user.id,
        name: user.name,
        email: user.email,
        mobile: user.mobile
    };
}

function publicBooking(booking) {
    return {
        id: booking.id,
        userId: booking.userId,
        customerName: booking.customerName,
        mobile: booking.mobile,
        category: booking.category,
        address: booking.address,
        pickupDate: booking.pickupDate,
        pickupTime: booking.pickupTime,
        status: booking.status,
        createdAt: booking.createdAt
    };
}

async function verifyToken(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith("Bearer ")) {
        return res.status(401).json({ message: "Authentication token required" });
    }

    try {
        const decoded = jwt.verify(authHeader.split(" ")[1], JWT_SECRET);
        const user = await User.findOne({ id: decoded.sub }).lean();
        if (!user) return res.status(401).json({ message: "User session is invalid" });
        req.user = user;
        return next();
    } catch (error) {
        if (error.name === "JsonWebTokenError" || error.name === "TokenExpiredError") {
            return res.status(401).json({ message: "Invalid or expired token" });
        }
        return next(error);
    }
}

function requireAdmin(req, res, next) {
    const configuredKey = process.env.ADMIN_API_KEY;
    const suppliedKey = req.get("x-admin-api-key") || "";
    if (!configuredKey) {
        return res.status(503).json({ message: "Administrator updates are not configured" });
    }

    const expected = Buffer.from(configuredKey);
    const supplied = Buffer.from(suppliedKey);
    if (expected.length !== supplied.length || !timingSafeEqual(expected, supplied)) {
        return res.status(401).json({ message: "Administrator API key is invalid" });
    }
    return next();
}

function healthResponse(req, res) {
    const connected = mongoose.connection.readyState === 1;
    const status = connected ? 200 : 503;
    return res.status(status).json({
        success: connected,
        status: connected ? "ok" : "unavailable",
        database: connected ? "connected" : "disconnected",
        message: connected ? "Server is up and running" : "Database is unavailable"
    });
}

app.get("/health/live", (req, res) => res.json({ status: "ok" }));
app.get("/health", healthResponse);
app.get("/api/health", healthResponse);

app.post("/api/auth/register", async (req, res) => {
    const cleanName = String(req.body?.name || "").trim();
    const cleanEmail = String(req.body?.email || "").trim().toLowerCase();
    const cleanMobile = String(req.body?.mobile || "").trim();
    const cleanPassword = String(req.body?.password || "");

    if (cleanName.length < 2 || !emailRegex.test(cleanEmail) ||
        !mobileRegex.test(cleanMobile) || cleanPassword.length < 8) {
        return res.status(400).json({ message: "Please provide valid registration details" });
    }

    const existingUser = await User.findOne({
        $or: [{ email: cleanEmail }, { mobile: cleanMobile }]
    }).lean();
    if (existingUser) {
        return res.status(409).json({ message: "Email or mobile number already registered" });
    }

    const user = await User.create({
        id: randomUUID(),
        name: cleanName,
        email: cleanEmail,
        mobile: cleanMobile,
        passwordHash: await bcrypt.hash(cleanPassword, 10)
    });

    return res.status(201).json({
        message: "User registered successfully",
        user: publicUser(user),
        token: generateToken(user)
    });
});

app.post("/api/auth/login", async (req, res) => {
    const identity = String(req.body?.emailOrMobile || "").trim();
    const cleanPassword = String(req.body?.password || "");
    if (!identity || !cleanPassword) {
        return res.status(400).json({ message: "Email/mobile and password are required" });
    }

    const user = await User.findOne({
        $or: [{ email: identity.toLowerCase() }, { mobile: identity }]
    }).lean();
    if (!user || !(await bcrypt.compare(cleanPassword, user.passwordHash))) {
        return res.status(401).json({ message: "Invalid credentials" });
    }

    return res.status(200).json({
        message: "Login successful",
        token: generateToken(user),
        user: publicUser(user)
    });
});

app.post("/api/bookings", verifyToken, async (req, res) => {
    const customerName = String(req.body?.customerName || "").trim();
    const mobile = String(req.body?.mobile || "").trim();
    const { id, category, address, pickupDate, pickupTime } = req.body || {};
    const cleanAddress = String(address || "").trim();

    if (!customerName || !mobile || !validCategories.includes(category) ||
        !cleanAddress || !pickupDate || !pickupTime) {
        return res.status(400).json({ message: "Please provide all valid booking details" });
    }

    const bookingId = id || randomUUID();
    const existingBooking = await Booking.findOne({ id: bookingId, userId: req.user.id }).lean();
    if (existingBooking) return res.status(200).json(publicBooking(existingBooking));

    const booking = await Booking.create({
        id: bookingId,
        userId: req.user.id,
        customerName,
        mobile,
        category,
        address: cleanAddress,
        pickupDate,
        pickupTime,
        status: "Pending",
        createdAt: new Date().toISOString()
    });
    return res.status(201).json(publicBooking(booking));
});

app.get("/api/bookings", verifyToken, async (req, res) => {
    const bookings = await Booking.find({ userId: req.user.id })
        .sort({ createdAt: -1 })
        .lean();
    return res.json(bookings.map(publicBooking));
});

app.get("/api/bookings/:id", verifyToken, async (req, res) => {
    const booking = await Booking.findOne({ id: req.params.id, userId: req.user.id }).lean();
    if (!booking) return res.status(404).json({ message: "Booking not found" });
    return res.json(publicBooking(booking));
});

app.patch("/api/bookings/:id/cancel", verifyToken, async (req, res) => {
    const booking = await Booking.findOneAndUpdate(
        { id: req.params.id, userId: req.user.id, status: { $in: ["Pending", "Confirmed"] } },
        { $set: { status: "Cancelled" } },
        { new: true, runValidators: true }
    ).lean();

    if (booking) return res.json(publicBooking(booking));
    const existingBooking = await Booking.findOne({ id: req.params.id, userId: req.user.id }).lean();
    if (!existingBooking) return res.status(404).json({ message: "Booking not found" });
    return res.status(400).json({ message: "This booking cannot be cancelled" });
});

app.patch("/api/admin/bookings/:id/status", requireAdmin, async (req, res) => {
    const { status } = req.body || {};
    if (!["Confirmed", "Completed"].includes(status)) {
        return res.status(400).json({ message: "Status must be Confirmed or Completed" });
    }

    const booking = await Booking.findOne({ id: req.params.id });
    if (!booking) return res.status(404).json({ message: "Booking not found" });
    const allowed = (booking.status === "Pending" && status === "Confirmed") ||
        (booking.status === "Confirmed" && status === "Completed");
    if (!allowed) {
        return res.status(409).json({ message: `Cannot change status from ${booking.status} to ${status}` });
    }

    booking.status = status;
    await booking.save();
    return res.json(publicBooking(booking));
});

app.use((req, res) => res.status(404).json({ message: "Endpoint not found" }));

app.use((error, req, res, next) => {
    if (res.headersSent) return next(error);
    if (error?.code === 11000) {
        const message = error.keyPattern?.email || error.keyPattern?.mobile
            ? "Email or mobile number already registered"
            : "A record with this identifier already exists";
        return res.status(409).json({ message });
    }
    if (error instanceof mongoose.Error.ValidationError || error instanceof mongoose.Error.CastError) {
        return res.status(400).json({ message: "Please provide valid data" });
    }
    if (error.name === "MongooseServerSelectionError" ||
        error.name === "MongoNetworkError" ||
        error.name === "MongoServerSelectionError") {
        return res.status(503).json({ message: "Database is temporarily unavailable" });
    }

    console.error("Request failed:", error.name || "Error", error.code || "");
    return res.status(500).json({ message: "Internal server error" });
});

async function startServer() {
    if (!JWT_SECRET) throw new Error("JWT_SECRET is required");
    await connectDatabase();
    await Promise.all([User.init(), Booking.init()]);
    const server = app.listen(PORT, "0.0.0.0", () => {
        console.log(`Ecosphere server listening on port ${PORT}`);
    });

    const shutdown = (signal) => {
        console.log(`${signal} received; closing server`);
        server.close(async () => {
            await mongoose.disconnect();
            process.exit(0);
        });
    };
    process.once("SIGINT", () => shutdown("SIGINT"));
    process.once("SIGTERM", () => shutdown("SIGTERM"));
    return server;
}

if (require.main === module) {
    startServer().catch((error) => {
        console.error("Backend startup failed:", error.name || "Error", error.code || "");
        process.exit(1);
    });
}

module.exports = { app, startServer };
