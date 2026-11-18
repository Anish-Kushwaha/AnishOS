package com.anishexperiments.anishos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors

class PhoneMainActivity : Activity() {
    private val black = Color.rgb(0, 0, 0)
    private val panel = Color.rgb(8, 15, 25)
    private val blue = Color.rgb(0, 183, 255)
    private val cyan = Color.rgb(65, 224, 255)
    private val white = Color.rgb(239, 248, 255)
    private val muted = Color.rgb(112, 143, 166)
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var sessionStore: SecureSessionStore
    private lateinit var auth: PhoneAuthRepository
    private lateinit var root: LinearLayout
    private lateinit var stateLabel: TextView
    private lateinit var actionLabel: TextView
    private lateinit var accountLabel: TextView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var authActions: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SecureSessionStore(this)
        auth = PhoneAuthRepository(sessionStore)
        window.statusBarColor = black
        window.navigationBarColor = black
        render()
        refreshState()
        refreshAccount()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
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

        val accountCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = panelDrawable()
        }
        accountCard.addView(text("ANISHOS ACCOUNT", 10f, blue, true))
        addGapTo(accountCard, 9)
        accountLabel = text("Checking account session…", 15f, cyan, true)
        accountCard.addView(accountLabel)
        addGapTo(accountCard, 10)
        emailInput = field("Email address", false)
        passwordInput = field("Password", true)
        accountCard.addView(emailInput)
        addGapTo(accountCard, 8)
        accountCard.addView(passwordInput)
        addGapTo(accountCard, 8)
        authActions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        accountCard.addView(authActions)
        root.addView(accountCard)
        addGap(16)

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
        root.addView(text("Pairing uses the Wear OS Data Layer. Sign in to associate the watch record with your AnishOS account. This does not modify system Bluetooth pairing.", 11f, muted))
        addGap(14)
        root.addView(text("ANISH ENTERPRISES  •  ANISHOS 0.2.0", 9f, muted, true))
    }

    private fun addGapTo(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun field(hint: String, password: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        setHintTextColor(muted)
        setTextColor(white)
        textSize = 14f
        setSingleLine(true)
        inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = GradientDrawable().apply {
            setColor(Color.rgb(3, 8, 14))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.rgb(17, 65, 91))
        }
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

    private fun actionButton(parent: LinearLayout, title: String, action: () -> Unit) {
        val view = TextView(this).apply {
            text = title
            textSize = 10f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(3, 20, 30))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(17, 65, 91))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7) })
    }

    private fun refreshAccount() {
        accountLabel.text = "CHECKING SESSION…"
        executor.execute {
            val session = auth.currentSession()
            runOnUiThread {
                if (session == null) {
                    accountLabel.text = "SIGN IN REQUIRED"
                    accountLabel.setTextColor(cyan)
                    emailInput.visibility = View.VISIBLE
                    passwordInput.visibility = View.VISIBLE
                    authActions.removeAllViews()
                    actionButton(authActions, "SIGN IN") { authenticate(false) }
                    actionButton(authActions, "CREATE ACCOUNT") { authenticate(true) }
                } else {
                    accountLabel.text = "SIGNED IN  /  ${session.email}"
                    accountLabel.setTextColor(Color.rgb(55, 235, 174))
                    emailInput.visibility = View.GONE
                    passwordInput.visibility = View.GONE
                    authActions.removeAllViews()
                    actionButton(authActions, "SIGN OUT") {
                        executor.execute {
                            auth.signOut()
                            runOnUiThread { refreshAccount(); refreshState() }
                        }
                    }
                }
            }
        }
    }

    private fun authenticate(signUp: Boolean) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        if (email.isBlank() || password.length < 8) {
            accountLabel.text = "ENTER A VALID EMAIL AND PASSWORD (8+ CHARACTERS)"
            accountLabel.setTextColor(Color.rgb(255, 125, 125))
            return
        }
        accountLabel.text = if (signUp) "CREATING ACCOUNT…" else "AUTHENTICATING…"
        executor.execute {
            try {
                if (signUp) {
                    val sessionCreated = auth.signUp(email, password)
                    runOnUiThread {
                        accountLabel.text = if (sessionCreated) "ACCOUNT CREATED" else "CHECK YOUR EMAIL TO CONFIRM, THEN SIGN IN"
                        refreshAccount()
                    }
                } else {
                    auth.signIn(email, password)
                    runOnUiThread { refreshAccount(); refreshState() }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    accountLabel.text = error.localizedMessage ?: "AUTHENTICATION FAILED"
                    accountLabel.setTextColor(Color.rgb(255, 125, 125))
                }
            }
        }
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
        val session = sessionStore.load()
        if (session == null) {
            stateLabel.text = "SIGN IN BEFORE PAIRING"
            actionLabel.text = "Use your AnishOS account above, then accept the watch request."
            refreshAccount()
            return
        }
        val prefs = getSharedPreferences(PhoneDataLayerService.PREFS, MODE_PRIVATE)
        val nodeId = prefs.getString(PhoneDataLayerService.KEY_PENDING_NODE, null)
        if (nodeId.isNullOrBlank()) {
            stateLabel.text = "NO PENDING REQUEST"
            actionLabel.text = "Request a connection from AnishOS on your watch first."
            return
        }
        stateLabel.text = "CONFIRMING LOCAL LINK…"
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PhoneDataLayerService.LINK_ACCEPTED_PATH, byteArrayOf())
            .addOnSuccessListener {
                prefs.edit()
                    .putString(PhoneDataLayerService.KEY_LINKED_NODE, nodeId)
                    .putBoolean(PhoneDataLayerService.KEY_LINKED, true)
                    .putBoolean(PhoneDataLayerService.KEY_PENDING, false)
                    .apply()
                Wearable.getNodeClient(this).connectedNodes
                    .addOnSuccessListener { nodes ->
                        val watchName = nodes.firstOrNull { it.id == nodeId }?.displayName ?: "AnishOS Watch"
                        executor.execute {
                            try {
                                auth.registerPendingWatch(session, watchName, "Wear OS")
                                runOnUiThread {
                                    refreshState()
                                    actionLabel.text = "Local link accepted and a pending watch record was added to your AnishOS account."
                                }
                            } catch (error: Exception) {
                                runOnUiThread {
                                    refreshState()
                                    actionLabel.text = "Local link active, but cloud registration failed: ${error.localizedMessage ?: "try again"}"
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        refreshState()
                        actionLabel.text = "Local link active. Cloud registration can be retried after reconnecting."
                    }
            }
            .addOnFailureListener { error ->
                stateLabel.text = "CONNECTION NOT SENT"
                actionLabel.text = error.localizedMessage ?: "Could not reach the watch. Keep both devices nearby and try again."
            }
    }
}
