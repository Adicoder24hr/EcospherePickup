const mongoose = require("mongoose");

const bookingSchema = new mongoose.Schema({
    id: { type: String, required: true, unique: true },
    userId: { type: String, required: true },
    customerName: { type: String, required: true, trim: true },
    mobile: { type: String, required: true, trim: true },
    category: { type: String, required: true, enum: ["Plastic", "Paper", "E-waste", "General Waste"] },
    address: { type: String, required: true, trim: true },
    pickupDate: { type: String, required: true },
    pickupTime: { type: String, required: true },
    status: { type: String, required: true, enum: ["Pending", "Confirmed", "Completed", "Cancelled"], default: "Pending" },
    createdAt: { type: String, required: true, default: () => new Date().toISOString() }
}, {
    collection: "bookings",
    versionKey: false
});

bookingSchema.index({ userId: 1, createdAt: -1 });

module.exports = mongoose.model("Booking", bookingSchema);
