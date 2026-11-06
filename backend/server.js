require("dotenv").config();

const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { randomUUID, timingSafeEqual } = require("crypto");
const fs = require("fs");
const path = require("path");

const app = express();

app.use(express.json());

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET;

const storePath = process.env.ECOSPHERE_DATA_FILE || path.join(__dirname, "data", "store.json");

function loadStore() {
    try {
        const store = JSON.parse(fs.readFileSync(storePath, "utf8"));
        return {
            users: Array.isArray(store.users) ? store.users : [],
            bookings: Array.isArray(store.bookings) ? store.bookings : []
        };
    } catch (error) {
        if (error.code === "ENOENT") return { users: [], bookings: [] };
        throw new Error(`Unable to read Ecosphere data store: ${error.message}`);
    }
}

const { users, bookings } = loadStore();

function saveStore() {
    fs.mkdirSync(path.dirname(storePath), { recursive: true });
    const temporaryPath = `${storePath}.tmp`;
    fs.writeFileSync(temporaryPath, JSON.stringify({ users, bookings }, null, 2), { mode: 0o600 });
    fs.renameSync(temporaryPath, storePath);
}

const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

const mobileRegex = /^\+?[0-9]{10,15}$/;

function generateToken(user){
return jwt.sign(
    { sub: user.id },
    JWT_SECRET,
    { expiresIn: "7d" }
);
}

function verifyToken(req, res, next) {

    const authHeader = req.headers.authorization;

    if (!authHeader?.startsWith("Bearer ")) {
        return res.status(401).json({
            message: "Authentication token required"
        });
    }

    const token = authHeader.split(" ")[1];

    try {
        const decoded = jwt.verify(token, JWT_SECRET);

        const user = users.find(
            item => item.id === decoded.sub
        );

        if (!user) {
            return res.status(401).json({
                message: "User session is invalid"
            });
        }

        req.user = user;
        next();

    } catch (error) {
        return res.status(401).json({
            message: "Invalid or expired token"
        });
    }
}

function publicUser(user){
    return {
        id: user.id,
        name: user.name,
        email: user.email,
        mobile: user.mobile,
    };
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
    next();
}

//   health check
app.get("/api/health", (req, res) => {
    res.json({
    success: true,
    message: "Server is up and running",
    });

});

// register API
app.post("/api/auth/register", async (req, res) => {

    try{

        const { name, email, mobile, password } = req.body;

        const cleanName = String(name || "").trim();
        const cleanEmail = String(email || "").trim().toLowerCase();
        const cleanMobile = String(mobile || "").trim();
        const cleanPassword = String(password || "");

                if (
                    cleanName.length < 2 ||
                    !emailRegex.test(cleanEmail) ||
                    !mobileRegex.test(cleanMobile) ||
                    cleanPassword.length < 8
                ) {
                    return res.status(400).json({
                        message: "Please provide valid registration details"
                    });
                }

        const existingUser = users.find(
        user =>
            user.email === cleanEmail ||
            user.mobile === cleanMobile

        );

        if(existingUser){
        return res.status(409).json({
        message: "Email or mobile number already registered"

        });

        }

        const passwordHash = await bcrypt.hash(cleanPassword, 10);

        const user = {
            id: randomUUID(),
            name: cleanName,
            email: cleanEmail,
            mobile: cleanMobile,
            passwordHash
        }

        users.push(user);
        saveStore();

        const token = generateToken(user);

        res.status(201).json({
        message: "User registered successfully",
        user: publicUser(user),
        token

        });

    }catch(error){
        console.log("Registration error: ", error);

        return res.status(500).json({
            message: "Internal server error"
        });
    }

});

// login API
app.post("/api/auth/login", async (req, res) => {

    try {
        const { emailOrMobile, password } = req.body;

        const identity = String(
            emailOrMobile || ""
        ).trim();

        const cleanPassword = String(password || "");

        if (!identity || !cleanPassword) {
            return res.status(400).json({
                message: "Email/mobile and password are required"
            });
        }

        const user = users.find(
            item =>
                item.email === identity.toLowerCase() ||
                item.mobile === identity
        );

        if (!user) {
            return res.status(401).json({
                message: "Invalid credentials"
            });
        }

        const passwordMatches = await bcrypt.compare(
            cleanPassword,
            user.passwordHash
        );

        if (!passwordMatches) {
            return res.status(401).json({
                message: "Invalid credentials"
            });
        }

        const token = generateToken(user);

        return res.status(200).json({
            message: "Login successful",
            token,
            user: publicUser(user)
        });

    } catch (error) {
        console.error("Login error:", error);

        return res.status(500).json({
            message: "Internal server error"
        });
    }
});


const validCategories = [
    "Plastic",
    "Paper",
    "E-waste",
    "General Waste"
];

// create new booking
app.post("/api/bookings", verifyToken, (req, res) => {

    const {
        id,
        customerName,
        mobile,
        category,
        address,
        pickupDate,
        pickupTime
    } = req.body;

    if (
        !customerName?.trim() ||
        !mobile?.trim() ||
        !validCategories.includes(category) ||
        !address?.trim() ||
        !pickupDate ||
        !pickupTime
    ) {
        return res.status(400).json({
            message: "Please provide all valid booking details"
        });
    }

    const bookingId = id || randomUUID();
    const existingBooking = bookings.find(item => item.id === bookingId && item.userId === req.user.id);
    if (existingBooking) return res.status(200).json(existingBooking);

    const booking = {
        id: bookingId,
        userId: req.user.id,
        customerName: customerName.trim(),
        mobile: mobile.trim(),
        category,
        address: address.trim(),
        pickupDate,
        pickupTime,
        status: "Pending",
        createdAt: new Date().toISOString()
    };

    bookings.push(booking);
    saveStore();

    return res.status(201).json(booking);
});


// GET ALL BOOKINGS FOR LOGGED-IN USER
app.get("/api/bookings", verifyToken, (req, res) => {

    const userBookings = bookings
        .filter(item => item.userId === req.user.id)
        .sort((a, b) =>
            b.createdAt.localeCompare(a.createdAt)
        );

    res.json(userBookings);
});


// GET BOOKING DETAILS
app.get("/api/bookings/:id", verifyToken, (req, res) => {

    const booking = bookings.find(
        item =>
            item.id === req.params.id &&
            item.userId === req.user.id
    );

    if (!booking) {
        return res.status(404).json({
            message: "Booking not found"
        });
    }

    res.json(booking);
});


// CANCEL BOOKING
app.patch(
    "/api/bookings/:id/cancel",
    verifyToken,
    (req, res) => {

        const booking = bookings.find(
            item =>
                item.id === req.params.id &&
                item.userId === req.user.id
        );

        if (!booking) {
            return res.status(404).json({
                message: "Booking not found"
            });
        }

        if (!["Pending", "Confirmed"].includes(booking.status)) {
            return res.status(400).json({
                message: "This booking cannot be cancelled"
            });
        }

        booking.status = "Cancelled";
        saveStore();

        res.json(booking);
    }
);

app.patch("/api/admin/bookings/:id/status", requireAdmin, (req, res) => {
    const { status } = req.body || {};
    if (!["Confirmed", "Completed"].includes(status)) {
        return res.status(400).json({ message: "Status must be Confirmed or Completed" });
    }
    const booking = bookings.find(item => item.id === req.params.id);
    if (!booking) return res.status(404).json({ message: "Booking not found" });
    const allowed = booking.status === "Pending" && status === "Confirmed" ||
        booking.status === "Confirmed" && status === "Completed";
    if (!allowed) {
        return res.status(409).json({ message: `Cannot change status from ${booking.status} to ${status}` });
    }
    booking.status = status;
    saveStore();
    return res.json(booking);
});


// for handling unknown routes
app.use((req, res)=> {
    res.status(404).json({
        message: "Endpoint not found"
    })
})

app.listen(PORT, "0.0.0.0", ()=>{

    console.log(`Ecosphere server running on port ${PORT}`);
});
