package com.anishexperiments.anishos

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val background = Color.rgb(5, 9, 20)
    private val panel = Color.rgb(13, 24, 42)
    private val blue = Color.rgb(91, 164, 255)
    private val muted = Color.rgb(145, 164, 190)
    private val bluetoothPermissionRequest = 41
    private var timer: CountDownTimer? = null
    private var remaining = 25 * 60 * 1000L
    private var running = false
    private lateinit var root: LinearLayout
    private lateinit var timerLabel: TextView
    private lateinit var focusActionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        showHome()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == bluetoothPermissionRequest) showPhone()
    }

    private fun base(title: String, subtitle: String): LinearLayout {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(background)
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22, 26, 22, 28)
        }
        scroll.addView(root)
        setContentView(scroll)
        root.addView(text(title, 25, Color.WHITE, true))
        root.addView(text(subtitle, 11, muted, false).apply {
            setPadding(0, 5, 0, 18)
        })
        return root
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

    private fun button(label: String, action: () -> Unit): Button {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(24, 55, 91))
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 7
            bottomMargin = 5
        })
        return b
    }

    private fun card(title: String, description: String, action: () -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 12, 15, 12)
            setBackgroundColor(panel)
        }
        box.addView(text(title, 15, Color.WHITE, true))
        box.addView(text(description, 10, muted, false).apply {
            setPadding(0, 5, 0, 0)
        })
        box.setOnClickListener { action() }
        root.addView(box, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 6
            bottomMargin = 6
        })
    }

    private fun showHome() {
        base("AnishOS", "YOUR WATCH. YOUR ECOSYSTEM.")
        root.addView(text("GALAXY WATCH EDITION", 10, blue, true).apply {
            setPadding(0, 0, 0, 14)
        })
        card("Focus mode", "A calm 25-minute focus session") { showFocus() }
        card("Health & activity", "Health sync is opt-in and not active yet") { showHealth() }
        card("Phone connection", "Check Bluetooth and companion setup") { showPhone() }
        card("AnishOS AI", "Assistant integration — coming next") { showAi() }
        card("Settings", "About this build and privacy") { showSettings() }
        root.addView(text("ANISH ENTERPRISES  ·  v0.1.0", 9, muted, false).apply {
            setPadding(0, 18, 0, 0)
        })
    }

    private fun showFocus() {
        base("Focus mode", "ONE TASK. ONE SESSION.")
        timerLabel = text(formatTime(remaining), 38, Color.WHITE, true)
        timerLabel.setPadding(0, 28, 0, 24)
        root.addView(timerLabel)
        focusActionButton = button(if (running) "Pause session" else "Start 25-minute session") {
            if (running) pauseTimer() else startTimer()
        }
        button("Reset session") {
            timer?.cancel()
            running = false
            remaining = 25 * 60 * 1000L
            showFocus()
        }
        button("Back to dashboard") { showHome() }
    }

    private fun startTimer() {
        if (running || remaining <= 0L) return
        running = true
        focusActionButton.text = "Pause session"
        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remaining = millisUntilFinished
                timerLabel.text = formatTime(remaining)
            }
            override fun onFinish() {
                running = false
                remaining = 0
                timerLabel.text = "00:00"
                focusActionButton.text = "Session complete"
            }
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        running = false
        showFocus()
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun showHealth() {
        base("Health & activity", "PRIVACY FIRST")
        root.addView(text("Health data is not being collected.", 14, Color.WHITE, true).apply {
            setPadding(0, 18, 0, 12)
        })
        root.addView(text("Sensor access, Health Services and cloud sync will only be enabled after explicit permissions and an opt-in setup. This starter build does not read or upload health metrics.", 11, muted, false))
        button("Back to dashboard") { showHome() }
    }

    private fun showPhone() {
        base("Phone connection", "BLUETOOTH STATUS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            root.addView(text("Allow Bluetooth access to read the watch's Bluetooth status.", 12, muted, false).apply {
                setPadding(0, 20, 0, 12)
            })
            button("Allow Bluetooth access") {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), bluetoothPermissionRequest)
            }
        } else {
            val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val enabled = try { manager.adapter?.isEnabled == true } catch (_: SecurityException) { false }
            root.addView(text(if (enabled) "Bluetooth is enabled" else "Bluetooth is disabled", 16, if (enabled) Color.rgb(123, 224, 177) else Color.WHITE, true).apply {
                setPadding(0, 24, 0, 12)
            })
            root.addView(text("This build does not yet establish a companion-phone link or relay notifications. The Bluetooth setting below opens the Wear OS system controls.", 11, muted, false))
            button("Open Bluetooth settings") {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
        button("Back to dashboard") { showHome() }
    }

    private fun showAi() {
        base("AnishOS AI", "ASSISTANT MODULE")
        root.addView(text("Your assistant starts here.", 15, Color.WHITE, true).apply {
            setPadding(0, 20, 0, 12)
        })
        root.addView(text("The AI interface is reserved in this build. No AI provider, API key, microphone permission or cloud inference endpoint is connected yet.", 11, muted, false))
        button("Back to dashboard") { showHome() }
    }

    private fun showSettings() {
        base("Settings", "ANISHOS v0.1.0")
        root.addView(text("AMOLED dark interface", 14, Color.WHITE, true).apply {
            setPadding(0, 18, 0, 10)
        })
        root.addView(text("Standalone Wear OS starter for Samsung Galaxy Watch4. This app does not replace or modify Samsung's system operating system or launcher.", 11, muted, false))
        button("Back to dashboard") { showHome() }
    }
}
