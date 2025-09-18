import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import axios from "axios";

admin.initializeApp();
const db = admin.firestore();
const auth = admin.auth();

// 서울 리전
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

/** ───────────────────────── HQ: 신청 승인/반려/리셋 ───────────────────────── */

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

/** ───────────────────────── HQ: 주문 상태 전이 ─────────────────────────
 * 프로젝트 규격: PLACED / APPROVED / REJECTED / SHIPPED / DELIVERED
 */
export const hqUpdateOrderStatus = functions
  .region(region)
  .https.onCall(async (data, context) => {
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
      if (F === "PLACED")  return T === "APPROVED" || T === "REJECTED";
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
      const prev = (order.status || "PLACED").toString().toUpperCase();

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

/** ───────────────────────── PortOne 결제 검증(onCall) + Webhook(옵션) ─────────────────────────
 * 환경 변수: firebase functions:config:set portone.key="___" portone.secret="___"
 */
const PORTONE_API_KEY =
  process.env.PORTONE_API_KEY || (functions.config().portone && functions.config().portone.key);
const PORTONE_API_SECRET =
  process.env.PORTONE_API_SECRET || (functions.config().portone && functions.config().portone.secret);

async function getPortOneToken(): Promise<string> {
  const res = await axios.post("https://api.iamport.kr/users/getToken", {
    imp_key: PORTONE_API_KEY,
    imp_secret: PORTONE_API_SECRET
  });
  const token = res.data?.response?.access_token;
  if (!token) throw new Error("PortOne token error");
  return token;
}

async function getPayment(impUid: string, token: string) {
  const res = await axios.get(`https://api.iamport.kr/payments/${impUid}`, {
    headers: { Authorization: token }
  });
  return res.data.response;
}

/** onCall: 클라에서 impUid/merchantUid 전달 → PortOne 검증 후 orders/{orderId} 반영 */
export const verifyPortOnePayment = functions
  .region(region)
  .https.onCall(async (data, context) => {
    // 인증 사용자는 BRANCH(본인 주문), HQ 모두 호출 가능하도록 별도 권한 제한은 두지 않음
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "auth required");
    }

    const orderId = String(data?.orderId || "");
    const merchantUid = String(data?.merchantUid || "");
    const impUid = String(data?.impUid || "");

    if (!orderId || !merchantUid || !impUid) {
      throw new functions.https.HttpsError("invalid-argument", "params missing");
    }

    const token = await getPortOneToken();
    const payment = await getPayment(impUid, token);

    const ok = payment?.status === "paid" && payment?.merchant_uid === merchantUid;
    const method = payment?.pay_method || null;
    const paidAtMs = payment?.paid_at ? payment.paid_at * 1000 : 0;

    // Firestore 업데이트
    await db.collection("orders").doc(orderId).update({
      merchantUid,
      paymentStatus: ok ? "PAID" : "FAILED",
      paymentTxId: impUid,
      paymentMethod: method,
      paidAt: ok ? admin.firestore.Timestamp.fromMillis(paidAtMs) : null,
      paymentMessage: ok ? "결제 완료(PortOne)" : `검증 실패: ${payment?.status ?? "unknown"}`
    });

    return {
      ok, method, paidAt: paidAtMs, impUid, message: ok ? "OK" : "NOT_PAID"
    };
  });

/** Webhook(옵션): PortOne 대시보드에 등록 시 상태 동기화 자동화 */
export const portoneWebhook = functions
  .region(region)
  .https.onRequest(async (req, res) => {
    try {
      const { imp_uid, merchant_uid, status } = req.body || {};
      if (!imp_uid || !merchant_uid) {
        res.status(400).send({ ok: false, error: "bad request" });
        return;
      }
      const token = await getPortOneToken();
      const payment = await getPayment(imp_uid, token);
      const ok = payment?.status === "paid";
      // merchant_uid 규칙: muid_{ts}_{orderId}
      const orderId = (merchant_uid as string).split("_").pop();

      if (orderId) {
        await db.collection("orders").doc(orderId).update({
          merchantUid: merchant_uid,
          paymentStatus: ok ? "PAID" : (status?.toUpperCase() || "FAILED"),
          paymentTxId: imp_uid,
          paymentMethod: payment?.pay_method || null,
          paidAt: ok && payment?.paid_at
            ? admin.firestore.Timestamp.fromMillis(payment.paid_at * 1000)
            : null,
          paymentMessage: ok ? "Webhook 결제 완료" : `Webhook 상태: ${status || "unknown"}`
        });
      }
      res.status(200).send({ ok: true });
    } catch (e: any) {
      console.error(e);
      res.status(500).send({ ok: false, error: e.message });
    }
  });
