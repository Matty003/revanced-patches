package app.revanced.patches.reddit.layout.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.ToolBarNavSearchCtaContainer
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object HomePagerScreenFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/HomePagerScreen;")
                && methodDef.containsWideLiteralInstructionValue(ToolBarNavSearchCtaContainer)
    }
)