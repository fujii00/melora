package com.music.bitchord

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * The menu contains only Melora-branded artwork. Legacy BitChord waveform
 * aliases are intentionally excluded so they cannot be selected again.
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int, val previewRes: Int? = null) {
    Ember("LauncherDefault", R.mipmap.ic_launcher, R.string.app_icon_ember),
    Velvet("LauncherVelvet", R.mipmap.ic_launcher_velvet, R.string.app_icon_velvet),
    Bone("LauncherBone", R.mipmap.ic_launcher_bone, R.string.app_icon_bone),
    CuteCat("LauncherCuteCat", R.mipmap.ic_launcher_cute_cat, R.string.app_icon_cute_cat),
    LiquidGlassRed("LauncherLiquidGlassRed", R.mipmap.ic_liquid_glass_red, R.string.app_icon_liquid_glass_red),
    MidnightBlue("LauncherMidnightBlue", R.mipmap.ic_midnight_blue, R.string.app_icon_midnight_blue);

    private fun component(context: Context) =
        ComponentName(context.packageName, "com.music.bitchord.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull {
                pm.getComponentEnabledSetting(it.component(context)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: Ember
        }

        /** Enables [icon] before disabling the rest so there is never a moment with no launcher entry. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
