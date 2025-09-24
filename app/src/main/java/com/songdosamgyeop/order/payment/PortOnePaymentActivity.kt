package com.songdosamgyeop.order.ui.payment

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iamport.sdk.data.sdk.IamPortRequest
import com.iamport.sdk.data.sdk.IamPortResponse
import com.iamport.sdk.domain.core.Iamport
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.databinding.ActivityPortonePaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PortOnePaymentActivity : AppCompatActivity() {

    private lateinit var b: ActivityPortonePaymentBinding
    private val vm: PaymentViewModel by viewModels()
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPortonePaymentBinding.inflate(layoutInflater)
        setContentView(b.root)

        Iamport.init(this)

        val orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
        val title = intent.getStringExtra("title") ?: "주문 결제"
        val amount = intent.getLongExtra("amount", 0L)
        val buyerName = intent.getStringExtra("buyerName")
        val buyerEmail = intent.getStringExtra("buyerEmail")
        val buyerTel = intent.getStringExtra("buyerTel")
        val payMethod = intent.getStringExtra("payMethod") ?: "card" // card | trans | vbank

        b.txtTitle.text = title
        b.txtAmount.text = "${nf.format(amount)}원"

        val userCode = Env.PORTONE_USER_CODE
        val merchantUid = "muid_${System.currentTimeMillis()}_${orderId}"

        b.btnPay.setOnClickListener {
            val req = IamPortRequest(
                pg = "html5_inicis",
                pay_method = payMethod,      // ← 카드/계좌이체/가상계좌 선택 반영
                name = title,
                merchant_uid = merchantUid,
                amount = amount.toString(),
                app_scheme = Env.APP_SCHEME,
                buyer_name = buyerName,
                buyer_email = buyerEmail,
                buyer_tel = buyerTel,
                // 가상계좌를 쓰는 경우(옵션) 만료일 등 파라미터 전달
                vbank_due = if (payMethod == "vbank") {
                    // 오늘 + 3일 23:59 (UTC 기준 시/분 문자열; 필요시 계산 로직 보강)
                    // "202512312359" 같은 형태(YYYYMMDDHHmm). 운영에 맞게 계산하세요.
                    null
                } else null
            )

            Iamport.payment(userCode, iamPortRequest = req) { resp: IamPortResponse? ->
                val success = resp?.success == true
                val impUid = resp?.imp_uid
                val msg = resp?.error_msg

                if (success && impUid != null) {
                    vm.verifyAndApply(orderId, merchantUid, impUid)
                } else {
                    vm.notifyFailed(msg ?: "결제 실패 또는 취소")
                }
            }
        }

        b.btnClose.setOnClickListener { finish() }

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