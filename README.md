# Healthcare Appointment & Subscription Management System

Welcome to the **Healthcare Appointment & Subscription Management System** repository. This project is a hybrid micro-service system featuring a high-performance Spring Boot REST API backend and an asynchronous Python background worker driven by Apache Kafka events.

---

## 🏗️ System Architecture

The application is structured into two main components:
1. **Java REST API (Backend)**: Built with Spring Boot, Spring Security (JWT), and Spring Data JPA. It handles HTTP requests, manages transactions, interacts with Postgres, and publishes events to Apache Kafka.
2. **Python Event Consumer (Background Worker)**: Subscribes to Kafka topics (`appointment-events` and `notification-events`) to handle heavy/delayed background operations like sending verification and welcome emails and updating notification delivery states in Postgres.

```
┌─────────────────────────────────┐
│     Postman / API Clients       │
└────────────────┬────────────────┘
                 │ (HTTP REST APIs)
                 ▼
┌─────────────────────────────────┐
│       Spring Boot Backend       │◄───────┐
└────────┬───────────────┬────────┘        │
         │ (JDBC)        │ (Events)        │
         ▼               ▼                 │ (JDBC Status Update)
┌────────┴───────┐     ┌─┴────────┐        │
│ Neon Postgres  │     │  Apache  │        │
│   Database     │     │  Kafka   │        │
└────────────────┘     └─┬────────┘        │
                         │ (Subscribe)     │
                         ▼                 │
                       ┌───────────────────┴┐
                       │   Python Worker    ├──────► [ SMTP Mail Service ]
                       └────────────────────┘
```

---

## 🛠️ Technology Stack

- **Backend core**: Java 17, Spring Boot 3.5.x, Maven
- **Database**: PostgreSQL (Neon Serverless Database)
- **Security**: Spring Security + JSON Web Token (JWT)
- **Messaging**: Apache Kafka (Aiven Managed Kafka)
- **Background Worker**: Python 3.10+, Confluent Kafka Client, Psycopg2
- **Email Delivery**: SMTP (Mailtrap sandbox / Google SMTP)
- **Payment Integration**: Stripe SDK, Razorpay SDK

---

## 🗃️ Database Schema

The database is structured to support multi-module domains:
- **Authentication**: Users, User Profiles, Email Verifications, and Password Reset tokens.
- **Appointments**: Doctors, Appointment Slots, Bookings, and Transition Logs.
- **Subscriptions**: Plans, Subscriptions, Usage Metrics, and Quotas.
- **Payments**: Razorpay and Stripe transaction receipts.

A detailed description of all tables, relations, column types, and constraints, along with a Mermaid diagram, can be found in:
👉 **[DATABASE_SCHEMA.md](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/DATABASE_SCHEMA.md)**

---

## 🚀 Quick Start Guide

### Prerequisites
Ensure you have the following installed on your local machine:
- **Java JDK 17** (or above)
- **Maven 3.8+**
- **Python 3.10+** (with `pip`)
- **Apache Kafka** & **PostgreSQL** (Hosted URLs are pre-configured in `.env`)

---

### Step 1: Configure Environment Variables

Create or update the `.env` file in the **project root** directory. A template is provided in [.env.example](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/.env.example).

```ini
# Database (Neon Connection)
DB_URL=jdbc:postgresql://<neon-host>/neondb
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>

# Kafka Configuration (Aiven SSL Details)
KAFKA_BOOTSTRAP_SERVERS=<kafka-host>:<port>
KAFKA_SASL_USERNAME=avnadmin
KAFKA_SASL_PASSWORD=<sasl-password>
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
KAFKA_SSL_TRUSTSTORE_LOCATION=file:./client.truststore.jks
KAFKA_SSL_TRUSTSTORE_PASSWORD=changeit

# Mail SMTP Credentials
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=587
MAIL_USERNAME=<smtp-username>
MAIL_PASSWORD=<smtp-password>

# JWT
JWT_SECRET_KEY=your-secret-key-min-256-bits-long-for-production-use-only
```

---

### Step 2: Set Up and Run the Java Backend

1. **Verify code builds and compile**:
   ```bash
   ./mvnw clean compile
   ```
2. **Launch the Spring Boot Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend server will start on port `9000` (default).

---

### Step 3: Set Up and Run the Python Background Worker

1. Navigate to the `python_workers` directory:
   ```bash
   cd python_workers
   ```
2. Create and activate a Python virtual environment:
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # On Windows, use: venv\Scripts\activate
   ```
3. Install the dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Verify the `.env` settings inside the `python_workers` directory. (A copy is pre-configured with working settings).
5. Start the background worker:
   ```bash
   python worker.py
   ```
   The background consumer will log its state and listen for incoming events from the Kafka topics.

---

## 📘 API Documentation (Postman Collection)

We have provided a complete Postman collection and environment template in the `postman/` directory:
- 📁 **Collection File**: [collection.json](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/postman/collection.json)
- 📁 **Environment Variables File**: [environment.json](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/postman/environment.json)

### How to Import & Run in Postman:
1. Open the Postman application.
2. Click **Import** in the top-left corner.
3. Drag and drop both [collection.json](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/postman/collection.json) and [environment.json](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/postman/environment.json) files.
4. Select the **Healthcare Appointment Environment** from the top-right environment selector dropdown in Postman.
5. You can now execute API requests. Dynamic tokens (like the JWT `jwt_token`, generated `doctor_id`, `slot_id`, and `appointment_id`) will automatically save to the environment variables upon successful responses and carry over to subsequent requests.

---

## 📦 Deliverables Status Checklist

- [x] **Backend & Frontend Code**: Spring Boot API Backend + Python Event Workers fully written and operational.
- [x] **API Documentation**: Postman collection and environment files preloaded in [postman/](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/postman).
- [x] **Database Schema**: Full database entity schema and Mermaid diagram created in [DATABASE_SCHEMA.md](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/DATABASE_SCHEMA.md).
- [x] **README**: Full system installation, runtime configuration, and environment setup instructions written in [README.md](file:///Users/saptarshisabui/Documents/GITHUB/bookingapp/README.md).
