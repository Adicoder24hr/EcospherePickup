require("dotenv").config();

const fs = require("fs");
const path = require("path");
const mongoose = require("mongoose");
const { connectDatabase } = require("../config/database");
const User = require("../models/User");
const Booking = require("../models/Booking");

async function importLocalStore() {
    if (process.argv[2] !== "--apply") {
        throw new Error("Pass --apply to import local JSON records into MongoDB Atlas");
    }

    const storePath = process.env.ECOSPHERE_DATA_FILE || path.join(__dirname, "..", "data", "store.json");
    if (!fs.existsSync(storePath)) {
        console.log("No local JSON store found; nothing to import.");
        return;
    }

    const store = JSON.parse(fs.readFileSync(storePath, "utf8"));
    const users = Array.isArray(store.users) ? store.users : [];
    const bookings = Array.isArray(store.bookings) ? store.bookings : [];
    const eligibleUserIds = new Set();
    let insertedUsers = 0;
    let skippedUsers = 0;
    let insertedBookings = 0;
    let skippedBookings = 0;

    await connectDatabase();
    await Promise.all([User.init(), Booking.init()]);

    for (const user of users) {
        if (!user.id || !user.email || !user.mobile || !user.passwordHash) {
            skippedUsers++;
            continue;
        }
        const existingId = await User.findOne({ id: user.id }).lean();
        if (existingId) {
            eligibleUserIds.add(user.id);
            skippedUsers++;
            continue;
        }

        const conflictingIdentity = await User.findOne({
            $or: [{ email: user.email }, { mobile: user.mobile }]
        }).lean();
        if (conflictingIdentity) {
            skippedUsers++;
            continue;
        }

        try {
            await User.create(user);
            eligibleUserIds.add(user.id);
            insertedUsers++;
        } catch (error) {
            if (error.code !== 11000) throw error;
            const inserted = await User.findOne({ id: user.id }).lean();
            if (inserted) eligibleUserIds.add(user.id);
            skippedUsers++;
        }
    }

    for (const booking of bookings) {
        if (!booking.id || !booking.userId) {
            skippedBookings++;
            continue;
        }
        if (!eligibleUserIds.has(booking.userId)) {
            const owner = await User.exists({ id: booking.userId });
            if (!owner) {
                skippedBookings++;
                continue;
            }
        }

        const existingBooking = await Booking.findOne({ id: booking.id }).lean();
        if (existingBooking) {
            skippedBookings++;
            continue;
        }

        try {
            await Booking.create(booking);
            insertedBookings++;
        } catch (error) {
            if (error.code !== 11000) throw error;
            skippedBookings++;
        }
    }

    console.log(JSON.stringify({ insertedUsers, skippedUsers, insertedBookings, skippedBookings }));
}

importLocalStore()
    .catch((error) => {
        console.error("Local data import failed:", error.name || "Error", error.code || "");
        process.exitCode = 1;
    })
    .finally(async () => {
        await mongoose.disconnect();
    });
