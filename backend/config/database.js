const mongoose = require("mongoose");

async function connectDatabase() {
    const uri = process.env.MONGODB_URI || process.env.MONGO_URI;
    if (!uri) throw new Error("MONGODB_URI is required");

    try {
        await mongoose.connect(uri, {
            serverSelectionTimeoutMS: 10000,
            maxPoolSize: 10
        });
        console.log("MongoDB connection established");
    } catch (error) {
        console.error("MongoDB connection failed:", error.name || "Error", error.code || "");
        throw error;
    }
}

module.exports = { connectDatabase };
