import axios from "axios";
import crypto from "crypto";
import ngeohash from "ngeohash";

/**
 * INICIS INIAPI / notiURL helper
 *
 * ⚠️ INIAPI v1는 NVP(application/x-www-form-urlencoded) 방식이고,
 *    일부 필드는 AES(CBC/PKCS5Padding) 암호화 대상이다. (매뉴얼의 [ENC] 표기)
 *    테스트 도메인: https://stginiapi.inicis.com
 *    운영 도메인  : https://iniapi.inicis.com
 *
 * 꼭 상점관리자에서 발급한 signKey / AES KEY, IV 사용!
 */
export type InicisVerifyResult = {
  ok: boolean;
  tid: string;
  amount?: number;
  status: "paid" | "cancelled" | "fail" | "unknown";
  approvedAt?: string;
  raw?: any;
};

export class InicisClient {
  private mid: string;
  private signKey: string; // 상점 관리자에서 조회 (SHA-256 서명 등에 사용)
  private aesKey?: string; // [ENC] 필드 암호화용 (선택)
  private aesIv?: string;  // [ENC] 필드 암호화용 (선택)
  private verifyUrl: string;

  constructor(opts?: {
    mid?: string;
    signKey?: string;
    aesKey?: string;
    aesIv?: string;
    verifyUrl?: string; // e.g. https://iniapi.inicis.com/api/v1/payinquiry (정확 엔드포인트는 매뉴얼 참조)
  }) {
    this.mid = opts?.mid ?? (process.env.INICIS_MID ?? "");
    this.signKey = opts?.signKey ?? (process.env.INICIS_SIGN_KEY ?? "");
    this.aesKey = opts?.aesKey ?? process.env.INICIS_AES_KEY;
    this.aesIv = opts?.aesIv ?? process.env.INICIS_AES_IV;

    // 매뉴얼/상점설정에 맞춰 환경변수로 주입하도록 설계
    this.verifyUrl =
      opts?.verifyUrl ??
      process.env.INICIS_VERIFY_URL ?? // 운영/스테이징 선택 주입
      "https://iniapi.inicis.com/api/v1/payinquiry"; // 기본값 (실제 경로는 매뉴얼 보고 맞추기)
  }

  /** AES-CBC-PKCS5Padding */
  private aesEncrypt(plain: string): string {
    if (!this.aesKey || !this.aesIv) throw new Error("AES key/iv not configured");
    const cipher = crypto.createCipheriv("aes-128-cbc", Buffer.from(this.aesKey, "utf8"), Buffer.from(this.aesIv, "utf8"));
    const enc = Buffer.concat([cipher.update(Buffer.from(plain, "utf8")), cipher.final()]);
    return enc.toString("base64");
  }

  /**
   * payinquiry (거래 검증) — TID로 재조회
   * 실제 파라미터/서명 규칙은 상점 계약/매뉴얼에 따름 (NVP form-post).
   * - 예시 필드: type, mid, tid, timestamp, sign 등
   * todo
   */
  async verifyByTid(tid: string): Promise<InicisVerifyResult> {
    const timestamp = Date.now().toString();

    // 예시 서명: SHA256(mid + tid + timestamp + signKey) — 실제 규격은 매뉴얼 확인 필요
    const signPayload = `${this.mid}${tid}${timestamp}${this.signKey}`;
    const sign = crypto.createHash("sha256").update(signPayload, "utf8").digest("hex");

    // NVP 본문
    const params = new URLSearchParams();
    // 실제 필드명은 매뉴얼을 맞출 것. (아래는 템플릿)
    params.append("type", "PayInquiry");
    params.append("mid", this.mid);
    params.append("tid", tid);
    params.append("timestamp", timestamp);
    params.append("sign", sign);
    // [ENC] 대상이 있으면 this.aesEncrypt()로 감싸서 추가

    const { data } = await axios.post(this.verifyUrl, params.toString(), {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      timeout: 10000,
      // stg 테스트 환경: https://stginiapi.inicis.com (manual 참고)
    });

    // 응답 파싱
    // - 결과코드/상태 필드명은 매뉴얼에 따름(예: resultCode/resultMsg, status 등)
    // - 여기서는 대표적으로 "승인(성공)"을 paid로 매핑
    const raw = data;
    const resultCode = String(raw.resultCode ?? raw.code ?? "");
    const statusStr = String(raw.status ?? "");
    const amount = Number(raw.price ?? raw.P_AMT ?? raw.amount ?? 0);
    const approvedAt = raw.approvedAt ?? raw.P_AUTH_DT;

    const ok = resultCode === "00" || statusStr.toLowerCase() === "paid";
    let status: InicisVerifyResult["status"] = "unknown";
    if (ok) status = "paid";
    else if (/cancel/i.test(statusStr)) status = "cancelled";
    else if (resultCode && resultCode !== "00") status = "fail";

    return { ok, tid, amount, status, approvedAt, raw };
  }
}

/**
 * notiURL 파라미터 파싱 헬퍼
 * - 대표 파라미터 (지불수단에 따라 달라짐):
 *   P_STATUS(00이면 성공), P_TID(거래번호), P_OID(주문번호), P_AMT(금액), P_AUTH_DT(승인시각) 등
 * - 성공 처리 시 반드시 "OK" 문자열을 그대로 응답해야 재전송이 중단됨.
 */
export function parseNoti(body: any) {
  const P_STATUS = String(body?.P_STATUS ?? "");
  const P_TID = String(body?.P_TID ?? "");
  const P_OID = String(body?.P_OID ?? "");
  const P_AMT = Number(body?.P_AMT ?? 0);
  const P_AUTH_DT = String(body?.P_AUTH_DT ?? "");

  const ok = P_STATUS === "00"; // (일반결제 성공 코드)
  return { ok, P_STATUS, tid: P_TID, orderId: P_OID, amount: P_AMT, approvedAt: P_AUTH_DT };
}

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
    // ✅ HQ 화면에서 최종 지사명/주소/좌표를 받을 수 있게 data 우선
    const branchName = String((data.branchName ?? reg.branchName ?? "") || "");
    const branchTel  = String((data.branchTel  ?? reg.branchTel  ?? "") || "");
        const address = {
          roadAddr: String(data.address?.roadAddr ?? reg.address?.roadAddr ?? ""),
          zipNo:    String(data.address?.zipNo    ?? reg.address?.zipNo    ?? ""),
          detail:   String(data.address?.detail   ?? reg.address?.detail   ?? "")
        };
    // 좌표: HQ가 지정한 값 우선, 없으면 가입자가 준 힌트 사용(있다면)
    const lat = Number.isFinite(Number(data.location?.lat))
      ? Number(data.location.lat) : (Number.isFinite(Number(reg.locationHint?.lat)) ? Number(reg.locationHint.lat) : NaN);
    const lng = Number.isFinite(Number(data.location?.lng))
      ? Number(data.location.lng) : (Number.isFinite(Number(reg.locationHint?.lng)) ? Number(reg.locationHint.lng) : NaN);
    const hasCoord = Number.isFinite(lat) && Number.isFinite(lng);

    const branchCode = reg.branchCode ? String(reg.branchCode) : null;
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

    // ✅ branches/{branchId} 생성/업데이트 (정답 저장소)
    const branchDoc = {
      branchId,
      name: branchName || branchId,
      tel: branchTel || null,
      address,
      ...(hasCoord
        ? { location: { lat, lng, geohash: ngeohash.encode(lat, lng) } }
        : {}), // 좌표 없으면 나중에 보완 가능
      status: "ACTIVE",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      // 최초 생성에만 createdAt 저장하고 싶으면 merge 전에 get해서 exists 체크
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    await db.collection("branches").doc(branchId).set(branchDoc, { merge: true });

    // users/{uid} 캐시
    await db.collection("users").doc(user.uid).set(
      {
        email,
        name,
        role: "BRANCH",
        branchId,
        branchName: branchDoc.name,
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

    // HQ 전체 알림
    await sendToTopic(
      HQ_TOPIC,
      "지사 가입 승인 완료",
      `${branchDoc.name} 승인 처리`,
      {
        type: "REGISTRATION_APPROVED",
        uid: user.uid,
        branchId,
        eventId: `reg-approved:${user.uid}:${Date.now()}`
      }
    );

    return { message: "approved", uid: user.uid, branchId };
  });