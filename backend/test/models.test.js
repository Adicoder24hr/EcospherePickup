const test = require("node:test");
const assert = require("node:assert/strict");
const User = require("../models/User");
const Booking = require("../models/Booking");

test("user model preserves UUID and public profile fields", async () => {
    const user = new User({
        id: "user-uuid",
        name: "Sample Customer",
        email: "sample@example.com",
        mobile: "+919876543210",
        passwordHash: "bcrypt-hash"
    });

    await user.validate();
    assert.equal(User.collection.collectionName, "users");
    assert.equal(User.schema.path("id").options.unique, true);
    assert.equal(User.schema.path("email").options.unique, true);
    assert.equal(User.schema.path("mobile").options.unique, true);
});

test("booking model accepts the existing API fields and status values", async () => {
    const booking = new Booking({
        id: "booking-uuid",
        userId: "user-uuid",
        customerName: "Sample Customer",
        mobile: "+919876543210",
        category: "Plastic",
        address: "1 Sample Road",
        pickupDate: "2026-11-12",
        pickupTime: "10:00 AM"
    });

    await booking.validate();
    assert.equal(booking.status, "Pending");
    assert.equal(typeof booking.createdAt, "string");
    assert.equal(Booking.collection.collectionName, "bookings");
    assert.equal(Booking.schema.path("id").options.unique, true);
});

test("booking model rejects unsupported statuses", async () => {
    const booking = new Booking({
        id: "booking-uuid",
        userId: "user-uuid",
        customerName: "Sample Customer",
        mobile: "+919876543210",
        category: "Plastic",
        address: "1 Sample Road",
        pickupDate: "2026-11-12",
        pickupTime: "10:00 AM",
        status: "Queued"
    });

    await assert.rejects(booking.validate(), (error) => error.errors.status.kind === "enum");
});
