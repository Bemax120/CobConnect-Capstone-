const express = require("express");
const admin = require("./firebase"); // Import Firebase configuration
const cors = require("cors");
const bodyParser = require("body-parser");

const app = express();
app.use(cors());
app.use(bodyParser.json());

// API to send notifications
app.post("/send-notification", async (req, res) => {
  const { token, title, body } = req.body;

  if (!token || !title || !body) {
    return res.status(400).json({ error: "Missing required fields" });
  }

  const message = {
    token: token,
    notification: {
      title: title,
      body: body,
    },
  };

  try {
    await admin.messaging().send(message);
    res.status(200).json({ success: "Notification sent successfully!" });
  } catch (error) {
    console.error("Error sending notification:", error);
    res.status(500).json({ error: "Failed to send notification" });
  }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
