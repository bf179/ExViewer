package com.hippo.ehviewer.ui.login

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.EhApplication.Companion.nonCacheOkHttpClient
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.Screen
import com.hippo.ehviewer.util.bgWork
import com.hippo.ehviewer.util.setDefaultSettings
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

private val jsCode = """
(function() {
    // 使用表单name属性'LOGIN'来获取表单元素
    var form = document.forms['LOGIN'];
    if (form) {
        form.onsubmit = function(event) {
            event.preventDefault();  // 阻止表单的默认提交行为

            // 把表单元素和它们的值转换为一个对象
            var formData = {};
            for (var i = 0; i < form.elements.length; i++) {
                var field = form.elements[i];
                // 排除不需要提交的字段
                if (field.name && !field.disabled && field.type !== 'submit' && field.type !== 'button') {
                    formData[field.name] = field.value;
                }
            }

            // 转换为 JSON 字符串
            var json = JSON.stringify(formData);

            // 发送数据到 Kotlin 端处理
            Android.postFormData(form.action, json);
        };
    }
})();
"""

@SuppressLint("JavascriptInterface")
@Destination<RootGraph>
@Composable
fun AnimatedVisibilityScope.WebViewSignInScreen(navigator: DestinationsNavigator) = Screen(navigator) {
    val state = rememberWebViewState(url = EhUrl.URL_SIGN_IN)
    class OkHttpWebViewClient(
        private val jsCode: String,
    ) : AccompanistWebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()
            if (!request.url.scheme.equals("http", ignoreCase = true) &&
                !request.url.scheme.equals("https", ignoreCase = true)
            ) {
                return null
            }
            val okHttpRequest = Request.Builder()
                .url(url)
                .addHeader("Referer", EhUrl.URL_SIGN_IN)
                .addHeader("Origin", EhUrl.URL_FORUMS)
                .build()

            return try {
                val response = nonCacheOkHttpClient.newCall(okHttpRequest).execute()
                if (response.isSuccessful) {
                    WebResourceResponse(
                        response.header("Content-Type") ?: "text/plain",
                        response.header("Content-Encoding") ?: "utf-8",
                        response.body.byteStream(),
                    ).apply {
                        val reasonPhrase = response.message.takeIf { it.isNotBlank() } ?: "OK"
                        setStatusCodeAndReasonPhrase(response.code, reasonPhrase)
                    }
                } else {
                    response.body.close()
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            view.evaluateJavascript(jsCode, null)
            if (EhCookieStore.hasSignedIn()) {
                postLogin()
                view.destroy()
                launch { bgWork { awaitCancellation() } }
            }
        }
    }
    class WebAppInterface(
        private val webView: WebView,
        private val handler: (WebView, String, String) -> Unit,
    ) {
        @JavascriptInterface
        fun postFormData(url: String, formData: String) {
            Log.d("WebAppInterface", "Received form data: $formData")
            handler(webView, url, formData)
        }
    }
    val okHttpWebViewClient = remember {
        OkHttpWebViewClient(
            jsCode = jsCode,
        )
    }
    fun handlePostRequest(webView: WebView, url: String, formData: String) {
        val formBodyBuilder = FormBody.Builder()
        val formFields = JSONObject(formData)
        formFields.keys().forEach {
            formBodyBuilder.add(it, formFields.getString(it))
        }
        val formBody = formBodyBuilder.build()

        // 构建请求
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Referer", EhUrl.URL_SIGN_IN)
            .addHeader("Origin", EhUrl.URL_FORUMS)
            .build()

        fun parseSetCookieHeader(setCookie: String): Cookie? {
            try {
                // 将Set-Cookie头分割为单个组件
                val cookieComponents = setCookie.split(";").map { it.trim() }
                var name: String? = null
                var value: String? = null
                var domain: String? = null
                var path: String? = null

                // 分析每个组件
                cookieComponents.forEach { component ->
                    val keyValue = component.split("=", limit = 2)
                    when {
                        keyValue[0].equals("domain", ignoreCase = true) -> {
                            domain = keyValue.getOrNull(1)?.trimStart('.')
                        }
                        keyValue[0].equals("path", ignoreCase = true) -> {
                            path = keyValue.getOrNull(1)
                        }
                        keyValue[0].equals("expires", ignoreCase = true) ||
                            keyValue[0].equals("max-age", ignoreCase = true) ||
                            keyValue[0].equals("secure", ignoreCase = true) ||
                            keyValue[0].equals("httponly", ignoreCase = true) -> {
                            // 不需要处理这些属性
                        }
                        else -> {
                            // 认为是Cookie的名称和值
                            if (name == null) {
                                name = keyValue[0]
                                value = keyValue.getOrNull(1)
                            }
                        }
                    }
                }

                if (name != null && value != null) {
                    val cookieBuilder = Cookie.Builder().name(name!!).value(value!!)
                    if (domain != null) cookieBuilder.domain(domain!!)
                    if (path != null) cookieBuilder.path(path!!)
                    return cookieBuilder.build()
                }
            } catch (e: IllegalArgumentException) {
                // 处理非法域异常
                Log.e("EhCookieStore", "Error parsing Set-Cookie header due to illegal domain: $e")
            }
            return null
        }

        launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    nonCacheOkHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = withContext(Dispatchers.IO) {
                        response.body.string()
                    }
                    withContext(Dispatchers.Main) {
                        webView.loadDataWithBaseURL(url, responseBody, "text/html", "UTF-8", null)
                    }
                } else {
                    Log.e("handlePostRequest", "Request failed: $response")
                }

                // 假设 'response' 是已经执行并获得响应的OkHttp响应对象
                val responseCookies = response.headers("Set-Cookie")
                // Log.d("EhCookieStore", "Set-Cookie headers found: $responseCookies")

                // 使用EhCookieStore来逐个存储Cookie
                responseCookies.forEach { cookieHeader ->
                    // 解析Cookie
                    val parsedCookie = parseSetCookieHeader(cookieHeader)
                    parsedCookie?.let { cookie ->
                        // 使用EhCookieStore的addCookie方法添加Cookie
                        EhCookieStore.addCookie(cookie.name, cookie.value, cookie.domain)
                    }
                }
                // 确保新的Cookie被写入
                EhCookieStore.flush()
            } catch (e: IOException) {
                Log.e("handlePostRequest", "Request error: ${e.localizedMessage}")
            }
        }
    }
    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        onCreated = { webView ->
            EhUtils.signOut()
            webView.setDefaultSettings()
            webView.settings.javaScriptEnabled = true
            webView.addJavascriptInterface(
                WebAppInterface(webView, ::handlePostRequest),
                "Android",
            )
        },
        client = okHttpWebViewClient,
    )
}
