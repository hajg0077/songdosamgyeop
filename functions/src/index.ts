import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// 서울 리전 권장 (원하면 us-central1로 바꿔도 OK)
const region = "asia-northeast3";

/** HQ 권한 확인 */
function assertHQ(context: functions.https.CallableContext) {
  const role = context.auth?.token?.role;
  if (!context.auth || role !== "HQ") {
    throw new functions.https.HttpsError("permission-denied", "HQ only");
  }
}

/** 공통: 등록문서 로드 + 상태체크 */
async function loadRegistration(docId: string) {
  const ref = db.collection("registrations").doc(docId);
  const snap = await ref.get();
  if (!snap.exists) {
    throw new functions.https.HttpsError("not-found", "registration not found");
  }
  const data = snap.data()!;
  return { ref, data };
}

/** 승인: Auth 사용자 생성(or 갱신) + 커스텀클레임(role, branchId) + users 문서 생성 + registrations.status=APPROVED */
export const hqApproveRegistration = functions
  .region(region)
  .https.onCall(async (data, context) => {
    assertHQ(context);
    const docId = String(data.docId || "");
    if (!docId) throw new functions.https.HttpsError("invalid-argument", "docId required");

    const { ref, data: reg } = await loadRegistration(docId);
    if (reg.status !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "already processed");
    }

    const email = String(reg.email || "");
    const name = String(reg.name || "");
    const branchName = String(reg.branchName || "");
    const branchCode = reg.branchCode ? String(reg.branchCode) : null;

    // branchId 결정 (선택: branchCode가 있으면 사용, 없으면 새로 발급)
    const branchId = branchCode || `BR_${ref.id}`;

    // 사용자 생성/갱신
    let user;
    try {
      user = await auth.getUserByEmail(email);
      // 기존 유저면 디스플레이네임 보정 정도만
      if (name && user.displayName !== name) {
        await auth.updateUser(user.uid, { displayName: name });
      }
    } catch {
      // 없으면 생성 (임시 비밀번호없이 생성 → 사용자 측은 "비밀번호 재설정"로 진입 권장)
      user = await auth.createUser({
        email,
        displayName: name || undefined,
        emailVerified: false,
        disabled: false,
      });
    }

    // 커스텀 클레임 부여
    await auth.setCustomUserClaims(user.uid, { role: "BRANCH", branchId });

    // users 문서 생성/갱신
    await db.collection("users").doc(user.uid).set(
      {
        email,
        name,
        role: "BRANCH",
        branchId,
        branchName,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    // registrations 상태 갱신
    await ref.update({
      status: "APPROVED",
      approvedAt: admin.firestore.FieldValue.serverTimestamp(),
      approvedBy: context.auth!.uid,
    });

    return { message: "approved", uid: user.uid, branchId };
  });

/** 반려: 이유 저장 + registrations.status=REJECTED */
export const hqRejectRegistration = functions
  .region(region)
  .https.onCall(async (data, context) => {
    assertHQ(context);
    const docId = String(data.docId || "");
    const reason = data.reason ? String(data.reason) : "";
    if (!docId) throw new functions.https.HttpsError("invalid-argument", "docId required");

    const { ref, data: reg } = await loadRegistration(docId);
    if (reg.status !== "PENDING") {
      throw new functions.https.HttpsError("failed-precondition", "already processed");
    }

    await ref.update({
      status: "REJECTED",
      rejectReason: reason,
      rejectedAt: admin.firestore.FieldValue.serverTimestamp(),
      rejectedBy: context.auth!.uid,
    });

    return { message: "rejected" };
  });

/** 리셋(UNDO): PENDING으로 되돌림 */
export const hqResetRegistration = functions
  .region(region)
  .https.onCall(async (data, context) => {
    assertHQ(context);
    const docId = String(data.docId || "");
    if (!docId) throw new functions.https.HttpsError("invalid-argument", "docId required");

    const { ref } = await loadRegistration(docId);
    await ref.update({
      status: "PENDING",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { message: "reset to PENDING" };
  });


export const hqUpdateOrderStatus = functions
  .region(region)
  .https.onCall(async (data, context) => {
    assertHQ(context);
    const orderId = String(data.orderId || "");
    const next = String(data.nextStatus || "");
    if (!orderId || !next) {
      throw new functions.https.HttpsError("invalid-argument", "orderId, nextStatus required");
    }

    const allowed = new Set(["PENDING","APPROVED","REJECTED","SHIPPED","DELIVERED"]);
    if (!allowed.has(next)) {
      throw new functions.https.HttpsError("invalid-argument", "invalid status");
    }

    const canTransit = (from: string, to: string) => {
      const F = (from || "PENDING").toUpperCase();
      const T = to.toUpperCase();
      if (F === "PENDING")  return T === "APPROVED" || T === "REJECTED";
      if (F === "APPROVED") return T === "SHIPPED";
      if (F === "SHIPPED")  return T === "DELIVERED";
      return false;
    };

    const ref = db.collection("orders").doc(orderId);
    return await db.runTransaction(async tx => {
      const snap = await tx.get(ref);
      if (!snap.exists) {
        throw new functions.https.HttpsError("not-found", "order not found");
      }
      const order = snap.data()!;
      const prev = (order.status || "PENDING").toString().toUpperCase();

      if (!canTransit(prev, next)) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `transition not allowed: ${prev} → ${next}`
        );
      }

      const now = admin.firestore.FieldValue.serverTimestamp();
      tx.update(ref, {
        status: next,
        updatedAt: now,
        updatedBy: context.auth!.uid,
        statusHistory: admin.firestore.FieldValue.arrayUnion({
          at: now,
          by: context.auth!.uid,
          from: prev,
          to: next
        })
      });

      return { ok: true, message: "status updated", from: prev, to: next };
    });
  });
