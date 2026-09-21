const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

async function notify(doc) {
  const m = doc.data();

  // Admin SDK bypasses rules, so verify sender/receiver ourselves
  const conv = (await db.collection("conversations").doc(m.conversationId).get()).data();
  if (!conv || !conv.memberIds.includes(m.senderId) || !conv.memberIds.includes(m.receiverId)) {
    console.log("Skipping invalid message:", doc.id);
    return;
  }

  const sender = (await db.collection("users").doc(m.senderId).get()).data();
  const senderName = sender?.displayName ?? "New message";

  const devices = await db
    .collection("users").doc(m.receiverId).collection("devices").get();

  for (const d of devices.docs) {
    try {
      await getMessaging().send({
        token: d.data().token,
        notification: { title: senderName, body: m.text },
        data: {
          conversationId: m.conversationId,
          messageId: doc.id,
          senderId: m.senderId,
          senderName: senderName,
        },
        android: { notification: { channelId: "messages" } },
      });
      console.log("Sent to device", d.id);
    } catch (e) {
      console.error("Send failed for device", d.id, e.code);
    }
  }
}

let isFirstSnapshot = true;

db.collectionGroup("messages").onSnapshot(
  (snapshot) => {
    if (isFirstSnapshot) {
      isFirstSnapshot = false;
      console.log(`Worker started. Ignoring ${snapshot.size} existing messages.`);
      return;
    }
    snapshot.docChanges().forEach((change) => {
      if (change.type === "added") notify(change.doc);
    });
  },
  (err) => console.error("Listener error:", err)
);