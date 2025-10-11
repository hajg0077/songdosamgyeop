package com.songdosamgyeop.order.payment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.play.integrity.internal.b
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.payment.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

/**
 * KG 이니시스(테스트 MID) 모바일 결제 호출 화면.
 * - WebView로 표준 결제 페이지 POST 호출
 * - 결제 완료/취소 후 앱스킴(songdosamgyeop://inicis/return)으로 복귀
 * - 복귀 시 (oid, tid) 파라미터를 파싱하여 서버 검증 호출
 *
 * ⚠️ 실제 상용 적용 시 KG 이니시스 최신 가이드의 결제 URL/파라미터/보안 지침을 준수하세요.
 */
@AndroidEntryPoint
class InicisPaymentActivity : AppCompatActivity() {

    private val vm: PaymentViewModel by viewModels()
    private lateinit var txtTitle: TextView
    private lateinit var txtStatus: TextView
    private lateinit var webView: WebView

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    private lateinit var orderId: String     // Firestore 문서 ID
    private lateinit var merchantUid: String // = oid (우리 주문번호)
    private var amount: Long = 0L
    private lateinit var title: String

    private lateinit var branchName: String

    private var buyerEmail: String = ""
    private var buyerTel: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicis_payment)

        txtTitle = findViewById(R.id.tv_payment_Title)
        txtStatus = findViewById(R.id.tv_payment_Status)
        webView = findViewById(R.id.payment_webView)

        // 인자 수신
        orderId = intent.getStringExtra("orderId") ?: run { finish(); return }
        title = intent.getStringExtra("title") ?: "주문 결제"
        amount = intent.getLongExtra("amount", 0L)
        branchName = intent.getStringExtra("branchName") ?: "주문 결제"
        buyerEmail = intent.getStringExtra("buyerEmail") ?: ""
        buyerTel = intent.getStringExtra("buyerTel") ?: ""

        txtTitle.text = "$title - ${nf.format(amount)}원"

        // 주문 고유번호(merchantUid / oid)
        merchantUid = "SDSG-${System.currentTimeMillis()}-$orderId"

        initObservers()
        loadPaymentPage()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            vm.events.collectLatest { ev ->
                when (ev) {
                    is PaymentViewModel.UiEvent.Verifying -> {
                        txtStatus.text = "결제 검증 중…"
                    }
                    is PaymentViewModel.UiEvent.Success -> {
                        txtStatus.text = ev.message
                        setResult(RESULT_OK)
                        finish()
                    }
                    is PaymentViewModel.UiEvent.Failure -> {
                        txtStatus.text = ev.message
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadPaymentPage() {
        // WebView 세팅
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                Log.d("Inicis", "Navigate: $uri")
                // 앱 복귀 스킴 처리: songdosamgyeop://inicis/return?oid=...&tid=...&resultCode=0000
                if (uri.scheme == Env.APP_SCHEME && uri.host == "inicis" && uri.path == "/return") {
                    handleReturnUri(uri)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                txtStatus.text = "결제창 로드 중…"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                txtStatus.text = "결제창 준비 완료"
            }
        }

        // ⚠️ 아래는 "개념 스텁"입니다.
        // 실제 테스트/샌드 환경 결제 엔드포인트와 파라미터는 KG 이니시스 최신 문서 기준으로 교체하세요.
        val inicisUrl = "https://mobile.inicis.com/some/test/endpoint" // ← 가이드에 맞게 변경
        val returnUrl = "${Env.APP_SCHEME}://inicis/return"

        // 최소 파라미터 샘플 (샌드 규격에 맞게 보완)
        val form = mapOf(
            "mid" to Env.INICIS_MID,
            "oid" to merchantUid,
            "price" to amount.toString(),
            "goodname" to title,
            "buyername" to "지사담당자",
            "returnurl" to returnUrl
        )

        // POST 폼 서브밋 (간단 구현)
        val html = buildString {
            append("<html><body onload='document.f.submit()'>")
            append("<form name='f' method='post' action='$inicisUrl'>")
            form.forEach { (k, v) ->
                val esc = URLEncoder.encode(v, "UTF-8")
                append("<input type='hidden' name='$k' value='$esc'/>")
            }
            append("</form></body></html>")
        }

        webView.loadDataWithBaseURL(inicisUrl, html, "text/html", "utf-8", null)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        val uri = intent?.data ?: return
        if (uri.scheme == Env.APP_SCHEME && uri.host == "inicis" && uri.path == "/return") {
            handleReturnUri(uri)
        }
    }

    private fun handleReturnUri(uri: Uri) {
        val resultCode = uri.getQueryParameter("resultCode") ?: uri.getQueryParameter("result_code")
        val tid = uri.getQueryParameter("tid") ?: ""
        val oid = uri.getQueryParameter("oid") ?: merchantUid

        Log.d("Inicis", "return: resultCode=$resultCode, tid=$tid, oid=$oid")

        if (resultCode == "0000" && tid.isNotBlank()) {
            // ✅ 서버 검증 호출 (tid/oid 기준)
            vm.verifyAndApply(orderId = orderId, merchantUid = oid, txId = tid)
        } else {
            vm.notifyFailed("결제 실패/취소 (code=$resultCode)")
        }
    }
}