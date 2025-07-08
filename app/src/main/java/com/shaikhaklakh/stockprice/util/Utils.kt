package com.shaikhaklakh.stockprice.util



import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shaikhaklakh.stockprice.R

object Utils {







    fun showCustomToast(context: Context, message: String) {
        // Safely unwrap the Activity from any Context wrapper
        val activity = context.unwrapActivity() ?: return

        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, activity.findViewById(android.R.id.content), false)

        val toastText = layout.findViewById<TextView>(R.id.toast_message)
        toastText.text = message

        val toast = Toast(context)

        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)

        @Suppress("DEPRECATION")
        toast.view = layout

        toast.show()
    }

    fun Context.unwrapActivity(): Activity? {
        var currentContext = this
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }







    fun setStatusBarColor(activity: Activity, colorResId: Int = R.color.white) {
        val window = activity.window
        val color = ContextCompat.getColor(activity, colorResId)

        // Use backward-compatible approach to set status bar color
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 = API 34
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        } else {
            // Alternative logic for API 34+ (if needed in future Android APIs)
            // As of now, no direct replacement, but the system may use dynamic theming
        }

        // Adjust status bar icon color (light/dark) based on background brightness
        val isLightBackground = isColorLight(color)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLightBackground
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (
                0.299 * ((color shr 16) and 0xFF) +
                        0.587 * ((color shr 8) and 0xFF) +
                        0.114 * (color and 0xFF)
                ) / 255
        return darkness < 0.5
    }



}