package com.meuappcontrole

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.view.ViewGroup
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Button

class BlockActivity : Activity() {
    private var remainingMs: Long = 12 * 60 * 60 * 1000 // 12 horas padrão

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // tenta ler tempo exato de SharedPreferences
        val prefs = getSharedPreferences("meuapp_prefs", MODE_PRIVATE)
        val until = prefs.getLong("block_until", 0L)
        if (until > System.currentTimeMillis()) {
            remainingMs = until - System.currentTimeMillis()
        }

        val reason = intent.getStringExtra("blocked_reason") ?: prefs.getString("block_reason", "Limite atingido")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)

        val title = TextView(this)
        title.textSize = 22f
        title.text = "Bloqueado"
        layout.addView(title)

        val reasonView = TextView(this)
        reasonView.text = "Motivo: $reason"
        reasonView.setPadding(0,20,0,20)
        layout.addView(reasonView)

        val timerView = TextView(this)
        timerView.textSize = 20f
        layout.addView(timerView)

        val ok = Button(this)
        ok.text = "Entendi"
        ok.isEnabled = false
        ok.setOnClickListener {
            // só habilita quando bloqueio terminar
        }
        layout.addView(ok)

        setContentView(layout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / (1000*60*60)
                val minutes = (millisUntilFinished / (1000*60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                timerView.text = "Bloqueio finalizado"
                ok.isEnabled = true
                finish()
            }
        }.start()
    }
}
