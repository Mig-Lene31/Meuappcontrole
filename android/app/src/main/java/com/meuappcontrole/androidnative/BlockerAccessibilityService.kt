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

class BlockerAccessibilityService : AccessibilityService() {
    private val TAG = "BlockerAccessibility"
    private val handler = Handler(Looper.getMainLooper())

    // última detecção para evitar spam (debounce)
    private val lastDetected = AtomicLong(0)

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = null
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
            val root = rootInActiveWindow ?: return
            val found = findPossibleBalance(root)
            if (found != null) {
                val now = System.currentTimeMillis()
                if (now - lastDetected.get() < 2000) {
                    // debouncing: ignora leituras muito próximas
                    return
                }
                lastDetected.set(now)

                val (text, boundsDesc) = found
                Log.i(TAG, "Candidate text: $text / $boundsDesc")

                // heurística simples: filtra se texto parece anúncio (palavras-chave)
                if (looksLikeAd(text)) {
                    Log.i(TAG, "Ignorado (ad-like): $text")
                    return
                }

                // frequency check / animation heuristic: se a mesma view está mudando muito, ignora
                // (simplified: count child changes not implemented here; rely on debounce)

                // 1ª detecção: volta ao HOME como confirmação (segunda verificação)
                confirmAndBlock(text)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {}

    // procura um texto que pareça um saldo/valor (procura em nodes visíveis)
    private fun findPossibleBalance(node: AccessibilityNodeInfo?): Pair<String,String>? {
        if (node == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            cur.text?.toString()?.let {
                val s = it.trim()
                if (looksLikeMoney(s)) {
                    // boundsInScreen pode ser usado como "posição" heurística
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

    // heurística textual para detectar valores monetários (R$, números com vírgula/ponto)
    private fun looksLikeMoney(s: String): Boolean {
        val low = s.toLowerCase()
        // evita strings muito longas
        if (low.length > 40) return false
        // aceita formatos tipo "R$ 123,45", "123.45", "50"
        val moneyRegex = Regex("(r\\$\\s*)?\\d{1,3}(?:[.,]\\d{2})?(?:\\s?[kKmM])?")
        return moneyRegex.containsMatchIn(low)
    }

    // palavras-chave que indicam anúncio/ganhadores (lista reduzida)
    private fun looksLikeAd(s: String): Boolean {
        val low = s.toLowerCase()
        val keys = listOf("ganhou","ganhador","ganhadores","winner","jackpot","vencedor","vencedores","últimos","ultimos","premio","prêmio","multiplicador")
        for (k in keys) if (low.contains(k)) return true
        return false
    }

    // confirma antes de bloquear: tenta voltar home e reler depois de curto delay
    private fun confirmAndBlock(detectedText: String) {
        // abre Home (fecha app atual)
        try {
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
        } catch (t: Throwable) {
            Log.w(TAG, "unable to go home: ${t.message}")
        }

        // aguarda e tenta reler a janela ativa (segunda verificação)
        handler.postDelayed({
            try {
                val root2 = rootInActiveWindow
                val again = if (root2 != null) {
                    // tenta achar o mesmo valor/host novamente
                    val pair = findPossibleBalance(root2)
                    pair?.first
                } else null

                if (again != null) {
                    // se ainda acha algo plausível com mesmo padrão -> disparar bloqueio
                    Log.i(TAG, "Second verification found: $again -> blocking")
                    triggerBlockFlow(detectedText)
                } else {
                    Log.i(TAG, "Second verification negative, ignoring")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "confirmAndBlock error", t)
            }
        }, 1200) // 1.2s depois
    }

    // ação de bloqueio: abre BlockActivity e grava estado via SharedPreferences (simples)
    private fun triggerBlockFlow(reason: String) {
        try {
            // gravação simples em prefs (persistência mínima)
            val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
            val editor = prefs.edit()
            val until = System.currentTimeMillis() + 12 * 60 * 60 * 1000L
            editor.putLong("block_until", until)
            editor.putString("block_reason", reason)
            editor.apply()

            // iniciar BlockActivity
            val intent = Intent(this, BlockActivity::class.java).apply {
                putExtra("blocked_reason", reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)

            // manda o usuário pra home depois de pequeno delay (para garantir app fechado)
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
