package com.anishexperiments.anishos

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

/**
 * AnishOS Wear — AMOLED / neon interface.
 * Built with platform views so the app stays light on Wear OS hardware.
 */
class MainActivity : Activity() {
    private val black = Color.rgb(0, 0, 0)
    private val surface = Color.rgb(7, 12, 20)
    private val surfaceRaised = Color.rgb(10, 19, 31)
    private val blue = Color.rgb(0, 183, 255)
    private val cyan = Color.rgb(65, 224, 255)
    private val white = Color.rgb(239, 248, 255)
    private val muted = Color.rgb(112, 143, 166)
    private val border = Color.rgb(17, 65, 91)
    private val green = Color.rgb(55, 235, 174)

    private val bluetoothPermissionRequest = 41
    private var timer: CountDownTimer? = null
    private var remaining = 25 * 60 * 1000L
    private var running = false

    private lateinit var root: LinearLayout
    private lateinit var timerLabel: TextView
    private lateinit var focusActionButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        window.decorView.setBackgroundColor(black)
        showHome()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == bluetoothPermissionRequest) showPhone()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, stroke: Int = Color.TRANSPARENT, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun label(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false,
        mono: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = when {
            mono && bold -> Typeface.create("monospace", Typeface.BOLD)
            mono -> Typeface.MONOSPACE
            bold -> Typeface.create("sans-serif-black", Typeface.BOLD)
            else -> Typeface.create("sans-serif", Typeface.NORMAL)
        }
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
        letterSpacing = if (bold) 0.035f else 0.015f
    }

    private fun centered(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        label(value, size, color, bold).apply { gravity = Gravity.CENTER }

    private fun vertical(spacing: Int = 0): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            if (spacing > 0) showDividers = LinearLayout.SHOW_DIVIDER_NONE
        }

    private fun addGap(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun page(kicker: String, title: String, subtitle: String? = null) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(black)
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(12), dp(12), dp(12), dp(16))
        }
        root = vertical()
        scroll.addView(root, ScrollView.LayoutParams(-1, -2))
        setContentView(scroll)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(label("A/", 18f, blue, true, true))
        val brand = vertical()
        brand.addView(label("ANISHOS", 11f, white, true))
        brand.addView(label(kicker.uppercase(Locale.ROOT), 8f, muted, true))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val live = label("● LIVE", 8f, cyan, true, true).apply {
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = rounded(Color.rgb(3, 20, 30), border, 20)
        }
        top.addView(live)
        root.addView(top)
        addGap(root, 16)

        root.addView(label(title, 24f, white, true))
        if (!subtitle.isNullOrBlank()) {
            addGap(root, 4)
            root.addView(label(subtitle, 10f, muted))
        }
        addGap(root, 14)
        val line = View(this).apply { setBackgroundColor(border) }
        root.addView(line, LinearLayout.LayoutParams(-1, dp(1)))
        addGap(root, 12)
    }

    private fun actionCard(
        index: String,
        symbol: String,
        title: String,
        description: String,
        status: String = "OPEN",
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), dp(12), dp(10), dp(12))
            background = rounded(surface, border, 16)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            contentDescription = "$title. $description"
        }
        val icon = centered(symbol, 18f, blue, true).apply {
            background = rounded(Color.rgb(3, 22, 35), Color.rgb(13, 77, 108), 12)
        }
        card.addView(icon, LinearLayout.LayoutParams(dp(38), dp(38)))
        val copy = vertical()
        copy.setPadding(dp(10), 0, dp(4), 0)
        copy.addView(label("$index  /  $title.uppercase()", 10f, white, true))
        addGap(copy, 4)
        copy.addView(label(description, 8.5f, muted))
        card.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(centered("›", 23f, cyan, true))
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        })
    }

    private fun primaryButton(title: String, action: () -> Unit): TextView {
        val button = centered(title.uppercase(Locale.ROOT), 10f, black, true).apply {
            background = rounded(blue, blue, 24)
            isClickable = true
            isFocusable = true
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setOnClickListener { action() }
        }
        root.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(7)
            bottomMargin = dp(7)
        })
        return button
    }

    private fun secondaryButton(title: String, action: () -> Unit) {
        val button = centered(title.uppercase(Locale.ROOT), 9f, cyan, true).apply {
            background = rounded(surfaceRaised, border, 24)
            isClickable = true
            isFocusable = true
            setPadding(dp(14), dp(11), dp(14), dp(11))
            setOnClickListener { action() }
        }
        root.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(5)
            bottomMargin = dp(5)
        })
    }

    private fun sectionTag(value: String) {
        root.addView(label(value.uppercase(Locale.ROOT), 8f, blue, true, true))
        addGap(root, 8)
    }

    private fun infoPanel(title: String, body: String) {
        val panel = vertical()
        panel.setPadding(dp(13), dp(12), dp(13), dp(12))
        panel.background = rounded(surface, border, 14)
        panel.addView(label(title, 11f, white, true))
        addGap(panel, 7)
        panel.addView(label(body, 9f, muted))
        root.addView(panel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(9)
        })
    }

    private fun showHome() {
        page("PERSONAL WATCH SYSTEM", "CONTROL\nYOUR WORLD.", "GALAXY WATCH EDITION  /  BUILD 0.2")
        val hero = vertical()
        hero.setPadding(dp(13), dp(14), dp(13), dp(13))
        hero.background = rounded(surfaceRaised, Color.rgb(0, 102, 150), 18)
        hero.addView(label("SYSTEM STATUS", 8f, muted, true, true))
        addGap(hero, 5)
        hero.addView(label("ALL SYSTEMS NOMINAL", 13f, cyan, true))
        addGap(hero, 4)
        hero.addView(label("AMOLED CORE  •  NEON INTERFACE", 8f, muted, false, true))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(13)
        })

        sectionTag("Modules  /  05")
        actionCard("01", "◉", "FOCUS MODE", "25 MIN  /  LOCK IN") { showFocus() }
        actionCard("02", "⌁", "HEALTH SYSTEM", "SENSOR SYNC  /  OPT-IN") { showHealth() }
        actionCard("03", "⌘", "PHONE LINK", "BLUETOOTH  /  CONNECTIVITY") { showPhone() }
        actionCard("04", "✳", "ANISH AI", "INTELLIGENCE  /  COMING SOON") { showAi() }
        actionCard("05", "⚙", "SYSTEM CONFIG", "DISPLAY  /  PRIVACY  /  INFO") { showSettings() }

        addGap(root, 5)
        centered("ANISH ENTERPRISES  •  EST. 2026", 7.5f, muted, true).also { root.addView(it) }
        addGap(root, 4)
        centered("DESIGNED FOR THE EDGE.", 7f, blue, true,).also { root.addView(it) }
    }

    private fun showFocus() {
        page("PERFORMANCE MODULE", "FOCUS MODE", "DEEP WORK  /  25 MIN SESSION")
        sectionTag("Session timer")
        val timerPanel = vertical()
        timerPanel.gravity = Gravity.CENTER
        timerPanel.setPadding(dp(10), dp(22), dp(10), dp(22))
        timerPanel.background = rounded(surfaceRaised, Color.rgb(0, 111, 163), 20)
        timerPanel.addView(centered("STAY IN THE ZONE", 8f, muted, true))
        addGap(timerPanel, 12)
        timerLabel = centered(formatTime(remaining), 38f, cyan, true).apply {
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }
        timerPanel.addView(timerLabel)
        addGap(timerPanel, 12)
        timerPanel.addView(centered(if (running) "●  SESSION ACTIVE" else "○  READY WHEN YOU ARE", 8f, blue, true))
        root.addView(timerPanel, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })

        focusActionButton = primaryButton(if (running) "PAUSE SESSION" else "START FOCUS") {
            if (running) pauseTimer() else startTimer()
        }
        secondaryButton("RESET TIMER") {
            timer?.cancel()
            running = false
            remaining = 25 * 60 * 1000L
            showFocus()
        }
        secondaryButton("BACK TO SYSTEM") { showHome() }
    }

    private fun startTimer() {
        if (running || remaining <= 0L) return
        running = true
        focusActionButton.text = "PAUSE SESSION"
        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remaining = millisUntilFinished
                timerLabel.text = formatTime(remaining)
            }

            override fun onFinish() {
                running = false
                remaining = 0
                timerLabel.text = "00:00"
                focusActionButton.text = "SESSION COMPLETE"
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
        return "%02d:%02d".format(Locale.ROOT, seconds / 60, seconds % 60)
    }

    private fun showHealth() {
        page("BIO / ACTIVITY", "HEALTH SYSTEM", "YOUR DATA. YOUR CONTROL.")
        infoPanel("●  PRIVACY STATUS", "Health data collection is currently OFF.")
        infoPanel("SENSOR ACCESS", "Heart rate, activity and Health Services are not connected in this starter build.")
        infoPanel("CLOUD SYNC", "No health metrics are being uploaded. Sensor access will require explicit permission and opt-in.")
        secondaryButton("BACK TO SYSTEM") { showHome() }
    }

    private fun showPhone() {
        page("WIRELESS CONTROL", "PHONE LINK", "BLUETOOTH  /  DEVICE STATUS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            infoPanel("PERMISSION REQUIRED", "Allow Bluetooth access to read the watch Bluetooth state.")
            primaryButton("ALLOW BLUETOOTH") {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), bluetoothPermissionRequest)
            }
        } else {
            val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val enabled = try {
                manager.adapter?.isEnabled == true
            } catch (_: SecurityException) {
                false
            }
            infoPanel(
                if (enabled) "●  RADIO ONLINE" else "○  RADIO OFFLINE",
                if (enabled) "Bluetooth is enabled on this watch." else "Bluetooth is currently disabled."
            )
            infoPanel("COMPANION LINK", "Phone pairing and notification relay are not implemented in this build yet.")
            primaryButton("OPEN BLUETOOTH SETTINGS") {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
        secondaryButton("BACK TO SYSTEM") { showHome() }
    }

    private fun showAi() {
        page("INTELLIGENCE CORE", "ANISH AI", "LOCAL INTERFACE  /  CLOUD LINK PENDING")
        infoPanel("AI CORE  /  STANDBY", "The assistant interface is reserved for the AnishOS intelligence layer.")
        infoPanel("CONNECTION STATUS", "No AI provider, API key, microphone permission or inference endpoint is connected.")
        infoPanel("NEXT SYSTEM UPDATE", "Voice interaction and secure cloud inference will be added as separate opt-in capabilities.")
        secondaryButton("BACK TO SYSTEM") { showHome() }
    }

    private fun showSettings() {
        page("SYSTEM CONTROL", "CONFIGURATION", "ANISHOS  /  VERSION 0.2")
        infoPanel("DISPLAY ENGINE", "Pure AMOLED black  /  electric blue accents  /  high-contrast typography.")
        infoPanel("DEVICE", "Standalone Wear OS application designed for Samsung Galaxy Watch4.")
        infoPanel("PRIVACY", "No health metrics are collected and no AI service is connected in this build.")
        infoPanel("SYSTEM NOTE", "AnishOS is an app experience. It does not replace Samsung's underlying operating system or system launcher.")
        secondaryButton("BACK TO SYSTEM") { showHome() }
    }
}
