require("dotenv").config();
const express = require("express");
const cors = require("cors");
const stripe = require("stripe")(process.env.STRIPE_SECRET_KEY);

const app = express();

app.use(express.json());
app.use(cors()); // Allows requests from your mobile app

// Endpoint to create PaymentIntent
app.post("/create-payment-intent", async (req, res) => {
    try {
        const { amount } = req.body; // Receive amount from the mobile app

        // Create a new Stripe customer
        const customer = await stripe.customers.create();

        // Generate an ephemeral key for the customer
        const ephemeralKey = await stripe.ephemeralKeys.create(
            { customer: customer.id },
            { apiVersion: "2023-10-16" }
        );

        // Create a PaymentIntent with the specified amount and customer
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount, // Amount in cents
            currency: "usd",
            customer: customer.id,
        });

        // Send the client secret, customer ID, and ephemeral key to the frontend
        res.json({
            clientSecret: paymentIntent.client_secret,
            customerId: customer.id,
            ephemeralKey: ephemeralKey.secret,
        });
    } catch (error) {
        res.status(500).send({ error: error.message });
    }
});

// Start the server
app.listen(3000, () => console.log("Server running on port 3000"));
