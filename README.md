# StudyLock

StudyLock is a Cyber Pulse student-focus project with separate Android apps for students and parents.

## Modules

- `student` — student focus app, Live Tutor UI foundation, blocked-app accessibility service and local focus controls.
- `parent` — parent pairing/dashboard UI and remote-control foundation.
- `shared` — common pairing, metrics, configuration and parent-command models used by both apps.

## Current repository status

This repository now contains a clean Android Studio multi-module Kotlin/Jetpack Compose foundation. It intentionally does not contain private API keys, Firebase credentials, signing keys or generated APK files.

## Firebase setup

Create two Android apps in the same Firebase project:

- `com.cyberpulse.studylock.student`
- `com.cyberpulse.studylock.parent`

Download each app's `google-services.json` and place it inside its matching module locally. These files are ignored by Git and should not be committed to a public repository.

The backend should store pairing sessions, student metrics, parent commands and student configuration. The shared models in `shared/src/main/java/com/cyberpulse/studylock/shared/Models.kt` define the initial contract.

## Important implementation work still required

The current code is a foundation rather than a finished production parental-control system. The developer should connect Firebase Authentication/Firestore or another secure backend, implement real-time pairing, persist focus/session state, implement secure PIN management, scheduling, metrics upload, remote command handling, Gemini Live Tutor API calls and text-to-speech, and add tests.

Android Accessibility access must be explicitly enabled by the device user. StudyLock should not attempt to bypass Android security or secretly enable privileged permissions.

## Build notes

Open the repository in Android Studio with JDK 17. If the repository does not yet contain a Gradle wrapper on the developer's machine, generate one with a compatible Gradle installation or let Android Studio configure the project, then sync Gradle.

Compile SDK: 35
Minimum SDK: 26

## Project identity

StudyLock is a Cyber Pulse project focused on helping students remain focused during study sessions while allowing appropriately paired parent/guardian oversight.
