import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import * as express from "express";
import { sendToTopic } from "./fcm";
import { InicisClient, parseNoti } from "./pg/inicis";

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// ──────────────────────────────────────────────────────────────
// Config
// ──────────────────────────────────────────────────────────────
const region = "asia-northeast3";
const HQ_TOPIC = process.env.HQ_TOPIC || "hq";
const BRANCH_TOPIC = (branchId: string) => `branch-${branchId}`;

// ──────────────────────────────────────────────────────────────
/** HQ 권한 체크 */
// ──────────────────────────────────────────────────────────────
function assertHQ(context: functions.https.CallableContext) {
  const role = context.auth?.token?.role;
  if (!context.auth || role !== "HQ") {
    throw new functions.https.HttpsError("permission-denied", "HQ only");
  }
}

// ──────────────────────────────────────────────────────────────
/** registrations/{doc} 로드 헬퍼 */
// ──────────────────────────────────────────────────────────────
async function loadRegistration(docId: string) {
  const ref = db.collection("registrations").doc(docId);
  const snap = await ref.get();
  if (!snap.exists) throw new functions.https.HttpsError("not-found", "registration not found");
  return { ref, data: snap.data()! };
}

// ──────────────────────────────────────────────────────────────
// ① HQ: 가입 승인/반려/리셋
// ──────────────────────────────────────────────────────────────

/**
 * 가입 승인:
 * - 사용자 생성/업데이트
 * - 커스텀 클레임(role=BRANCH, branchId) 부여
 * - branches/{branchId} 생성(주소는 roadAddr/zipNo/detail 3필드만)
 * - users/{uid} 캐시 업데이트
 * - registrations 상태를 APPROVED로 변경
 * - HQ 토픽 알림
 */
export const hqApproveRegistration = functions.region(region).https.onCall(async (data, context) => {
  assertHQ(context);

  const docId = String(data.docId || "");
  if (!docId) throw new functions.https.HttpsError("invalid-argument", "docId required");

  const { ref, data: reg } = await loadRegistration(docId);
  if (reg.status !== "PENDING") {
    throw new functions.https.HttpsError("failed-precondition", "already processed");
  }

  const email = String(reg.email || "");
  const name = String(reg.name || "");

  // HQ에서 최종 지사명/전화/주소를 넘길 수 있고, 없으면 신청 데이터 사용
  const branchName = String((data.branchName ?? reg.branchName ?? "") || "");
  const branchTel = String((data.branchTel ?? reg.branchTel ?? "") || "");

  // ✅ 주소는 3필드만 사용
  const address = {
    roadAddr: String(data.address?.roadAddr ?? reg.address?.roadAddr ?? ""),
    zipNo: String(data.address?.zipNo ?? reg.address?.zipNo ?? ""),
    detail: String(data.address?.detail ?? reg.address?.detail ?? "")
  };

  // branchId 결정
  const branchId = branchCode || `BR_${ref.id}`;

  // 사용자 생성/갱신
  let user;
  try {
    user = await auth.getUserByEmail(email);
    if (name && user.displayName !== name) {
      await auth.updateUser(user.uid, { displayName: name });
    }
  } catch {
    user = await auth.createUser({
      email,
      displayName: name || undefined,
      emailVerified: false,
      disabled: false
    });
  }

  // 커스텀 클레임 부여
  await auth.setCustomUserClaims(user.uid, { role: "BRANCH", branchId });

  // ✅ branches/{branchId}: 주소 3필드만 저장
  await db
    .collection("branches")
    .doc(branchId)
    .set(
      {
        branchId,
        name: branchName || branchId,
        tel: branchTel || null,
        address, // roadAddr/zipNo/detail
        status: "ACTIVE",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );

  // users/{uid} 캐시
  await db
    .collection("users")
    .doc(user.uid)
    .set(
      {
        email,
        name,
        role: "BRANCH",
        branchId,
        branchName: branchName || branchId,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );

  // registrations 상태 갱신
  await ref.update({
    status: "APPROVED",
    approvedAt: admin.firestore.FieldValue.serverTimestamp(),
    approvedBy: context.auth!.uid
  });

  // HQ 전체 알림
  await sendToTopic(HQ_TOPIC, "지사 가입 승인 완료", `${branchName || branchId || user.uid} 승인 처리`, {
    type: "REGISTRATION_APPROVED",
    uid: user.uid,
    branchId,
    eventId: `reg-approved:${user.uid}:${Date.now()}`
  });

  return { message: "approved", uid: user.uid, branchId };
});

/** 가입 반려 */
export const hqRejectRegistration = functions.region(region).https.onCall(async (data, context) => {
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
    rejectedBy: context.auth!.uid
  });

  // (선택) HQ 공유
  await sendToTopic(HQ_TOPIC, "지사 가입 반려", `${reg.branchName || reg.email || docId} 반려 처리`, {
    type: "REGISTRATION_REJECTED",
    uid: docId,
    eventId: `reg-rejected:${docId}:${Date.now()}`
  });

  return { message: "rejected" };
});

/** 가입 상태 리셋 */
export const hqResetRegistration = functions.region(region).https.onCall(async (data, context) => {
  assertHQ(context);
  const docId = String(data.docId || "");
  if (!docId) throw new functions.https.HttpsError("invalid-argument", "docId required");

  const { ref } = await loadRegistration(docId);
  await ref.update({
    status: "PENDING",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  return { message: "reset to PENDING" };
});

// ──────────────────────────────────────────────────────────────
// ② HQ: 주문 상태 전이 (PLACED/APPROVED/REJECTED/SHIPPED/DELIVERED)
// ──────────────────────────────────────────────────────────────
export const hqUpdateOrderStatus = functions.region(region).https.onCall(async (data, context) => {
  assertHQ(context);
  const orderId = String(data.orderId || "");
  const next = String(data.nextStatus || "").toUpperCase();
  if (!orderId || !next) {
    throw new functions.https.HttpsError("invalid-argument", "orderId, nextStatus required");
  }

  const allowed = new Set(["PLACED", "APPROVED", "REJECTED", "SHIPPED", "DELIVERED"]);
  if (!allowed.has(next)) {
    throw new functions.https.HttpsError("invalid-argument", "invalid status");
  }

  const canTransit = (from: string, to: string) => {
    const F = (from || "PLACED").toUpperCase();
    const T = to.toUpperCase();
    if (F === "PLACED") return T === "APPROVED" || T === "REJECTED";
    if (F === "APPROVED") return T === "SHIPPED";
    if (F === "SHIPPED") return T === "DELIVERED";
    return false;
  };

  const ref = db.collection("orders").doc(orderId);
  const result = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new functions.https.HttpsError("not-found", "order not found");
    const order = snap.data()!;
    const prev = (order.status || "PLACED").toString().toUpperCase();
    const branchId = String(order.branchId || "");
    const branchName = String(order.branchName || "-");

    if (!canTransit(prev, next)) {
      throw new functions.https.HttpsError("failed-precondition", `transition not allowed: ${prev} → ${next}`);
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

    return { from: prev, to: next, branchId, branchName };
  });

  // 지사 토픽 알림
  if (result.branchId) {
    const topic = BRANCH_TOPIC(result.branchId);
    const title = "주문 상태 변경";
    const body = `지사 ${result.branchName} 주문 ${orderId} 상태가 ${result.to}로 변경되었습니다.`;
    await sendToTopic(topic, title, body, {
      type: "ORDER_STATUS",
      orderId,
      branchId: result.branchId,
      status: result.to,
      eventId: `order-status:${orderId}:${result.to}:${Date.now()}`
    });
  }

  return { ok: true, message: "status updated", ...result };
});

// ──────────────────────────────────────────────────────────────
// ③ 이니시스 결제 검증 (Callable) — { orderId, tid } 입력
//    서버가 INIAPI로 TID 재조회하여 확정
// ──────────────────────────────────────────────────────────────
export const verifyInicisPayment = functions.region(region).https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "auth required");

  const orderId = String(data?.orderId || "");
  const tid = String(data?.tid || "");
  if (!orderId || !tid) {
    throw new functions.https.HttpsError("invalid-argument", "orderId, tid required");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const snap = await orderRef.get();
  if (!snap.exists) throw new functions.https.HttpsError("not-found", "order not found");

  const order = snap.data()!;
  const expected = Number(order.totalAmount ?? 0);
  const branchId = String(order.branchId || "");
  const branchName = String(order.branchName || "-");

  const client = new InicisClient();
  const vr = await client.verifyByTid(tid);

  const ok = vr.ok && vr.status === "paid" && (!expected || !vr.amount || vr.amount === expected);

  await orderRef.update({
    paymentGateway: "INICIS",
    paymentTid: tid,
    paymentStatus: ok ? "PAID" : "FAILED",
    paidAt: ok ? admin.firestore.FieldValue.serverTimestamp() : null,
    paymentMessage: ok
      ? "결제 완료(INICIS verify)"
      : `검증 실패: ${vr.status} (amt=${vr.amount ?? "?"}, expected=${expected})`
  });

  if (ok) {
    await sendToTopic(HQ_TOPIC, "결제 승인", `${branchName} · 주문 ${orderId} · ${expected.toLocaleString("ko-KR")}원`, {
      type: "PAYMENT_APPROVED",
      orderId,
      branchId,
      paymentStatus: "PAID",
      eventId: `paid:${orderId}:${tid}`
    });
  }

  return { ok, status: vr.status, amount: vr.amount ?? null, tid };
});

// ──────────────────────────────────────────────────────────────
// ④ 이니시스 notiURL(Webhook) — x-www-form-urlencoded
//    성공 처리 시 "OK" 그대로 응답(재전송 중지)
// ──────────────────────────────────────────────────────────────
const app = express();
app.use(express.urlencoded({ extended: true }));

app.post("/inicis/noti", async (req, res) => {
  try {
    // (권장) 이니시스 발신 IP 화이트리스트 검증 로직 추가 가능
    const { ok, tid, orderId, amount } = parseNoti(req.body);
    if (!orderId || !tid) return res.status(200).send("INVALID"); // 재시도 유도

    const ref = db.collection("orders").doc(orderId);
    const snap = await ref.get();
    if (!snap.exists) {
      console.warn("inicis noti for unknown order", orderId, tid);
      return res.status(200).send("OK"); // 재전송 방지
    }

    if (ok) {
      await ref.update({
        paymentGateway: "INICIS",
        paymentTid: tid,
        paymentStatus: "PAID",
        paidAt: admin.firestore.FieldValue.serverTimestamp(),
        paymentMessage: "notiURL 승인"
      });

      const order = snap.data()!;
      const branchName = String(order.branchName || "-");
      const branchId = String(order.branchId || "");
      const finalAmt = Number(order.totalAmount ?? amount ?? 0);

      await sendToTopic(HQ_TOPIC, "결제 승인", `${branchName} · 주문 ${orderId} · ${finalAmt.toLocaleString("ko-KR")}원`, {
        type: "PAYMENT_APPROVED",
        orderId,
        branchId,
        paymentStatus: "PAID",
        eventId: `paid:${orderId}:${tid}`
      });
    } else {
      await ref.update({
        paymentGateway: "INICIS",
        paymentTid: tid,
        paymentStatus: "FAILED",
        paymentMessage: `notiURL status=${req.body?.P_STATUS ?? "?"}`
      });
    }

    return res.status(200).send("OK"); // 반드시 "OK"
  } catch (e) {
    console.error(e);
    return res.status(200).send("ERR"); // 재시도 유도
  }
});

export const inicisWebhook = functions.region(region).https.onRequest(app);

// ──────────────────────────────────────────────────────────────
// ⑤ 신규 가입 신청 트리거: HQ 알림 + 장치 락(멱등)
// ──────────────────────────────────────────────────────────────
export const onRegistrationCreated = functions
  .region(region)
  .firestore.document("registrations/{uid}")
  .onCreate(async (snap, ctx) => {
    const d = snap.data() || {};
    const uid = ctx.params.uid as string;
    const email = String(d.email || "");
    const tel = String(d.branchTel || "");
    const installationId = (d.installationId ? String(d.installationId) : "").trim();

    // HQ 토픽 알림
    await sendToTopic(HQ_TOPIC, "새 지사 가입 신청", `${email}${tel ? ` (${tel})` : ""}`, {
      type: "REGISTRATION_CREATED",
      uid,
      email,
      branchTel: tel,
      eventId: `reg-created:${uid}:${snap.createTime.toMillis?.() ?? Date.now()}`
    });

    // 장치 락(멱등)
    if (installationId) {
      const devRef = db.collection("devices").doc(installationId);
      const devSnap = await devRef.get();
      if (!devSnap.exists) {
        await devRef.set({
          registeredUid: uid,
          email,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          via: "registration_onCreate"
        });
      }
    }
  });
