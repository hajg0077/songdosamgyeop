// tools/set_hq_claims.js
const admin = require("firebase-admin");
const path = require("path");

const serviceAccount = require(path.join(__dirname, "serviceAccountKey.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

(async () => {
  const uid = process.argv[2];
  if (!uid) {
    console.error("Usage: node tools/set_hq_claims.js <UID>");
    process.exit(1);
  }

  // 1) Custom Claims 부여 (rules에서 role=='HQ' 사용 중)
  await admin.auth().setCustomUserClaims(uid, { role: "HQ", admin: true });

  // 2) users/{uid} 캐시 문서(선택이지만 추천)
  const user = await admin.auth().getUser(uid);
  await db.doc(`users/${uid}`).set(
    {
      role: "HQ",
      isAdmin: true,
      email: user.email || null,
      name: user.displayName || null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      bootstrappedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  console.log("✅ HQ claims set:", uid);
  process.exit(0);
})().catch((e) => {
  console.error("❌ Failed:", e);
  process.exit(1);
});