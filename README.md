SMART AIR - Required Features
1) Purpose
SMART AIR is a kid-friendly Android app that helps children (ages 6–16) understand asthma, practice good inhaler technique, log symptoms/medicine use, and share parent-approved information with a healthcare provider via a concise, exportable report.
2) Users & Roles
Child: logs medicines and symptoms; sees gamified technique helpers and simple guidance.
Parent: links one or more children; views dashboards, receives alerts, manages sharing, exports reports.
Provider (read-only): views the shared summary/report only when the Parent enables sharing.

Privacy defaults:
-Child sees only their own data.
-By default nothing is shared with a Provider.
-The Parent controls what a Provider can view per child via granular toggles; changes take effect in real time and are reversible.
Access & basics:
-Provider in-app access: Parent sends a one-time invite code/link (expires in 7 days; can be revoked anytime). Sharing can also be done by exporting a PDF/CSV.
3) Glossary
-Rescue inhaler: quick-relief medicine for sudden symptoms.
-Controller medicine: daily prevention medicine that reduces airway swelling.
-Peak-flow (PEF): a number the user manually enters to reflect how fast they can blow out.
-Personal best (PB): highest healthy PEF used to define zones: Green ≥80% PB; Yellow 50–79% PB; Red <50% PB.
-Triggers: things that can set off symptoms (e.g., dust mites, pets, smoke, strong odors/perfumes, cold air, illness, exercise).
-Dose count (for logs): number of puffs/measures taken.
-Rapid rescue repeats (alert default): ≥3 rescue uses within 3 hours (configurable).
-Low canister (alert default): ≤20% remaining (child can flag manually).
-Low rescue month (badge default): ≤4 rescue days in 30 days.
-High-quality technique session: all technique steps completed with positive feedback; N = 10 sessions unlock a badge (configurable).
-Trend snippet default range: 7 days (toggle to 30 days).
Note: PEF is treated as a simple number for kids; PB is set by the Parent from the clinician’s plan.
4) Core Requirements
R1. Accounts, Roles & Onboarding
Goal: Secure sign-in; role-appropriate homes; clear expectations.
Functional requirements
-Sign-in & recovery: Email/password sign-in and credential recovery for Parent and Provider. The Child can (a) sign in with their own account or (b) use a child profile under a Parent account (no email required for the child profile).
-Role routing: Role selection on first run; on sign-in, each role lands on its correct home (Child / Parent / Provider).
-Onboarding (first run for each role):
    Explains rescue vs controller, app purpose, and who can see what.
    Shows the privacy defaults and how Parent sharing controls and Provider invites work.
-Security:
    Sign-out available for all roles.
    Protected screens are inaccessible when signed out.
    Registration validation blocks empty/invalid submissions (e.g., malformed email, weak password, missing required fields).
Clarifications:
-Providers are always read-only inside the app.
-A Child using a child profile does not require an email; the Parent owns the account and permissions.
R2. Parent/Child Linking & Selective Sharing
Goal: Parents manage children and control sharing with Providers.
Functional requirements
-Manage children: Parents can add/link/manage multiple children (name, DOB/age, optional notes).
-Granular sharing (per child): A simple “Share with Provider” selector with toggles for each data type:
    Rescue logs
    Controller adherence summary
    Symptoms
    Triggers
    Peak-flow (PEF)
    Triage incidents
    Summary charts (dashboard/report visuals)
-In-app labels: Items shared with a Provider display a small “Shared with Provider” tag.
-Behavior: Toggles apply in real time and are reversible at any moment.
-Provider access: Providers can view only what the Parent has enabled, and only for the selected child(ren).
-Invite flow: Parent generates a one-time invite code/link (valid 7 days); Parent can revoke or regenerate at any time.
R3. Medicines, Technique & Motivation
Goal: Make correct use easy, visible, and rewarding.
Functional requirements
-Medicine logs:
    Rescue vs Controller logging kept distinct.
    Each entry captures timestamp and dose count (puffs/measures).
-Technique Helper:
    Step-by-step prompts (seal lips, slow deep breath, hold ~10s, wait 30–60s between puffs if directed, spacer/mask tips).
    Includes at least one embedded video/animation.
-Pre/Post check:
    Before and after a dose: “Better / Same / Worse” + a short breath rating.
-Inventory (Parent):
    Track purchase date, amount left (parent/child-marked), expiry date, and
replacement reminders.
    Alerts: Low canister (≤20%) and expired medication.
-Motivation (shared Streaks/Badges service):
    Streaks: consecutive planned controller days; consecutive technique-completed days.
    Badges: first perfect controller week; 10 high-quality technique sessions; low rescue month (≤4 rescue days / 30 days). Thresholds are configurable in settings.
R4. Safety & Control (PEF, Zones & Triage)
Goal: Give clear, safe next steps.
Functional requirements
-PEF:
    Manual PEF entry, with optional pre/post-med values.
-Personal Best & Zones:
    PB is settable by Parent.
    App computes today’s zone using PB and shows it on Child and Parent homes.
    Zone thresholds: Green ≥80% PB; Yellow 50–79% PB; Red <50% PB.
    Zone changes are logged to history.
-One-tap triage (“Having trouble breathing?”):
    Quick red-flag checks: can’t speak full sentences; chest pulling in/retractions; blue/gray lips/nails.
    Asks for recent rescue attempts and optional current PEF.
    Decision Card:
■ Call Emergency Now for critical flags.
■ Start Home Steps for controllable symptoms (shows zone-aligned steps from the action plan).
    Timer & re-check: default 10-minute timer; auto-escalation if not improving or if new red flags appear.
    Parent alert: fired when a triage session starts and on escalation.
-Incident log: Captures which flags were present, guidance shown, user response, and optional PEF. Appears in history and in the Provider report if sharing is enabled.
-Safety note (always visible in triage): “This is guidance, not a diagnosis. If in doubt, call emergency.”
-Alert defaults (safety-related):
    Rapid rescue repeats: ≥3 rescue uses within 3 hours triggers an alert to the Parent.
    Notification latency target: under 10 seconds from event to delivered push (FCM), network permitting.
R5. Symptoms, Triggers & History
Goal: A simple daily check-in and enough history to see patterns.
Functional requirements
-Daily check-in: Night waking, activity limits, cough/wheeze (aligned to control concepts).
-Entry author: Each entry is labeled Child-entered or Parent-entered.
-Triggers: Multiple tags per entry (e.g., exercise, cold air, dust/pets, smoke, illness, perfume/cleaners/strong odors).
-History browser (3–6 months):
    Filters by symptom, trigger, and date range.
    Export selected range as PDF (and optional CSV) for provider use.
R6. Parent Home, Notifications & Provider Report
Goal: One-glance status for parents, plus a provider-ready summary.
Functional requirements
-Dashboard tiles (Parent):
    Today’s zone
    Last rescue time
    Weekly rescue count
    Trend snippet (default 7 days; toggle 30 days) using a shared Charts component
-Alerts (FCM, real-time):
    Red-zone day
    Rapid rescue repeats (≥3 in 3 hours)
    “Worse after dose”
    Triage escalation
    Inventory low/expired
-Provider report (shareable PDF; parent-controlled window 3–6 months):
    Rescue frequency and controller adherence (% of planned days completed)
    Symptom burden (counts of problem days)
    Zone distribution over time
    Notable triage incidents (if sharing enabled)
    At least one time-series chart and one categorical chart (bars/pie)
-Adherence calculation: From controller logs vs planned schedule; the schedule is parent-configurable.

User Stories:

Epic A – Accounts, Roles & Onboarding (R1)

US-A1 – Parent registration & login
As a Parent, I want to sign up and sign in using my email and password, so that I can securely access and manage my child’s asthma information.
Acceptance criteria:
Email/password registration with validation (non-empty, valid email, strong password).
Login rejects invalid credentials with friendly error.
“Forgot password” flow via email.
Sign-out option that clears protected data from view.

US-A2 – Provider registration & login
As a Provider, I want to sign in with my email and password, so that I can access shared patient summaries securely.
Acceptance criteria:
Email/password login + recovery.
Provider role is read-only.
Provider always lands on Provider Home.

US-A3 – Child profile under Parent account
As a Parent, I want to create child profiles without requiring their email, so that younger kids can use the app under my account.
Acceptance criteria:
Parent can create a child with name, DOB/age, optional notes.
Child profile can be used by logging in as Parent and switching to Child mode.
Child profile does not require an email/password.

US-A4 – Role selection & routing
As any new user, I want to select my role (Parent, Child, Provider) on first run, so that the app can tailor my home experience.
Acceptance criteria:
First run asks: “Who’s using SMART AIR?” → Parent / Child / Provider.
After login, app routes to the correct home (Parent Home / Child Home / Provider Home).
Remember chosen role for the account (when applicable).

US-A5 – Onboarding & privacy explanation:
As any user, I want a simple onboarding that explains rescue vs controller, app purpose, and who can see what, so that I feel safe and informed.
Acceptance criteria:
Onboarding screens cover:
What SMART AIR is for.
Rescue vs controller (simple language).
Privacy defaults (nothing shared with Provider by default).
How Parent sharing controls and Provider invites work.
Onboarding is shown once per role, with option to view again later.

US-A6 – Session security
As any user, I want protected screens to be inaccessible when signed out, so that others can’t see my data.
Acceptance criteria:
If token/session expired ⇒ redirect to sign-in.
Direct navigation to protected screen while signed out is blocked.

Epic B – Parent/Child Linking & Selective Sharing (R2)

US-B1 – Manage multiple children
As a Parent, I want to add and manage multiple children, so that I can track asthma for each child separately.
Acceptance criteria:
Parent can add/edit/remove child profiles.
Each child’s data is stored separately.
Switching between children is clear and safe (e.g., drop-down or tabs).

US-B2 – Granular sharing per child
As a Parent, I want to control which parts of my child’s data are shared with the Provider, so that I can protect privacy while still being helpful.
Acceptance criteria:
Per child, there is a “Share with Provider” section with toggles for:
Rescue logs
Controller adherence summary
Symptoms
Triggers
PEF
Triage incidents
Summary charts
Changes are applied in real time and are reversible anytime.
Items that are currently shared show a “Shared with Provider” tag in the UI.

US-B3 – No sharing by default
As a Parent, I expect that nothing is shared with Providers until I explicitly turn it on, so that privacy is preserved.
Acceptance criteria:
All sharing toggles default to OFF when a child is created.
No provider can see child data unless at least one toggle is ON and a valid invite exists.

US-B4 – Provider invite flow
As a Parent, I want to generate a one-time invite link/code for my child’s Provider, so that they can access a read-only report.
Acceptance criteria:
Parent can generate an invite code or link per child (or per family).
Invite is valid for 7 days.
Parent can revoke or regenerate at any time.
When revoked, Provider access is removed immediately.

US-B5 – Provider access scope
As a Provider, I want to see only what the Parent has chosen to share, so that privacy is respected while I get the key information I need.
Acceptance criteria:
Provider sees a list of children they have access to.
For each child, only shared data types are visible.
If Parent disables a data type or revokes access, Provider’s view updates accordingly (e.g., section disappears or shows “Parent has stopped sharing this data”).

Epic C – Medicines, Technique & Motivation (R3)


US-C1 – Log rescue medicine
As a Child, I want to quickly log when I use my rescue inhaler, so that I and my Parent can track how often I need it.
Acceptance criteria:
Rescue log screen lets child choose: timestamp (default = now) and dose count (puffs).
Rescue logs are clearly separated from controller logs.
Pre/Post “Better / Same / Worse” and breath rating are captured.

US-C2 – Log controller medicine
As a Child, I want to log my daily controller medicine use, so I can build good habits and see my streak.
Acceptance criteria:
Controller log screen with timestamp and dose count.
Pre/Post check (Better / Same / Worse + breath rating).
Controller logs are associated with planned schedule for adherence calculations.

US-C3 – Technique helper session
As a Child, I want a step-by-step inhaler helper with animations, so that I can practice good technique and get feedback.
Acceptance criteria:
Flow includes prompts such as:
Seal lips around mouthpiece
Slow, deep breath in
Hold ~10s
Wait 30–60 seconds between puffs if needed
Spacer/mask tips
Includes at least one video/animation.
After completion, system evaluates if all key steps were done → marks session as “high-quality” or not.

US-C4 – Inventory tracking & alerts (Parent)
As a Parent, I want to track inhaler inventory and expiry, so that my child doesn’t run out or use expired medicine.
Acceptance criteria:
Parent can enter: purchase date, amount left (estimated or child-input), expiry date.
Alerts for:
Low canister: ≤ 20% remaining.
Expired medication.
Parent and/or Child can mark “low” manually as well.

US-C5 – Motivation: streaks
As a Child, I want to see streaks for controller adherence and technique practice, so that I’m motivated to keep going.
Acceptance criteria:
Streaks tracked for:
Consecutive planned controller days completed.
Consecutive days with technique-completed sessions.
Shared streak service calculates streak lengths and exposes them to Parent & Child UIs.

US-C6 – Motivation: badges
As a Child, I want to earn badges for good habits, so that using my medicine correctly feels rewarding.
Acceptance criteria:
Badges include (at minimum):
First perfect controller week.
10 high-quality technique sessions.
Low rescue month (≤ 4 rescue days/30 days).
Thresholds are configurable in Settings.
Badges appear in Child profile and optionally in Parent view.

Epic D – Safety & Control: PEF, Zones & Triage (R4)

US-D1 – Enter PEF values
As a Child or Parent, I want to manually enter my peak-flow number before/after medicine, so that we can track lung function.
Acceptance criteria:
PEF entry screen allows:
Value
Tag as pre-med or post-med (optional).
PEF treated as simple number.

US-D2 – Set personal best & compute zones
As a Parent, I want to set or update my child’s Personal Best PEF, so that the app can compute daily zones.
Acceptance criteria:
Parent can enter PB.
App calculates today’s zone using PB:
Green: ≥ 80% of PB
Yellow: 50–79% of PB
Red: < 50% of PB
Zone is visible on Child Home and Parent Home.
Zone changes are logged to history.

US-D3 – One-tap triage: “Having trouble breathing?”
As a Child, I want a simple “Having trouble breathing?” button that guides me through what to do, so that I get quick, safe advice.
Acceptance criteria:
One-tap entry opens triage flow.
Flow asks:
Red flags: can’t speak full sentences, chest pulling in/retractions, blue/gray lips/nails.
Recent rescue attempts and optional current PEF.
Decision card outcomes:
“Call Emergency Now” when critical flags present.
“Start Home Steps” with zone-aligned steps otherwise.
Always shows safety note: “This is guidance, not a diagnosis. If in doubt, call emergency.”

US-D4 – Triage timer & escalation
As a Parent, I want the app to re-check after a period and escalate if things don’t improve, so that my child doesn’t stay in trouble unnoticed.
Acceptance criteria:
Default timer: 10 minutes.
After timer, re-check symptoms and ask if “Better / Same / Worse”.
Auto-escalation if “not improving” or new red flags:
Show stronger “Call Emergency Now” guidance.
Parent receives escalation alert.

US-D5 – Incident log
As a Parent or Provider, I want a history of triage incidents, so that we can review what happened and how severe episodes were.
Acceptance criteria:
Each triage incident logs:
Which flags were present.
Guidance shown.
User response (what they did).
Optional PEF values.
Appears in history.
Appears in Provider report if sharing enabled for triage incidents.

US-D6 – Rapid rescue alert
As a Parent, I want to be alerted when rescue medicine is used very frequently, so that I can intervene or seek help.
Acceptance criteria:
App triggers an alert if ≥ 3 rescue uses within 3 hours (default; configurable).
Uses push notifications (FCM).
Target latency: under 10 seconds from event to delivered push (network permitting).

Epic E – Symptoms, Triggers & History (R5)

US-E1 – Daily check-in
As a Child or Parent, I want a simple daily check-in for symptoms, so that I can track how well controlled asthma is.
Acceptance criteria:
Daily check-in asks about:
Night waking.
Activity limits.
Cough/wheeze.
Uses simple kid-friendly options (e.g., none / a little / a lot).

US-E2 – Mark entry author
As a Parent, I want to know who entered each check-in, so I can interpret the data better.
Acceptance criteria:
Each entry labeled as “Child-entered” or “Parent-entered”.

US-E3 – Attach triggers to entries
As a Child or Parent, I want to tag triggers to symptom entries, so that patterns can be spotted (e.g., exercise, cold air).
Acceptance criteria:
Multiple triggers can be tagged per entry from a set (e.g., exercise, cold air, dust/pets, smoke, illness, perfume/cleaners/strong odors).
User can optionally add notes.

US-E4 – History browser & filters
As a Parent, I want to browse 3–6 months of history and filter by symptom, trigger, and date range, so I can see patterns.
Acceptance criteria:
History screen shows entries for at least 3 months (up to 6).
Filters by symptom type, trigger tags, and date range.
Filtered view updates list & charts.

US-E5 – Export history for provider
As a Parent, I want to export a selected date range as a PDF and optional CSV, so that I can share it with the Provider.
Acceptance criteria:
Export button on history screen.
User selects date range.
Generates printable PDF summary and optionally CSV.
Export respects sharing settings when used via Provider report; a separate full export may be allowed for offline sharing.

Epic F – Parent Home, Notifications & Provider Report (R6)

US-F1 – Parent dashboard tiles
As a Parent, I want a quick dashboard that shows my child’s current status, so that I can see if things are okay at a glance.
Acceptance criteria:
For selected child, dashboard tiles include:
Today’s zone (Green/Yellow/Red).
Last rescue time.
Weekly rescue count.
Trend snippet (default 7 days; toggle 30) using shared Charts component.
Tap on tiles navigates to more detail where appropriate.

US-F2 – Alerts & notifications
As a Parent, I want real-time alerts for key safety events, so that I can respond quickly.
Acceptance criteria:
Push notifications (FCM) triggered for:
Red-zone day.
Rapid rescue repeats.
“Worse after dose”.
Triage escalation.
Inventory low/expired.
Target latency: events processed and notification sent within ~10 seconds (network permitting).

US-F3 – Provider report generation
As a Parent, I want a provider-ready report that summarizes my child’s control over 3–6 months, so I don’t have to explain everything from memory.
Acceptance criteria:
Parent sets a reporting window (e.g., last 3 months, or specific range within 3–6 months).
Report includes:
Rescue frequency.
Controller adherence (% of planned days completed).
Symptom burden (counts of problem days).
Zone distribution over time.
Notable triage incidents (if that sharing toggle is on).
At least:
1 time-series chart (e.g., PEF or rescue frequency over time).
1 categorical chart (e.g., trigger distribution or zone distribution).
Exportable as PDF.
Shares only data types whose toggles are ON.

US-F4 – Adherence calculation
As a Parent, I want to see my child’s controller adherence as a percentage, so that I know if we’re following the plan.
Acceptance criteria:
Parent can configure planned schedule (e.g., once daily, twice daily).
Adherence = (days following schedule) / (total planned days) * 100%.
Adherence shows on dashboard and in Provider report.