package com.meuappcontrole.androidnative

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.meuappcontrole.BlockActivity

class BlockerAccessibilityService : AccessibilityService() {
    private val TAG = "BlockerAccessibility"
    private val handler = Handler(Looper.getMainLooper())

    // Lista de domínios a bloquear (pode ser atualizada via SharedPreferences/NativeModule)
    private val blockedHostsKey = "blocked_hosts"

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.packageNames = null // monitorar todos
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val root = rootInActiveWindow ?: return
            // tenta extrair texto/URL da toolbar ou WebView
            val url = findUrlInNode(root)
            if (url != null) {
                val host = extractHost(url)
                if (host != null && isHostBlocked(host)) {
                    // ação de bloqueio: abrir Activity do app e (quando possível) fechar/voltar app atacado
                    launchBlockActivity(host)
                }
            } else {
                // tenta pegar pacote da app atual por event
                val pkg = event.packageName?.toString()
                if (pkg != null && isPackageBlocked(pkg)) {
                    launchBlockActivity(pkg)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() { /* sem-op */ }

    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        // Procura nodes que costumam conter a URL (resource-id / contentDescription / text)
        val candidates = ArrayList<AccessibilityNodeInfo>()
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (!queue.isEmpty()) {
            val n = queue.removeFirst()
            if (n.text != null && n.text.length > 3) {
                val t = n.text.toString()
                if (t.startsWith("http") || t.contains(".com") || t.contains(".br")) {
                    return t
                }
            }
            // verifica contentDescription
            val desc = n.contentDescription
            if (desc != null) {
                val d = desc.toString()
                if (d.startsWith("http") || d.contains(".com") || d.contains(".br")) return d
            }
            for (i in 0 until n.childCount) {
                val c = n.getChild(i)
                if (c != null) queue.add(c)
            }
        }
        return null
    }

    private fun extractHost(url: String): String? {
        try {
            var u = url
            if (!u.startsWith("http")) u = "http://$u"
            val uri = android.net.Uri.parse(u)
            return uri.host?.replaceFirst("^www\\.".toRegex(), "")
        } catch (t: Throwable) {
            return null
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        // Lê lista de bloqueio do SharedPreferences (padrão: alguns domínios)
        val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
        val list = prefs.getStringSet("blocked_hosts", null)
        if (list != null) return list.any { host.contains(it) || it.contains(host) }
        // default: alguns exemplos
        val defaults = listOf("bet365", "blaze", "betano", "pixbet", "sportingbet")
        return defaults.any { host.contains(it) }
    }

    private fun isPackageBlocked(pkg: String): Boolean {
        // opcional: bloquear apps conhecidos (ex.: browsers?) — retornar false por padrão
        return false
    }

    private fun launchBlockActivity(reason: String) {
        try {
            val intent = Intent(this, BlockActivity::class.java).apply {
                putExtra("blocked_reason", reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            // opcional: simular voltar para home do usuário (fecha app atual)
            handler.postDelayed({
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }, 200)
        } catch (t: Throwable) {
            Log.e(TAG, "launchBlockActivity error", t)
        }
    }
}
