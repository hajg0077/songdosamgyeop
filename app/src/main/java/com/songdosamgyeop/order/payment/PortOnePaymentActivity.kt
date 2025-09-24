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

        // SDK 초기화
        Iamport.init(this)

        // 전달 파라미터
        val orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
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
            // 현재 버전은 문자열 파라미터를 기대
            val req = IamPortRequest(
                pg = "html5_inicis",
                pay_method = "card",
                name = title,
                merchant_uid = merchantUid,
                amount = amount.toString(),
                app_scheme = Env.APP_SCHEME,
                buyer_name = buyerName,
                buyer_email = buyerEmail,
                buyer_tel = buyerTel
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
                        NotificationHelper.notify(
                            this,
                            NotificationChannels.Orders,
                            getString(R.string.push_title_paid),
                            getString(R.string.push_body_paid)
                        )
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