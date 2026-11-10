const mongoose = require("mongoose");

const userSchema = new mongoose.Schema({
    id: { type: String, required: true, unique: true },
    name: { type: String, required: true, minlength: 2, trim: true },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    mobile: { type: String, required: true, unique: true, trim: true },
    passwordHash: { type: String, required: true }
}, {
    collection: "users",
    versionKey: false
});

module.exports = mongoose.model("User", userSchema);
