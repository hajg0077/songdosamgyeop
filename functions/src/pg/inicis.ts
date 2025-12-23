import axios from "axios";
import crypto from "crypto";

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
  private signKey: string;
  private aesKey?: string;
  private aesIv?: string;
  private verifyUrl: string;

  constructor(opts?: {
    mid?: string;
    signKey?: string;
    aesKey?: string;
    aesIv?: string;
    verifyUrl?: string;
  }) {
    this.mid = opts?.mid ?? (process.env.INICIS_MID ?? "");
    this.signKey = opts?.signKey ?? (process.env.INICIS_SIGN_KEY ?? "");
    this.aesKey = opts?.aesKey ?? process.env.INICIS_AES_KEY;
    this.aesIv = opts?.aesIv ?? process.env.INICIS_AES_IV;
    this.verifyUrl =
      opts?.verifyUrl ??
      process.env.INICIS_VERIFY_URL ??
      "https://iniapi.inicis.com/api/v1/payinquiry";
  }

  /** AES-CBC-PKCS5Padding (옵션) */
  private aesEncrypt(plain: string): string {
    if (!this.aesKey || !this.aesIv) throw new Error("AES key/iv not configured");
    const cipher = crypto.createCipheriv(
      "aes-128-cbc",
      Buffer.from(this.aesKey, "utf8"),
      Buffer.from(this.aesIv, "utf8")
    );
    const enc = Buffer.concat([cipher.update(Buffer.from(plain, "utf8")), cipher.final()]);
    return enc.toString("base64");
  }

  async verifyByTid(tid: string): Promise<InicisVerifyResult> {
    const timestamp = Date.now().toString();

    // ⚠️ 실제 서명 규칙은 계약/매뉴얼에 맞춰 조정
    const signPayload = `${this.mid}${tid}${timestamp}${this.signKey}`;
    const sign = crypto.createHash("sha256").update(signPayload, "utf8").digest("hex");

    const params = new URLSearchParams();
    params.append("type", "PayInquiry");
    params.append("mid", this.mid);
    params.append("tid", tid);
    params.append("timestamp", timestamp);
    params.append("sign", sign);

    const { data } = await axios.post(this.verifyUrl, params.toString(), {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      timeout: 10000,
    });

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
 * notiURL 파라미터 파싱
 * 성공 처리 시 반드시 "OK" 문자열 응답해야 재전송 중단
 */
export function parseNoti(body: any) {
  const P_STATUS = String(body?.P_STATUS ?? "");
  const P_TID = String(body?.P_TID ?? "");
  const P_OID = String(body?.P_OID ?? "");
  const P_AMT = Number(body?.P_AMT ?? 0);
  const P_AUTH_DT = String(body?.P_AUTH_DT ?? "");

  const ok = P_STATUS === "00";
  return { ok, P_STATUS, tid: P_TID, orderId: P_OID, amount: P_AMT, approvedAt: P_AUTH_DT };
}
