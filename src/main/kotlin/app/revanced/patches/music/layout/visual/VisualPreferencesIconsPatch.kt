package app.revanced.patches.music.layout.visual

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.layout.branding.icon.CustomBrandingIconPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.ResourceUtils.SETTINGS_HEADER_PATH
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow
import org.w3c.dom.Element
import java.io.Closeable

@Suppress("DEPRECATION", "unused")
object VisualPreferencesIconsPatch : BaseResourcePatch(
    name = "Visual preferences icons for YouTube Music",
    description = "Adds icons to specific preferences in the settings.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
), Closeable {
    private const val DEFAULT_ICON = "extension"

    private val RVXSettingsMenuIcon = stringPatchOption(
        key = "RVXSettingsMenuIcon",
        default = DEFAULT_ICON,
        values = mapOf(
            "Custom branding icon" to "custom_branding_icon",
            "Extension" to DEFAULT_ICON,
            "Gear" to "gear",
            "ReVanced" to "revanced",
            "ReVanced Colored" to "revanced_colored",
        ),
        title = "RVX settings menu icon",
        description = "The icon for the RVX settings menu.",
        required = true
    )

    private lateinit var context: ResourceContext

    override fun execute(context: ResourceContext) {
        this.context = context

        // Check patch options first.
        val selectedIconType = RVXSettingsMenuIcon
            .underBarOrThrow()

        val customBrandingIconType = CustomBrandingIconPatch.AppIcon
            .underBarOrThrow()

        // region copy shared resources.

        arrayOf(
            ResourceGroup(
                "drawable",
                *preferenceKey.map { it + "_icon.xml" }.toTypedArray()
            ),
        ).forEach { resourceGroup ->
            context.copyResources("music/visual/shared", resourceGroup)
        }

        // endregion.

        // region copy RVX settings menu icon.

        val iconPath = when (selectedIconType) {
            "custom_branding_icon" -> "music/branding/$customBrandingIconType/settings"
            else -> "music/visual/icons/$selectedIconType"
        }
        val resourceGroup = ResourceGroup(
            "drawable",
            "revanced_extended_settings_icon.xml"
        )

        try {
            context.copyResources(iconPath, resourceGroup)
        } catch (_: Exception) {
            // Ignore if resource copy fails
        }

        // endregion.
    }

    override fun close() {

        // region set visual preferences icon.

        context.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            editor.file.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                node.getAttributeNode("android:key")
                    ?.textContent
                    ?.removePrefix("@string/")
                    ?.let { title ->
                        val drawableName = when (title) {
                            in preferenceKey -> title + "_icon"
                            else -> null
                        }

                        drawableName?.let {
                            node.setAttribute("android:icon", "@drawable/$it")
                        }
                    }
            }
        }

        // endregion.

    }

    // region preference key and icon.

    private val preferenceKey = setOf(
        // YouTube settings.
        "pref_key_parent_tools",
        "settings_header_general",
        "settings_header_playback",
        "settings_header_data_saving",
        "settings_header_downloads_and_storage",
        "settings_header_notifications",
        "settings_header_privacy_and_location",
        "settings_header_recommendations",
        "settings_header_paid_memberships",
        "settings_header_about_youtube_music",

        // RVX settings.
        "revanced_extended_settings",

        "revanced_preference_screen_account",
        "revanced_preference_screen_action_bar",
        "revanced_preference_screen_ads",
        "revanced_preference_screen_flyout",
        "revanced_preference_screen_general",
        "revanced_preference_screen_navigation",
        "revanced_preference_screen_player",
        "revanced_preference_screen_settings",
        "revanced_preference_screen_video",
        "revanced_preference_screen_ryd",
        "revanced_preference_screen_return_youtube_username",
        "revanced_preference_screen_sb",
        "revanced_preference_screen_misc",
    )

    // endregion.

}
