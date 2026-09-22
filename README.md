# RelayChat

A lightweight one-to-one Android chat app built with Kotlin, Jetpack Compose, and Firebase.

## Features

- Email/password registration, login, logout
- User discovery — browse registered users and start a conversation
- Realtime one-to-one messaging with deterministic conversation IDs (no duplicate conversations)
- Conversation list with last-message preview, timestamp, and unread-bold styling
- Reply-to-message with quoted preview (long-press a message to reply to it)
- Typing indicator
- Read receipts ("Seen" on your last message once it's read)
- Push notifications for new messages, with tap-to-open routing to the correct conversation
- Avatar/initial circles, loading/error/empty states throughout

## Tech Stack

**Android**
- Kotlin, Jetpack Compose, Android Navigation
- ViewModel + Kotlin Coroutines + StateFlow
- MVVM + Repository pattern

**Backend**
- Firebase Authentication (Email/Password)
- Cloud Firestore
- Firebase Cloud Messaging (FCM)
- A local Node.js script (`firebase-admin` SDK) that listens for new messages and triggers push notifications — used in place of a Cloud Function to keep the project on Firebase's free Spark plan

## Architecture

```
Compose UI → ViewModel → Repository → Firebase SDK → Firebase services
```

- **UI** renders state and forwards user actions — no Firebase logic lives here.
- **ViewModel** owns screen state and collects repository streams.
- **Repository** owns all Firebase reads/writes and realtime listeners.
- **Firebase** is the persistent source of truth.

Cloud Firestore is authoritative for chat state. FCM is used only for notification delivery and navigation — never as the mechanism for syncing messages. Opening a conversation always attaches to the live Firestore listener, so a notification only routes you there; it never stands in for the message itself.

## Firestore Schema

```
users/{uid}
  uid, displayName, email, createdAt

users/{uid}/devices/{deviceId}
  token, updatedAt, platform

conversations/{conversationId}
  id, memberIds, lastMessage, lastMessageAt, lastSenderId,
  createdAt, updatedAt, typing (map, per-user), lastReadAt (map, per-user)

conversations/{conversationId}/messages/{messageId}
  id, conversationId, senderId, receiverId, text, createdAt, type,
  replyToMessageId / replyToText / replyToSenderId (optional)
```

The conversation ID is deterministic — the sorted pair of member UIDs joined with `_` — so opening a conversation is a single `set(..., merge = true)` call rather than a query-then-create.

## Notification Flow

```
message committed to Firestore
    ↓
local Node.js worker reacts (firebase-admin SDK)
    ↓
recipient device token(s) loaded
    ↓
FCM notification sent
    ↓
tap → conversationId resolved → ChatScreen opens → Firestore listener attaches
```

The Node.js worker replaces a Cloud Function to avoid requiring Firebase's paid Blaze plan; in a production setting this would move to a deployed Cloud Function or small backend service.

## Setup

### Prerequisites

- Android Studio, Android SDK/Emulator
- Node.js + npm
- Firebase CLI
- A Firebase project on the Spark (free) plan

### Firebase project

1. Create a Firebase project (Spark plan).
2. Register the Android app (`com.example.relaychat`), download `google-services.json` into `app/`.
3. Enable Email/Password auth, Cloud Firestore, and Cloud Messaging.

### Deploy security rules

```bash
firebase login
firebase deploy --only firestore:rules
```

### Notification worker

```bash
cd notification-worker
npm init -y
npm install firebase-admin
```

Generate a service-account key (Firebase Console → Project Settings → Service Accounts) and reference it locally. This file must never be committed.

## Running the App

1. Run the Android app from Android Studio on an emulator/device.
2. In a separate terminal, start the notification worker:
   ```bash
   node notify-worker.js
   ```
   This must be running for background push notifications to be delivered.

## Testing with Two Emulators

Create two AVDs with Google Play services, install the app on both, and log in as different accounts. Verify: realtime two-way messaging with no manual refresh, correct ordering under rapid sends, background notification delivery, notification tap opens the right conversation, and state recovers correctly on reopen/logout-login.

## Firestore Security Rules (summary)

```
users/{uid}                      read: any signed-in user; write: own doc only
users/{uid}/devices/{deviceId}   read/write: owner only
conversations/{id}               get/list: members only
                                  create: exactly 2 members, deterministic ID
                                  update: limited fields; per-user map writes
                                  (typing/lastReadAt) restricted to caller's own key
conversations/{id}/messages/{id} read: members only
                                  create: sender must equal caller, fixed shape
                                  no update/delete
```

Access control is enforced server-side by these rules, not by hiding UI. The notification worker runs with Admin SDK privileges (bypassing rules) and validates recipient data itself before sending.

## Known Limitations

- The notification trigger depends on a local Node.js worker rather than an always-on deployed service.
- No media/image messages (would require Firebase Storage, which needs the paid Blaze plan).
- No end-to-end encryption.
- Minimal offline-first UX polish beyond Firestore's default offline caching.

## Possible Future Improvements

- Move the notification trigger to a deployed Cloud Function or backend service.
- Image/media messages, presence, message search.
- Consecutive-message grouping and date dividers in the chat thread.
- Profile photos, dark theme polish.
