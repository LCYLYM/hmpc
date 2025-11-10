package com.example.pcmode

import android.hardware.display.DisplayManager

/**
 * 虚拟显示器配置
 */
data class DisplayProfile(
    val id: String,
    val labelRes: Int,
    val width: Int,
    val height: Int,
    val dpi: Int,
    val flags: Int
) {
    companion object {
        fun defaultProfiles(): List<DisplayProfile> = listOf(
            DisplayProfile(
                id = "typec_1080p",
                labelRes = R.string.profile_typec,
                width = 1920,
                height = 1080,
                dpi = 160,
                flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            ),
            DisplayProfile(
                id = "wireless_720p",
                labelRes = R.string.profile_wireless,
                width = 1280,
                height = 720,
                dpi = 140,
                flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
            ),
            DisplayProfile(
                id = "desktop_4k",
                labelRes = R.string.profile_large_monitor,
                width = 3840,
                height = 2160,
                dpi = 320,
                flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            ),
            DisplayProfile(
                id = "portable_1280",
                labelRes = R.string.profile_small_panel,
                width = 1280,
                height = 800,
                dpi = 160,
                flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
        )
    }
}
