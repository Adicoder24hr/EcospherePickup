
package com.ecosphere.wastepickup

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val navHost = findViewById<View>(
            R.id.nav_host_fragment
        )

        val initialLeft = navHost.paddingLeft
        val initialTop = navHost.paddingTop
        val initialRight = navHost.paddingRight
        val initialBottom = navHost.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(
            navHost
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            val ime = insets.getInsets(
                WindowInsetsCompat.Type.ime()
            )

            view.updatePadding(
                left = initialLeft + systemBars.left,
                top = initialTop + systemBars.top,
                right = initialRight + systemBars.right,
                bottom = initialBottom +
                        maxOf(systemBars.bottom, ime.bottom)
            )

            insets
        }

        ViewCompat.requestApplyInsets(navHost)
    }
}
