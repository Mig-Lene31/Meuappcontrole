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

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.packageNames = null
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags =
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            val root = rootInActiveWindow ?: return

            val url = findUrlInNode(root)
            if (url != null) {
                val host = extractHost(url)
                if (host != null && isHostBlocked(host)) {
                    launchBlockActivity(host)
                }
            } else {
                val pkg = event.packageName?.toString()
                if (pkg != null && isPackageBlocked(pkg)) {
                    launchBlockActivity(pkg)
                }
            }

        } catch (t: Throwable) {
            Log.e(TAG, "onAccessibilityEvent error", t)
        }
    }

    override fun onInterrupt() {}

    private fun findUrlInNode(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val text = current.text?.toString()
            if (text != null && looksLikeUrl(text)) return text

            val desc = current.contentDescription?.toString()
            if (desc != null && looksLikeUrl(desc)) return desc

            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }

        return null
    }

    private fun looksLikeUrl(text: String): Boolean {
        return text.startsWith("http") ||
                text.contains(".com") ||
                text.contains(".br") ||
                text.contains(".bet")
    }

    private fun extractHost(url: String): String? {
        return try {
            val fixed = if (!url.startsWith("http")) "http://$url" else url
            android.net.Uri.parse(fixed).host?.replaceFirst("^www\\.".toRegex(), "")
        } catch (e: Exception) {
            null
        }
    }

    private fun isHostBlocked(host: String): Boolean {
        val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
        val list = prefs.getStringSet("blocked_hosts", null)

        if (list != null)
            return list.any { host.contains(it) }

        val defaults = listOf("bet", "blaze", "bet365", "pixbet", "betano", "sportingbet")
        return defaults.any { host.contains(it) }
    }

    private fun isPackageBlocked(pkg: String): Boolean {
        return false
    }

    private fun launchBlockActivity(reason: String) {
        try {
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
            }, 200)

        } catch (t: Throwable) {
            Log.e(TAG, "launchBlockActivity error", t)
        }
    }
}
