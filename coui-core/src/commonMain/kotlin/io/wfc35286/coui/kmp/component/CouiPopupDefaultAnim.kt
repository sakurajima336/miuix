
package io.wfc35286.coui.kmp.component

internal class CouiPopupDefaultScreenController(
    private val helper: CouiPopupLocateHelper,
) {
    val domain: CouiPopupDomain get() = helper.domain

    companion object {
        const val DEFAULT_MAIN_MENU_ALPHA_PERCENT = 0.7f

        const val DEFAULT_MAIN_MENU_SCALE_PERCENT = 0.9f

        const val DEFAULT_ALPHA = 1.0f

        const val DEFAULT_SCALE = 1.0f

        const val MIN_ALPHA = 0.1f

        const val PIVOT_CENTER_RATIO = 0.5f

        const val SUB_MENU_ENTER_RESPONSE = 0.35f

        const val SUB_MENU_ENTER_BOUNCE = 0f
    }

    var mainMenuScalePercent = DEFAULT_MAIN_MENU_SCALE_PERCENT
        private set

    var mainMenuAlphaPercent = DEFAULT_MAIN_MENU_ALPHA_PERCENT
        private set

    fun configMainMenuAnimationPropertiesForSubMenuEnter() {
        mainMenuScalePercent = DEFAULT_MAIN_MENU_SCALE_PERCENT
        mainMenuAlphaPercent = DEFAULT_MAIN_MENU_ALPHA_PERCENT
    }

    fun mainMenuEnterPivotX(): Int = (domain.mainMenu.width() * PIVOT_CENTER_RATIO).toInt()
    fun mainMenuEnterPivotY(): Int = (domain.mainMenu.height() * PIVOT_CENTER_RATIO).toInt()

    fun subMenuEnterPivotX(): Int = helper.domain.getSubMenuEnterPivotX()
    fun subMenuEnterPivotY(): Int = helper.domain.getSubMenuEnterPivotY()

    fun startMainMenuSubMenuEnter(menuUnit: CouiPopupMenuTransitionUnit) {
        menuUnit.animateToSubMenuEnterState(
            scaleX = mainMenuScalePercent,
            scaleY = mainMenuScalePercent,
            translationY = 0f,
            alpha = mainMenuAlphaPercent,
            bounce = SUB_MENU_ENTER_BOUNCE,
            response = SUB_MENU_ENTER_RESPONSE,
        )
    }

    fun startSubMenuEnter(subUnit: CouiPopupSubMenuTransitionUnit) {
        subUnit.configForDefaultScreenEnter(scaleStart = 0f, alphaStart = 0f)
        subUnit.animateToEnterState(SUB_MENU_ENTER_BOUNCE, SUB_MENU_ENTER_RESPONSE)
    }

    fun startSubMenuExit(menuUnit: CouiPopupMenuTransitionUnit, subUnit: CouiPopupSubMenuTransitionUnit) {
        menuUnit.animateToSubMenuExitState(SUB_MENU_ENTER_BOUNCE, SUB_MENU_ENTER_RESPONSE)
        subUnit.animateToExitState(SUB_MENU_ENTER_BOUNCE, SUB_MENU_ENTER_RESPONSE)
    }
}