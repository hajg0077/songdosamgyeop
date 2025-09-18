package com.songdosamgyeop.order.ui.payment

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iamport.sdk.data.sdk.PayMethod
import com.iamport.sdk.data.sdk.IamportPayment
import com.iamport.sdk.data.sdk.IamportResponse
import com.iamport.sdk.domain.core.Iamport
import com.songdosamgyeop.order.databinding.ActivityPortonePaymentBinding
import com.songdosamgyeop.order.Env
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * PortOne 결제 화면(Activity)
 * - UI는 단순 버튼/금액 표시
 * - 콜백 결과는 PaymentViewModel로 위임(검증/반영)
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

        Iamport.init(this)
        // 전달 파라미터
        val orderId = intent.getStringExtra("orderId")!!
        val title = intent.getStringExtra("title") ?: "주문 결제"
        val amount = intent.getLongExtra("amount", 0L)
        val buyerName = intent.getStringExtra("buyerName").orEmpty()
        val buyerEmail = intent.getStringExtra("buyerEmail").orEmpty()
        val buyerTel = intent.getStringExtra("buyerTel").orEmpty()

        b.txtTitle.text = title
        b.txtAmount.text = "${nf.format(amount)}원"

        // SDK 초기화(권장: Application에서 Iamport.init(this))
        Iamport.init(this)

        val merchantUid = "muid_${System.currentTimeMillis()}_${orderId}"

        b.btnPay.setOnClickListener {
            val payment = IamportPayment(
                pg = "html5_inicis",                 // 필요시 포트원 대시보드 설정에 맞춰 변경
                payMethod = PayMethod.card,
                name = title,
                merchantUid = merchantUid,
                amount = BigDecimal(amount),
                appScheme = Env.APP_SCHEME,
                buyerName = buyerName.ifBlank { null },
                buyerEmail = buyerEmail.ifBlank { null },
                buyerTel = buyerTel.ifBlank { null }
            )

            Iamport.payment(
                activity = this,
                payment = payment
            ) { resp: IamportResponse? ->
                val success = resp?.success == true
                val impUid = resp?.impUid
                val msg = resp?.errorMsg ?: resp?.pgProvider?.name

                if (success && impUid != null) {
                    vm.verifyAndApply(orderId, merchantUid, impUid)
                } else {
                    vm.markFailed(orderId, merchantUid, msg ?: "결제 실패 또는 취소")
                }
            }
        }

        b.btnClose.setOnClickListener { finish() }

        // 이벤트 구독: 성공 시 종료, 실패 시 토스트/스낵바 등 표시(여기선 텍스트)
        lifecycle.addObserver(Iamport.lifecycleObserver(this)) // 안전한 생명주기 연결
        collectEvents()
    }

    private fun collectEvents() {
        // 간단히 텍스트로 상태 보여줌. 필요하면 Snackbar로 교체
        vm.events.collectWhileStarted(this) { ev ->
            when (ev) {
                is PaymentViewModel.UiEvent.Verifying -> {
                    b.txtStatus.text = "결제 검증 중..."
                    b.btnPay.isEnabled = false
                }
                is PaymentViewModel.UiEvent.Success -> {
                    b.txtStatus.text = ev.message
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

/** LifecycleScope 확장(간단 유틸) */
private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectWhileStarted(
    activity: AppCompatActivity,
    crossinline block: (T) -> Unit
) {
    activity.lifecycle.addObserver(androidx.lifecycle.LifecycleEventObserver { _, ev ->
        // no-op (sample)
    })
    activity.lifecycleScope.launchWhenStarted {
        collect { block(it) }
    }
}