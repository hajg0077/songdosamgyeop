package com.songdosamgyeop.order.ui.payment

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iamport.sdk.data.sdk.IamPortRequest
import com.iamport.sdk.data.sdk.IamPortResponse
import com.iamport.sdk.data.sdk.PG
import com.iamport.sdk.data.sdk.PayMethod
import com.iamport.sdk.domain.core.Iamport
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.databinding.ActivityPortonePaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * PortOne 결제 전용 Activity
 * - Intent extras: orderId(String), title(String), amount(Long), buyerName/email/tel(옵션)
 * - 외부 앱/브라우저 전환 후 스킴 복귀를 Activity에서 직접 처리(Manifest <intent-filter>)
 */
@AndroidEntryPoint
class PortOnePaymentActivity : AppCompatActivity() {

    private lateinit var b: ActivityPortonePaymentBinding
    private val vm: PaymentViewModel by viewModels()
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPortonePaymentBinding.inflate(layoutInflater)
        setContentView(b.root)

        // SDK 초기화 (Activity/Fragment 에서만 가능)
        Iamport.init(this)

        // 전달 파라미터
        val orderId = intent.getStringExtra("orderId") ?: run {
            finish(); return
        }
        val title = intent.getStringExtra("title") ?: "주문 결제"
        val amount = intent.getLongExtra("amount", 0L)
        val buyerName = intent.getStringExtra("buyerName")
        val buyerEmail = intent.getStringExtra("buyerEmail")
        val buyerTel = intent.getStringExtra("buyerTel")

        // UI
        b.txtTitle.text = title
        b.txtAmount.text = "${nf.format(amount)}원"

        val userCode = Env.PORTONE_USER_CODE   // 예: "imp12345678"
        val merchantUid = "muid_${System.currentTimeMillis()}_${orderId}"

        b.btnPay.setOnClickListener {
            // PortOne 샘플과 동일한 Request 타입(data.sdk.*)
            val req = IamPortRequest(
                pg = PG.html5_inicis,
                pay_method = PayMethod.card,
                name = title,
                merchant_uid = merchantUid,
                amount = amount.toString(),      // 문자열
                app_scheme = Env.APP_SCHEME,     // 예: "songdo-pay"
                buyer_name = buyerName,
                buyer_email = buyerEmail,
                buyer_tel = buyerTel
            )

            Iamport.payment(userCode, iamPortRequest = req) { resp: IamPortResponse? ->
                val success = resp?.success == true
                val impUid = resp?.imp_uid
                val msg = resp?.error_msg ?: resp?.pg_provider

                if (success && impUid != null) {
                    vm.verifyAndApply(orderId, merchantUid, impUid)
                } else {
                    vm.notifyFailed(msg ?: "결제 실패 또는 취소")
                }
            }
        }

        b.btnClose.setOnClickListener { finish() }

        // 검증 이벤트 수신
        lifecycleScope.launch {
            vm.events.collectLatest { ev ->
                when (ev) {
                    is PaymentViewModel.UiEvent.Verifying -> {
                        b.txtStatus.text = "결제 검증 중..."
                        b.btnPay.isEnabled = false
                    }
                    is PaymentViewModel.UiEvent.Success -> {
                        b.txtStatus.text = ev.message
                        setResult(RESULT_OK)
                        finish()
                    }
                    is PaymentViewModel.UiEvent.Failure -> {
                        b.txtStatus.text = ev.message
                        b.btnPay.isEnabled = true
                    }
                }
            }
        }
    }
}