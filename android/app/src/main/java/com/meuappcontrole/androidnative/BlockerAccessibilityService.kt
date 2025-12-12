package com.meuappcontrole.androidnative

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.meuappcontrole.BlockActivity
import java.util.concurrent.atomic.AtomicLong

/**
 * BlockerAccessibilityService
 * - Detecta textos que parecem saldo/valores ou hosts (sites) em telas ativas
 * - Faz uma segunda verificação (volta ao HOME e releitura) para confirmar
 * - Aplica regras (deposit, stopWin, stopLoss, dailySeconds) lidas de SharedPreferences
 * - Dispara BlockActivity e grava block_until por 12 horas quando regra for satisfeita
 *
 * Observações:
 * - Configure SharedPreferences com as chaves usadas abaixo a partir do código JS/RN:
 *     rules_deposit (float), rules_stopWin (float), rules_stopLoss (float),
 *     rules_dailySeconds (long), blocked_hosts (StringSet)
 * - Este serviço exige permissão de ACCESSIBILITY configurada pelo usuário nas configurações.
 */

class BlockerAccessibilityService : AccessibilityService() {
    private val TAG = "BlockerAccessibility"
    private val handler = Handler(Looper.getMainLooper())

    // evita spam de detecções
    private val lastDetected = AtomicLong(0)
    private val MIN_DETECT_INTERVAL_MS = 2000L
    private val SECOND_CHECK_DELAY_MS = 1200L

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = null // monitorar todas as apps
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        serviceInfo = info
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val now = System.currentTimeMillis()
            if (now - lastDetected.get() < MIN_DETECT_INTERVAL_MS) return

            val root = rootInActiveWindow ?: return
            // tenta detectar valor ou host
            val pair = findPossibleBalance(root) ?: findPossibleHost(root)
            if (pair != null) {
                val (detectedText, boundsDesc) = pair
                if (looksLikeAd(detectedText)) {
                    Log.i(TAG, "Ignored ad-like text: $detectedText")
                    return
                }
                lastDetected.set(now)
                Log.i(TAG, "Candidate detected: $detectedText | $boundsDesc")
                confirmAndMaybeBlock(detectedText)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {}

    // *******************************
    // Detecção de valores / hosts
    // *******************************

    private fun findPossibleBalance(node: AccessibilityNodeInfo?): Pair<String, String>? {
        if (node == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            cur.text?.toString()?.let {
                val s = it.trim()
                if (looksLikeMoney(s)) {
                    val rect = android.graphics.Rect()
                    try { cur.getBoundsInScreen(rect) } catch (_: Throwable) { rect.set(0,0,0,0) }
                    val desc = "bounds:${rect.left},${rect.top},${rect.right},${rect.bottom}"
                    return Pair(s, desc)
                }
            }
            cur.contentDescription?.toString()?.let {
                val s = it.trim()
                if (looksLikeMoney(s)) {
                    val rect = android.graphics.Rect()
                    try { cur.getBoundsInScreen(rect) } catch (_: Throwable) { rect.set(0,0,0,0) }
                    val desc = "bounds:${rect.left},${rect.top},${rect.right},${rect.bottom}"
                    return Pair(s, desc)
                }
            }
            for (i in 0 until cur.childCount) {
                cur.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findPossibleHost(node: AccessibilityNodeInfo?): Pair<String, String>? {
        if (node == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            cur.text?.toString()?.let {
                val s = it.trim()
                if (looksLikeHost(s)) {
                    val rect = android.graphics.Rect()
                    try { cur.getBoundsInScreen(rect) } catch (_: Throwable) { rect.set(0,0,0,0) }
                    val desc = "bounds:${rect.left},${rect.top},${rect.right},${rect.bottom}"
                    return Pair(s, desc)
                }
            }
            cur.contentDescription?.toString()?.let {
                val s = it.trim()
                if (looksLikeHost(s)) {
                    val rect = android.graphics.Rect()
                    try { cur.getBoundsInScreen(rect) } catch (_: Throwable) { rect.set(0,0,0,0) }
                    val desc = "bounds:${rect.left},${rect.top},${rect.right},${rect.bottom}"
                    return Pair(s, desc)
                }
            }
            for (i in 0 until cur.childCount) {
                cur.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    // *******************************
    // Heurísticas textuais
    // *******************************

    private fun looksLikeMoney(s: String): Boolean {
        val low = s.lowercase()
        if (low.length > 60) return false
        // aceita "R$ 123,45", "123.45", "50", "1.234,56"
        val moneyRegex = Regex("(r\\$\\s*)?\\d{1,3}(?:[.,]\\d{2})?(?:[.,]\\d{3})*")
        return moneyRegex.containsMatchIn(low)
    }

    private fun looksLikeHost(s: String): Boolean {
        val low = s.lowercase()
        if (low.length > 120) return false
        if (low.contains(".com") || low.contains(".br") || low.contains(".bet")) return true
        // domínio genérico
        return Regex("[a-z0-9.-]+\\.[a-z]{2,}").containsMatchIn(low)
    }

    private fun looksLikeAd(s: String): Boolean {
        val low = s.lowercase()
        val keys = listOf(
            "ganhou","ganhador","ganhadores","winner","jackpot","vencedor","vencedores",
            "últimos","ultimos","premio","prêmio","multiplicador","promo","oferta","ganhos"
        )
        for (k in keys) if (low.contains(k)) return true
        return false
    }

    // *******************************
    // Confirmação + Rechecagem + Aplicação de regras
    // *******************************

    private fun confirmAndMaybeBlock(detectedText: String) {
        try {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
        } catch (t: Throwable) {
            Log.w(TAG, "unable to go home: ${t.message}")
        }

        handler.postDelayed({
            try {
                val root2 = rootInActiveWindow
                val againPair = if (root2 != null) {
                    findPossibleBalance(root2) ?: findPossibleHost(root2)
                } else null
                val again = againPair?.first
                if (again != null) {
                    Log.i(TAG, "Second verification found: $again -> checking rules")
                    if (shouldBlockAccordingToRules(again)) {
                        triggerBlockFlow(again)
                    } else {
                        Log.i(TAG, "Rules not matched -> ignoring")
                    }
                } else {
                    Log.i(TAG, "Second verification negative -> ignoring")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "confirmAndMaybeBlock error", t)
            }
        }, SECOND_CHECK_DELAY_MS)
    }

    private fun shouldBlockAccordingToRules(detectedText: String): Boolean {
        try {
            val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
            // check hosts first
            val hosts = prefs.getStringSet("blocked_hosts", null)
            if (hosts != null) {
                for (h in hosts) if (detectedText.lowercase().contains(h.lowercase())) return true
            } else {
                // defaults
                val defaults = listOf("bet","blaze","bet365","pixbet","betano","sportingbet")
                for (d in defaults) if (detectedText.lowercase().contains(d)) return true
            }

            // try parse number from detectedText (balance)
            val parsed = parseNumberFromText(detectedText) ?: return false

            // read rules saved (if present)
            val deposit = prefs.getFloat("rules_deposit", -1f)
            val stopWin = prefs.getFloat("rules_stopWin", -1f)
            val stopLoss = prefs.getFloat("rules_stopLoss", -1f)
            val dailySeconds = prefs.getLong("rules_dailySeconds", -1L)

            if (deposit >= 0f) {
                val delta = parsed - deposit
                if (stopWin >= 0f && delta >= stopWin) return true
                if (stopLoss >= 0f && -delta >= stopLoss) return true
            } else {
                if (stopWin >= 0f && parsed >= stopWin) return true
                if (stopLoss >= 0f && parsed <= stopLoss) return true
            }

            // optionally: dailySeconds logic can be implemented elsewhere (RN side)
        } catch (t: Throwable) {
            Log.e(TAG, "shouldBlockAccordingToRules error", t)
        }
        return false
    }

    private fun parseNumberFromText(s: String): Float? {
        try {
            var low = s.lowercase()
            low = low.replace("r$", "").replace("\\s".toRegex(), "")
            val m = Regex("(\\d+[.,]?\\d{0,2})").find(low) ?: return null
            val v = m.groupValues[1].replace(',', '.')
            return v.toFloatOrNull()
        } catch (_: Throwable) {
            return null
        }
    }

    // grava bloqueio e abre tela de bloqueio
    private fun triggerBlockFlow(reason: String) {
        try {
            val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
            val editor = prefs.edit()
            val until = System.currentTimeMillis() + 12 * 60 * 60 * 1000L // 12h
            editor.putLong("block_until", until)
            editor.putString("block_reason", reason)
            editor.apply()

            val intent = Intent(this, BlockActivity::class.java).apply {
                putExtra("blocked_reason", reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)

            handler.postDelayed({
                val home = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(home)
            }, 300)
        } catch (t: Throwable) {
            Log.e(TAG, "triggerBlockFlow error", t)
        }
    }
}
