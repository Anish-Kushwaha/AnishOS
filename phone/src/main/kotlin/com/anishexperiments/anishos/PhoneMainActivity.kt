package com.anishexperiments.anishos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.wearable.Wearable

class PhoneMainActivity : Activity() {
    private val black = Color.rgb(0, 0, 0)
    private val panel = Color.rgb(8, 15, 25)
    private val blue = Color.rgb(0, 183, 255)
    private val cyan = Color.rgb(65, 224, 255)
    private val white = Color.rgb(239, 248, 255)
    private val muted = Color.rgb(112, 143, 166)
    private lateinit var root: LinearLayout
    private lateinit var stateLabel: TextView
    private lateinit var actionLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        render()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun panelDrawable(): GradientDrawable = GradientDrawable().apply {
        setColor(panel)
        cornerRadius = dp(18).toFloat()
        setStroke(dp(1), Color.rgb(17, 65, 91))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif-black", Typeface.BOLD) else Typeface.DEFAULT
            includeFontPadding = false
        }

    private fun addGap(height: Int) {
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun render() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(black)
            setPadding(dp(22), dp(28), dp(22), dp(24))
            isFillViewport = true
        }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("A/  ANISHOS", 15f, blue, true))
        addGap(8)
        root.addView(text("COMPANION", 28f, white, true))
        root.addView(text("PHONE ↔ WATCH  /  DEVICE LINK", 11f, muted, true))
        addGap(20)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = panelDrawable()
        }
        card.addView(text("CONNECTION STATUS", 10f, muted, true))
        addGapTo(card, 10)
        stateLabel = text("Checking nearby AnishOS devices…", 17f, cyan, true)
        card.addView(stateLabel)
        addGapTo(card, 8)
        actionLabel = text("Open AnishOS on your Galaxy Watch and request a link.", 12f, muted)
        card.addView(actionLabel)
        root.addView(card)
        addGap(18)

        root.addView(text("PAIRING REQUEST", 10f, blue, true))
        addGap(8)
        button("ACCEPT WATCH CONNECTION") { acceptRequest() }
        button("REFRESH STATUS") { refreshState() }
        button("REVOKE PHONE ↔ WATCH LINK") { revokeLink() }
        addGap(16)
        root.addView(text("AnishOS uses the Wear OS Data Layer to exchange app messages between a paired Android phone and watch. This does not change the phone's system Bluetooth pairing.", 11f, muted))
        addGap(14)
        root.addView(text("ANISH ENTERPRISES  •  ANISHOS 0.2.0", 9f, muted, true))
    }

    private fun addGapTo(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun button(title: String, action: () -> Unit) {
        val view = TextView(this).apply {
            text = title
            textSize = 11f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(if (title.startsWith("ACCEPT")) black else cyan)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(15), dp(14), dp(15))
            background = GradientDrawable().apply {
                setColor(if (title.startsWith("ACCEPT")) blue else panel)
                cornerRadius = dp(28).toFloat()
                if (!title.startsWith("ACCEPT")) setStroke(dp(1), Color.rgb(17, 65, 91))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }

    private fun refreshState() {
        val prefs = getSharedPreferences(PhoneDataLayerService.PREFS, MODE_PRIVATE)
        val pendingAt = prefs.getLong(PhoneDataLayerService.KEY_REQUESTED_AT, 0L)
        val isExpired = pendingAt > 0L && System.currentTimeMillis() - pendingAt > 120_000L
        if (isExpired) prefs.edit().putBoolean(PhoneDataLayerService.KEY_PENDING, false).remove(PhoneDataLayerService.KEY_PENDING_NODE).apply()
        val pending = prefs.getBoolean(PhoneDataLayerService.KEY_PENDING, false)
        val linked = prefs.getBoolean(PhoneDataLayerService.KEY_LINKED, false)
        when {
            pending -> {
                stateLabel.text = "WATCH REQUEST DETECTED"
                actionLabel.text = "A watch is requesting an AnishOS app link. Confirm only if this is your watch."
            }
            linked -> {
                stateLabel.text = "APP LINK ACTIVE"
                actionLabel.text = "The watch and phone have exchanged the AnishOS link confirmation."
            }
            else -> {
                stateLabel.text = "WAITING FOR WATCH"
                actionLabel.text = "Open AnishOS on your Galaxy Watch and request a link."
            }
        }
        actionLabel.setTextColor(if (pending) cyan else muted)
    }

    private fun revokeLink() {
        val prefs = getSharedPreferences(PhoneDataLayerService.PREFS, MODE_PRIVATE)
        val nodeId = prefs.getString(PhoneDataLayerService.KEY_LINKED_NODE, null)
        if (nodeId.isNullOrBlank()) {
            prefs.edit().putBoolean(PhoneDataLayerService.KEY_LINKED, false).apply()
            refreshState()
            return
        }
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PhoneDataLayerService.LINK_REVOKED_PATH, byteArrayOf())
            .addOnCompleteListener {
                prefs.edit()
                    .remove(PhoneDataLayerService.KEY_LINKED_NODE)
                    .putBoolean(PhoneDataLayerService.KEY_LINKED, false)
                    .putBoolean(PhoneDataLayerService.KEY_PENDING, false)
                    .remove(PhoneDataLayerService.KEY_PENDING_NODE)
                    .apply()
                refreshState()
            }
    }

    private fun acceptRequest() {
        val prefs = getSharedPreferences(PhoneDataLayerService.PREFS, MODE_PRIVATE)
        val nodeId = prefs.getString(PhoneDataLayerService.KEY_PENDING_NODE, null)
        if (nodeId.isNullOrBlank()) {
            stateLabel.text = "NO PENDING REQUEST"
            actionLabel.text = "Request a connection from AnishOS on your watch first."
            return
        }
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PhoneDataLayerService.LINK_ACCEPTED_PATH, byteArrayOf())
            .addOnSuccessListener {
                prefs.edit()
                    .putString(PhoneDataLayerService.KEY_LINKED_NODE, nodeId)
                    .putBoolean(PhoneDataLayerService.KEY_LINKED, true)
                    .putBoolean(PhoneDataLayerService.KEY_PENDING, false)
                    .apply()
                refreshState()
            }
            .addOnFailureListener { error ->
                stateLabel.text = "CONNECTION NOT SENT"
                actionLabel.text = error.localizedMessage ?: "Could not reach the watch. Keep both devices nearby and try again."
            }
    }
}
