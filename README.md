# SmartAir

**SmartAir** is a comprehensive Android application designed to assist children in managing their asthma while providing parents and healthcare providers with powerful monitoring and tracking tools. The app combines medical adherence tracking with gamification to encourage consistent medication use and proper inhaler technique.

## Project Overview

SmartAir bridges the gap between pediatric patients and their care network. It transforms the daily routine of asthma management into an engaging experience for children, while offering parents peace of mind through real-time inventory tracking and adherence logs.

## Key Features

### For Children (Gamified Experience)
*   **Interactive Inhaler Guide:** A step-by-step visual guide (with optional Spacer instructions) that walks the child through the inhaler process. It includes:
    *   Pre-check questions (Breathing check).
    *   Timers for shaking, inhaling, and holding breath.
    *   Technique validation.
    *   Post-check questions to ensure that doses improve symptoms.
*   **Gamification & Awards:**
    *   **Badges:** Children earn badges ("Consistent Controller", "Well Controlled") for maintaining streaks and keeping rescue usage low.
    *   **Visual Rewards:** Confetti animations and sound effects upon unlocking achievements.
    *   **Streaks:** Tracks "Technique Streak" and "Controller Adherence Streak", obtained by consistently following a pre-set controller schedule with or without technique, respectively.
*   **Asthma Action Plan:** Visual "Traffic Light" system (Green, Yellow, Red zones) based on Asthma Score and PEF (Peak Expiratory Flow) logs.
*   **Medication Logging:** Easy interface to log Controller and Rescue inhaler doses.

### For Parents (Monitoring & Safety)
*   **Parent Dashboard:** Link and monitor multiple children from a single parent account.
*   **Smart Inventory Management:**
    *   Track specific inhalers (Rescue vs. Controller).
    *   Visual indicators for remaining doses (Percentage and Count).
*   **Real-time Inventory Alerts:**
    *   **Instant Notifications:** The app runs a background service (`InventoryAlertService`) that notifies the parent immediately if a child's medication is:
        *   **Expired**
        *   **Empty**
        *   **Running Low (< 20%)**
    *   *Note: These alerts trigger whether the dose is logged by the parent or the child.*
*   **Adherence Summary:**
    *   Tap on the Adherence tile on the Parent dashboard to view a detailed summary of your child's controller adherence.
    *   **Weekly View:** Breakdown of compliant vs. missed days based on the planned schedule.
    *   **Monthly Calendar:** Visual history of adherence success.
*   **Symptom & Trigger Logging:** Log daily symptoms to keep a history for provider visits.

### For Providers
*   **Patient Oversight:** View patient lists and adherence data to make informed treatment adjustments.
*   **Triage:** Access to patient triage information.

## Technical Stack

*   **Language:** Java
*   **Platform:** Android (Min SDK 24)
*   **Backend:** Firebase Realtime Database
*   **Authentication:** Firebase Auth
*   **Key Libraries:**
    *   `Konfetti` (for particle systems/animations)
    *   `Glide` (for image/GIF loading)
    *   `MPAndroidChart` (for graphing health data)

## Notes on Inventory Notifications

The app utilizes a `Service` (`InventoryAlertService`) initiated by the Parent's Home screen.
1.  It monitors the Firebase Inventory node for linked children.
2.  It listens for changes in `remainingDoses` or `expiryDate`.
3.  If a threshold is breached (Expired, Empty, Low), it pushes a local notification to the parent's device.
4.  **Safety:** Role checks ensure that children logged into the same device do not receive administrative inventory alerts.

---
