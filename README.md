# REXXZO — Better Everyday 🛍️

A modern, full-stack e-commerce platform built with **Next.js 15 (React 19)** on the frontend and **Spring Boot 3 (Java 17+)** with **PostgreSQL** on the backend.

---

## 🚀 Architecture & Tech Stack

### Storefront & Admin Portal (`frontend/`)
- **Framework:** Next.js 15 (App Router, React 19, TypeScript)
- **Styling:** Tailwind CSS, Lucide Icons
- **Features:**
  - Modern, responsive customer shopping experience (Categories, Gift Corner, Dynamic Cart & Checkout)
  - Dedicated, isolated Admin Console (`/admin`) for analytics, inventory, order processing, return/refund management, and logistics integrations
  - Seamless OTP authentication flow (Email/SMS)

### REST Backend (`backend/`)
- **Framework:** Spring Boot 3.x with Java 17+
- **Database & ORM:** PostgreSQL, Spring Data JPA, Hibernate
- **Security:** Spring Security with JWT stateless authentication & Role-Based Access Control (RBAC)
- **Integrations:**
  - Fast2SMS / Twilio for SMS OTP
  - SMTP for Email OTP
  - Shiprocket Logistics & Automated Tracking
  - Razorpay / Custom Payment Gateway

---

## 📂 Project Structure

```text
├── frontend/             # Next.js 15 App Router Frontend
│   ├── src/app/          # Public storefront & admin routes
│   ├── src/components/   # Reusable UI components
│   ├── src/context/      # Store and state management
│   └── src/lib/          # API client and helper utilities
├── backend/              # Spring Boot Backend Service
│   ├── src/main/java/    # Controllers, Services, Repositories, Entities
│   └── src/main/resources/ # application.properties
├── assets/               # Static assets & media
└── css/                  # Styling assets
```

---

## 🛠️ Getting Started

### 1. Backend Setup
1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```
2. Copy `.env.example` to `.env` and configure your database and API credentials:
   ```bash
   cp .env.example .env
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend service starts at `http://localhost:8080`.

### 2. Frontend Setup
1. Navigate to the `frontend/` directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Next.js development server:
   ```bash
   npm run dev
   ```
   The application will be live at `http://localhost:3000`.

---

## 🔒 Security
- All sensitive credentials and production secrets are managed strictly through environment variables.
- Admin portal routes and administrative endpoints are fully authenticated and role-protected.
