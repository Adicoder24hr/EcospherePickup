package com.ecosphere.wastepickup.core.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ecosphere.wastepickup.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.lang.ref.WeakReference

class SweetAlertDialogManager {
    private enum class DialogKind { LOADING, MESSAGE, CONFIRMATION }
    private enum class AlertType(val symbol: String, val color: Int, val background: Int) {
        SUCCESS("✓", R.color.status_green, R.color.status_green_bg),
        ERROR("×", R.color.status_red, R.color.status_red_bg),
        WARNING("!", R.color.status_amber, R.color.status_amber_bg),
        INFO("i", R.color.status_blue, R.color.status_blue_bg)
    }

    private var contextReference: WeakReference<Context>? = null
    private var ownerReference: WeakReference<LifecycleOwner>? = null
    private var lifecycleReference: WeakReference<Lifecycle>? = null
    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private var activeDialog: Dialog? = null
    private var activeKind: DialogKind? = null

    fun bind(context: Context, owner: LifecycleOwner) {
        if (ownerReference?.get() === owner) return
        detachObserver()
        dismiss()
        contextReference = WeakReference(context)
        ownerReference = WeakReference(owner)
        lifecycleReference = WeakReference(owner.lifecycle)
        lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = dismiss()

            override fun onDestroy(owner: LifecycleOwner) {
                dismiss()
                owner.lifecycle.removeObserver(this)
                if (ownerReference?.get() === owner) clearReferences()
            }
        }.also(owner.lifecycle::addObserver)
    }

    fun showLoading(title: String = "Please wait", message: String = "") {
        if (activeKind == DialogKind.LOADING && activeDialog?.isShowing == true) return
        val context = usableContext() ?: return
        val dialog = Dialog(context)
        dialog.setContentView(createLoadingContent(context, title, message))
        show(dialog, DialogKind.LOADING, cancelable = false)
    }

    fun showSuccess(
        title: String,
        message: String,
        buttonText: String = "OK",
        onConfirm: (() -> Unit)? = null
    ) = showAlert(AlertType.SUCCESS, title, message, buttonText, onConfirm = onConfirm)

    fun showError(title: String, message: String, buttonText: String = "OK") =
        showAlert(AlertType.ERROR, title, message, buttonText)

    fun showWarning(title: String, message: String, buttonText: String = "OK") =
        showAlert(AlertType.WARNING, title, message, buttonText)

    fun showInfo(title: String, message: String, buttonText: String = "OK") =
        showAlert(AlertType.INFO, title, message, buttonText)

    fun showConfirmation(
        title: String,
        message: String,
        cancelText: String = "Cancel",
        confirmText: String = "Continue",
        onConfirm: () -> Unit
    ) = showAlert(
        type = AlertType.WARNING,
        title = title,
        message = message,
        buttonText = confirmText,
        cancelText = cancelText,
        onConfirm = onConfirm
    )

    fun dismissLoading() {
        if (activeKind == DialogKind.LOADING) dismiss()
    }

    fun dismiss() {
        activeDialog?.setOnDismissListener(null)
        activeDialog?.dismiss()
        activeDialog = null
        activeKind = null
    }

    private fun showAlert(
        type: AlertType,
        title: String,
        message: String,
        buttonText: String,
        cancelText: String? = null,
        onConfirm: (() -> Unit)? = null
    ) {
        val context = usableContext() ?: return
        val dialog = Dialog(context)
        val content = createAlertContent(
            context = context,
            type = type,
            title = title,
            message = message,
            buttonText = buttonText,
            cancelText = cancelText,
            onCancel = { dialog.dismiss() },
            onConfirm = {
                dialog.dismiss()
                onConfirm?.invoke()
            }
        )
        dialog.setContentView(content.first)
        dialog.setCancelable(cancelText == null)
        show(dialog, if (cancelText == null) DialogKind.MESSAGE else DialogKind.CONFIRMATION, cancelText == null)
        content.second.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180L)
            .start()
    }

    private fun usableContext(): Context? {
        val owner = ownerReference?.get() ?: return null
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return null
        return contextReference?.get()
    }

    private fun createLoadingContent(context: Context, title: String, message: String): FrameLayout {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = createCard(context, density)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        val progress = ProgressBar(context).apply {
            indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.eco_green))
        }
        content.addView(progress, LinearLayout.LayoutParams(dp(44), dp(44)))
        content.addView(createTitle(context, title), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(18) })
        if (message.isNotBlank()) {
            content.addView(createMessage(context, message), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) })
        }
        card.addView(content)
        return createCenteredContent(context, density, card)
    }

    private fun createAlertContent(
        context: Context,
        type: AlertType,
        title: String,
        message: String,
        buttonText: String,
        cancelText: String?,
        onCancel: () -> Unit,
        onConfirm: () -> Unit
    ): Pair<FrameLayout, View> {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val card = createCard(context, density)
        card.alpha = 0f
        card.scaleX = 0.94f
        card.scaleY = 0.94f
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        content.addView(TextView(context).apply {
            text = type.symbol
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.getColor(type.color))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(type.background))
            }
        }, LinearLayout.LayoutParams(dp(58), dp(58)))
        content.addView(createTitle(context, title), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })
        content.addView(createMessage(context, message), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        if (cancelText != null) {
            actions.addView(createActionButton(context, cancelText, secondary = true, onClick = onCancel),
                LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        }
        actions.addView(createActionButton(context, buttonText, secondary = false, onClick = onConfirm),
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = if (cancelText != null) dp(8) else 0 })
        content.addView(actions, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(22) })
        card.addView(content)
        return createCenteredContent(context, density, card) to card
    }

    private fun createCard(context: Context, density: Float) = MaterialCardView(context).apply {
        radius = 20f * density
        cardElevation = 10f * density
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = (density).toInt().coerceAtLeast(1)
        setStrokeColor(context.getColor(R.color.eco_border))
    }

    private fun createCenteredContent(context: Context, density: Float, card: View): FrameLayout {
        fun dp(value: Int) = (value * density).toInt()
        val width = (context.resources.displayMetrics.widthPixels / density - 48f)
            .toInt().coerceAtLeast(180).coerceAtMost(360).let(::dp)
        return FrameLayout(context).apply {
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(card, FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            post {
                val params = card.layoutParams as? FrameLayout.LayoutParams ?: return@post
                params.width = (this.width - paddingLeft - paddingRight).coerceAtMost(dp(360))
                card.layoutParams = params
            }
        }
    }

    private fun createTitle(context: Context, text: String) = TextView(context).apply {
        this.text = text
        textSize = 18f
        setTextColor(context.getColor(R.color.eco_dark_green))
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
    }

    private fun createMessage(context: Context, text: String) = TextView(context).apply {
        this.text = text
        textSize = 14f
        setTextColor(context.getColor(R.color.eco_secondary_text))
        gravity = Gravity.CENTER
    }

    private fun createActionButton(
        context: Context,
        text: String,
        secondary: Boolean,
        onClick: () -> Unit
    ) = MaterialButton(context).apply {
        this.text = text
        isAllCaps = false
        cornerRadius = (12f * context.resources.displayMetrics.density).toInt()
        if (secondary) {
            backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.eco_light_green))
            setTextColor(context.getColor(R.color.eco_dark_green))
        } else {
            backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.eco_green))
            setTextColor(Color.WHITE)
        }
        setOnClickListener { onClick() }
    }

    private fun show(dialog: Dialog, kind: DialogKind, cancelable: Boolean) {
        val context = usableContext() ?: return
        dismiss()
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)
        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
                activeKind = null
            }
        }
        activeDialog = dialog
        activeKind = kind
        dialog.show()
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setGravity(Gravity.CENTER)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.attributes = window.attributes.apply {
                gravity = Gravity.CENTER
                x = 0
                y = 0
                dimAmount = 0.32f
            }
            window.decorView.post {
                if (activeDialog === dialog && dialog.isShowing) {
                    window.setGravity(Gravity.CENTER)
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    window.attributes = window.attributes.apply { gravity = Gravity.CENTER; x = 0; y = 0 }
                }
            }
        }
    }

    private fun detachObserver() {
        val lifecycle = lifecycleReference?.get()
        val observer = lifecycleObserver
        if (lifecycle != null && observer != null) lifecycle.removeObserver(observer)
        lifecycleObserver = null
    }

    private fun clearReferences() {
        contextReference?.clear()
        ownerReference?.clear()
        lifecycleReference?.clear()
        contextReference = null
        ownerReference = null
        lifecycleReference = null
        lifecycleObserver = null
    }
}
