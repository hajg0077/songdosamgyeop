import * as functions from "firebase-functions";
import express, { Request, Response } from "express";

import { admin, auth, db } from "./firebase";
import { sendToTopic } from "./fcm";
import { InicisClient, parseNoti } from "./pg/inicis";

export { makeUserHQ } from "./admin/makeUserHQ";
// ──────────────────────────────────────────────────────────────
// Config
// ──────────────────────────────────────────────────────────────
const app = express();
app.use(express.urlencoded({ extended: true }));

const region = "asia-northeast3";
const HQ_TOPIC = process.env.HQ_TOPIC || "hq";

app.post("/inicis/noti", async (req: Request, res: Response) => {
  try {
    const { ok, tid, orderId, amount } = parseNoti(req.body);
    if (!orderId || !tid) return res.status(200).send("INVALID");

    const ref = db.collection("orders").doc(orderId);
    const snap = await ref.get();

    if (!snap.exists) {
      console.warn("inicis noti for unknown order", orderId, tid);
      return res.status(200).send("OK");
    }

    if (ok) {
      await ref.update({
        paymentGateway: "INICIS",
        paymentTid: tid,
        paymentStatus: "PAID",
        paidAt: admin.firestore.FieldValue.serverTimestamp(),
        paymentMessage: "notiURL 승인",
      });

      const order = snap.data()!;
      const branchName = String(order.branchName || "-");
      const branchId = String(order.branchId || "");
      const finalAmt = Number(order.totalAmount ?? amount ?? 0);

      await sendToTopic(
        HQ_TOPIC,
        "결제 승인",
        `${branchName} · 주문 ${orderId} · ${finalAmt.toLocaleString("ko-KR")}원`,
        {
          type: "PAYMENT_APPROVED",
          orderId,
          branchId,
          paymentStatus: "PAID",
          eventId: `paid:${orderId}:${tid}`,
        }
      );
    } else {
      await ref.update({
        paymentGateway: "INICIS",
        paymentTid: tid,
        paymentStatus: "FAILED",
        paymentMessage: `notiURL status=${req.body?.P_STATUS ?? "?"}`,
      });
    }

    return res.status(200).send("OK");
  } catch (e) {
    console.error(e);
    return res.status(200).send("ERR");
  }
});

export const inicisWebhook = functions.region(region).https.onRequest(app);

const BOOTSTRAP_SECRET = (functions.config().bootstrap?.secret || "") as string;
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
// 0) 최초 HQ 부트스트랩 (1회용 권장)
// ──────────────────────────────────────────────────────────────
export const bootstrapHqAdmin = functions.region(region).https.onCall(async (data, context) => {
  const secret = String(data?.secret || "");
  const uid = String(data?.uid || "");

  if (!secret || !uid) {
    throw new functions.https.HttpsError("invalid-argument", "secret, uid required");
  }
  if (!BOOTSTRAP_SECRET || secret !== BOOTSTRAP_SECRET) {
    throw new functions.https.HttpsError("permission-denied", "bad secret");
  }

  // ✅ 이미 HQ가 있으면 더 이상 부트스트랩 금지
  const existing = await db.collection("users").where("role", "==", "HQ").limit(1).get();
  if (!existing.empty) {
    throw new functions.https.HttpsError("failed-precondition", "HQ already exists");
  }

  // 유저 존재 확인
  const user = await auth.getUser(uid).catch(() => null);
  if (!user) {
    throw new functions.https.HttpsError("not-found", "user not found");
  }

  // 1) Custom Claims 부여
  await auth.setCustomUserClaims(uid, { role: "HQ", admin: true });

  // 2) users/{uid} 캐시 문서 기록
  await db.collection("users").doc(uid).set(
    {
      role: "HQ",
      isAdmin: true,
      email: user.email || null,
      name: user.displayName || null,
      bootstrappedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    { merge: true }
  );

  return { ok: true, uid };
});

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
  const branchCode = String(data.branchCode ?? "").trim();
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

    // 🔔 지사 개인 알림 추가
    const tokenSnap = await db.collection("userTokens").doc(user.uid).get();

    if (tokenSnap.exists) {
      const token = tokenSnap.data()!.token;

      await admin.messaging().send({
        token,
        notification: {
          title: "지사 가입 승인 완료",
          body: "본사에서 가입을 승인했습니다. 이제 로그인할 수 있어요."
        },
        data: {
          type: "REGISTRATION_APPROVED",
          branchId,
          eventId: `reg-approved-user:${user.uid}:${Date.now()}`
        }
      });
    }


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

    const tokenSnap = await db.collection("userTokens").doc(docId).get();

    if (tokenSnap.exists) {
      const token = tokenSnap.data()!.token;

      await admin.messaging().send({
        token,
        notification: {
          title: "지사 가입 반려",
          body: reason || "본사에서 가입이 반려되었습니다."
        },
        data: {
          type: "REGISTRATION_REJECTED",
          eventId: `reg-rejected-user:${docId}:${Date.now()}`
        }
      });
    }

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
      eventId: `reg-created:${uid}:${snap.createTime.toMillis()}`
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
