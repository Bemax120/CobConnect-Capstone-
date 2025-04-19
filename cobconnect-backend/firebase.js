const admin = require("firebase-admin");
const serviceAccount = require("./cobconnectapp-firebase-adminsdk-6p905-5d17a7f8cf.json"); // Ensure the correct filename

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

module.exports = admin;
