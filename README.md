# Fitness Tracker 🏋️‍♂️

A modern, fast, and stable Android application built with Jetpack Compose to help you track your workouts, nutrition, and daily progress. Featuring an intelligent AI Coach powered by Gemini.

> **Note:** This application currently only supports the **Indonesian language**.

## Key Features

### 1. Advanced Workout Tracking 📊
- Track your **Leg Day**, **Push Day**, and **Pull Day** sessions with ease.
- Secured "Inject Data" mode with biometric (fingerprint/face) or PIN protection to prevent accidental edits.
- Automatic 60-second security re-lock to keep your data safe.
- Stable list ordering to prevent navigation errors.

### 2. AI Coach with Multi-API Rotation ✨
- Get personalized fitness and nutrition advice from an intelligent AI Coach.
- Robust stability with **Multi-API Key Rotation**: Automatically switches to backup Gemini API keys if one hits its quota (Error 429).
- Context-aware suggestions based on your actual workout history and schedule.

### 3. Nutrition & Protein Tracker 🥚
- Monitor your daily intake of eggs, fish, and pea protein.
- Dynamic targets that adjust based on whether it's a "Workout Day" or a "Rest Day."

### 4. Smart Clock & Weekly Schedule ⏰
- Real-time display of the current time, day, and your scheduled activity (e.g., `11:30 Saturday - Rest Day`).
- Customizable weekly workout schedule.

### 5. Flexible Theme System 🌙
- Seamlessly switch between **Light Mode**, **Dark Mode**, or follow your **System Default**.
- Persists your preference across app restarts.

### 6. Cloud Sync & Security ☁️
- Instant cloud synchronization using **Firebase Firestore**.
- Secure Google Sign-In and local login options.
- Digital Member Card to track your total consistency.

---

## Technical Stack
- **Framework:** Jetpack Compose (Kotlin)
- **Architecture:** MVVM with Flow & StateFlow
- **Database:** Room (Local) & Firestore (Cloud)
- **AI Integration:** Google Generative AI (Gemini)
- **UI Components:** Material 3 with Horizontal Pager

---

## Setup & Installation

1. Clone the repository.
2. Add your `google-services.json` to the `app/` directory.
3. Configure your API keys in `secrets.properties`:
   ```properties
   GEMINI_API_KEY=your_primary_key
   GEMINI_API_KEY_2=your_backup_key_1
   GEMINI_API_KEY_3=your_backup_key_2
   ```
4. Build and run via Android Studio.

---
*Developed with focus on performance and reliability.*
