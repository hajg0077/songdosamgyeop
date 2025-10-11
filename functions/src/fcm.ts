import * as admin from "firebase-admin";

/**
 * 공통 FCM 토픽 발송 유틸
 * - channelId: "orders" (Android 앱 채널과 일치)
 * - tag: orderId → eventId → timestamp 순으로 fallback
 */
export async function sendToTopic(
  topic: string,
  title: string,
  body: string,
  data: Record<string, string> = {},
  channelId = "orders"
) {
  const msg: admin.messaging.Message = {
    topic,
    notification: { title, body },
    data,
    android: {
      priority: "high",
      notification: {
        channelId,
        tag: data["orderId"] ?? data["eventId"] ?? Date.now().toString(),
      },
    },
    apns: { headers: { "apns-priority": "10" } },
  };
  await admin.messaging().send(msg);
}
