import * as admin from "firebase-admin";
import { onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";

setGlobalOptions({ region: "asia-northeast3" }); // 서울
admin.initializeApp();

export const ping = onCall(async () => ({ ok: true, msg: "pong" }));