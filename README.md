# StudyLock

StudyLock is a Cyber Pulse Android project with separate Student and Parent apps connected through Firebase.

## Project modules

- `student` — focus sessions, accessibility-based blocked-app redirection, Firebase sync, pairing codes, daily schedule support, study metrics, Gemini Live Tutor, and text-to-speech.
- `parent` — pairing, live student metrics, remote focus start/end, blocked-app configuration, and scheduled-study configuration.
- `shared` — common models used by both apps.

## What is implemented

### Student app

- Kotlin + Jetpack Compose Android app.
- Focus length limited to 25–300 minutes.
- Local focus state persistence.
- Accessibility service that redirects away from configured blocked apps while focus is active.
- Six-character pairing-code generation.
- Firebase Anonymous Authentication.
- Firestore pairing confirmation.
- Firestore metrics upload.
- Real-time parent command listener.
- Remote start-focus and end-focus commands.
- Remote blocked-app updates.
- Remote daily schedule configuration.
- AlarmManager-based daily schedule state activation.
- Gemini Live Tutor using the current Gemini Interactions REST API.
- API key stored only in local app preferences, not in GitHub or Firestore.
- Android TextToSpeech output for tutor replies.

### Parent app

- Kotlin + Jetpack Compose Android app.
- Join Student app using the six-character pairing code.
- Pairing waits for Student-side confirmation before protected data becomes accessible.
- Live student focus state.
- Total-study and AI-usage metric display.
- Remote focus start/end.
- Focus duration selection from 25–300 minutes.
- Remote blocked-app package list.
- Remote daily schedule configuration.

### Firebase security

`firestore.rules` restricts protected student metrics, commands, and configuration to the Student account and the paired Parent account. The Student account is the only account allowed to accept the paired parent into its own student record.

Pairing-code documents are readable by authenticated users because the Parent app must resolve a code before it knows the Student UID. Pairing codes are random, short-lived application credentials and should be regenerated if exposed. For a production deployment, add expiry timestamps and cleanup of stale pairing documents.

## Firebase setup

Create one Firebase project and register these two Android applications inside it:

- Student package: `com.cyberpulse.studylock.student`
- Parent package: `com.cyberpulse.studylock.parent`

For each registered Android app, download its own `google-services.json` file.

Place them locally as:

```text
student/google-services.json
parent/google-services.json
```

Do not commit either file to a public repository. The repository `.gitignore` already excludes `google-services.json`.

### Enable Authentication

In Firebase Console:

1. Open Authentication.
2. Open Sign-in method.
3. Enable Anonymous authentication.

StudyLock currently uses anonymous Firebase identities so pairing works without collecting student or parent email addresses. A production release can later migrate to email/passkey/provider accounts while preserving the Firestore ownership model.

### Create Firestore

Create a Cloud Firestore database for the Firebase project.

The repository contains:

```text
firebase.json
firestore.rules
firestore.indexes.json
```

Deploy the included rules with the Firebase CLI from the repository root:

```bash
firebase login
firebase use YOUR_FIREBASE_PROJECT_ID
firebase deploy --only firestore
```

Review the rules before production deployment and test them using the Firebase Emulator Suite.

## Firestore layout

```text
pairings/{pairingCode}
  studentId
  parentId
  paired
  createdAt

students/{studentId}
  ownerId
  parentId

students/{studentId}/metrics/current
  totalStudyMinutes
  aiUsageCount
  activeFocusSession
  currentSessionMinutes
  blockedApps
  lastUpdatedEpochMs

students/{studentId}/config/current
  blockedApps
  autoStudyEnabled
  scheduledStartHour
  scheduledStartMinute
  defaultStudyMinutes

students/{studentId}/commands/{commandId}
  type
  minutes
  blockedApps
  parentId
  createdAt
  processed
```

## Pairing flow

1. Student opens StudyLock Student and generates a pairing code.
2. Student shares that code with the intended parent/guardian.
3. Parent enters the code in StudyLock Parent.
4. Parent app claims the pairing document using its Firebase UID.
5. Student app observes the claim and writes that Parent UID into `students/{studentId}`.
6. Firestore security rules then allow only that paired Parent UID to read protected metrics and create remote commands.

## Gemini Live Tutor

The Student app uses Google's Gemini Interactions API and currently targets `gemini-3.7-flash`.

The Gemini API key is entered by the Student on-device and stored locally in Android SharedPreferences. It is not committed to the repository and is not uploaded to Firestore by StudyLock.

For a production release, consider proxying Gemini requests through a controlled backend so quotas and abuse protection are not tied directly to a client-side API key.

## Accessibility and Android limitations

StudyLock does not secretly enable Android permissions. The device user must explicitly enable the StudyLock Accessibility Service in Android Settings before blocked-app redirection can work.

The accessibility service only redirects configured blocked packages while StudyLock's focus state is active. Android may restrict automatically bringing an activity to the foreground from the background on some devices. The scheduled receiver still records the scheduled focus state, but production behavior should be tested on the specific Android versions and manufacturers being supported.

StudyLock should not attempt to bypass Android security, prevent normal operating-system recovery, hide itself, or silently grant privileged permissions. Stronger institution-managed restrictions should use Android's supported Device Policy / managed-device APIs rather than accessibility tricks.

## Build requirements

- Android Studio with JDK 17.
- Compile SDK 35.
- Minimum SDK 26.
- Internet access for Gradle dependency download, Firebase, and Gemini.
- Each module's correct local `google-services.json`.

The repository does not currently include generated APK files or signing keys.

If a Gradle wrapper is not present after cloning, Android Studio can import/sync the project using a compatible local Gradle installation and a wrapper can then be generated for the repository.

## Before release

The developer should still complete production testing, including:

- Firebase Emulator security-rule tests.
- Pairing expiry and stale-code cleanup.
- Authentication-account upgrade/migration if anonymous identities are not sufficient.
- Session-duration accumulation into `totalStudyMinutes`.
- App lifecycle/reboot handling for scheduled study sessions.
- Manufacturer-specific background restrictions.
- Unit/UI/instrumentation tests.
- Crash reporting and privacy review.
- Release signing and Play policy review, especially for AccessibilityService usage.

## Repository safety

Never commit:

- `google-services.json`
- Gemini/API secrets
- signing keystores
- service-account keys
- Firebase Admin credentials

## Project identity

StudyLock is a Cyber Pulse student-focus project designed to help students remain focused during study sessions while supporting transparent, appropriately paired parent or guardian oversight.
