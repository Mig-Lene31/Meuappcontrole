package com.meuappcontrole.androidnative

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject

class BlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val fullText = extractText(root).lowercase()

        val json = resources.openRawResource(
            resources.getIdentifier("block_list", "raw", packageName)
        ).bufferedReader().use { it.readText() }

        val list = JSONObject(json).getJSONArray("blocked_domains")

        for (i in 0 until list.length()) {
            val domain = list.getString(i)
            if (fullText.contains(domain.lowercase())) {

                val intent = Intent("BLOCK_EVENT")
                intent.putExtra("domain", domain)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            }
        }
    }

    override fun onInterrupt() {}

    private fun extractText(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        if (node.text != null) builder.append(node.text).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) builder.append(extractText(child))
        }
        return builder.toString()
    }
}
