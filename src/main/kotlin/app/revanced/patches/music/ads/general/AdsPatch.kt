package app.revanced.patches.music.ads.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.ads.general.MusicAdsPatch.hookLithoFullscreenAds
import app.revanced.patches.music.ads.general.MusicAdsPatch.hookNonLithoFullscreenAds
import app.revanced.patches.music.ads.general.fingerprints.AccountMenuFooterFingerprint
import app.revanced.patches.music.ads.general.fingerprints.FloatingLayoutFingerprint
import app.revanced.patches.music.ads.general.fingerprints.GetPremiumTextViewFingerprint
import app.revanced.patches.music.ads.general.fingerprints.InterstitialsContainerFingerprint
import app.revanced.patches.music.ads.general.fingerprints.NotifierShelfFingerprint
import app.revanced.patches.music.ads.general.fingerprints.ShowDialogCommandFingerprint
import app.revanced.patches.music.navigation.components.NavigationBarComponentsPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.ADS_PATH
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ButtonContainer
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.FloatingLayout
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PrivacyTosFooter
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
object AdsPatch : BaseBytecodePatch(
    name = "Hide ads",
    description = "Adds options to hide ads.",
    dependencies = setOf(
        LithoFilterPatch::class,
        MusicAdsPatch::class,
        NavigationBarComponentsPatch::class, // for 'Hide upgrade button' setting
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountMenuFooterFingerprint,
        FloatingLayoutFingerprint,
        GetPremiumTextViewFingerprint,
        InterstitialsContainerFingerprint,
        NotifierShelfFingerprint,
        ShowDialogCommandFingerprint
    )
) {
    private const val ADS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/AdsFilter;"

    private const val PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumPromotionPatch;"

    private const val PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumRenewalPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for hide fullscreen ads

        // non-litho view, used in some old clients
        InterstitialsContainerFingerprint
            .resultOrThrow()
            .hookNonLithoFullscreenAds(InterstitialsContainer)

        // litho view, used in 'ShowDialogCommandOuterClass' in innertube
        ShowDialogCommandFingerprint
            .resultOrThrow()
            .hookLithoFullscreenAds()

        // endregion

        // region patch for hide premium promotion popup

        FloatingLayoutFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstWideLiteralInstructionValueOrThrow(FloatingLayout) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR->hidePremiumPromotion(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide premium renewal banner

        NotifierShelfFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val linearLayoutIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(ButtonContainer) + 3
                val linearLayoutRegister =
                    getInstruction<OneRegisterInstruction>(linearLayoutIndex).registerA

                addInstruction(
                    linearLayoutIndex + 1,
                    "invoke-static {v$linearLayoutRegister}, $PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR->hidePremiumRenewal(Landroid/widget/LinearLayout;)V"
                )
            }
        }

        // endregion

        // region patch for hide get premium

        // get premium button at the top of the account switching menu
        GetPremiumTextViewFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        }

        // get premium button at the bottom of the account switching menu
        AccountMenuFooterFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(PrivacyTosFooter)
                val walkerIndex =
                    indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL)
                val viewIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.IGET_OBJECT)
                val viewReference =
                    getInstruction<ReferenceInstruction>(viewIndex).reference.toString()

                val walkerMethod = getWalkerMethod(context, walkerIndex)
                walkerMethod.apply {
                    val insertIndex = indexOfFirstInstructionOrThrow {
                        getReference<FieldReference>()?.toString() == viewReference
                    }
                    val nullCheckIndex =
                        indexOfFirstInstructionOrThrow(insertIndex - 1, Opcode.IF_NEZ)
                    val nullCheckRegister =
                        getInstruction<OneRegisterInstruction>(nullCheckIndex).registerA

                    addInstruction(
                        nullCheckIndex,
                        "const/4 v$nullCheckRegister, 0x0"
                    )
                }
            }
        }

        LithoFilterPatch.addFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_fullscreen_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_general_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_music_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_paid_promotion_label",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_promotion",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_renewal",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_promotion_alert_banner",
            "true"
        )
    }
}
