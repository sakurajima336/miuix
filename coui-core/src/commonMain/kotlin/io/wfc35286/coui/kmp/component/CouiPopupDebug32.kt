package io.wfc35286.coui.kmp.component
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
internal object CouiPopupDebug32 {
    var slowMotion = false
    var main: CouiPopupVisualState? = null
    var sub: CouiPopupVisualState? = null
    var driver: CouiPopupTransitionDriver? = null
    var open: () -> Unit = {}
    var close: () -> Unit = {}
    var dismiss: () -> Unit = {}
    var mainOffset = Offset.Zero
    var subOffset = Offset.Zero
    var row = Rect.Zero
    var mainPanel: CouiPopupPanelState? = null
    var subPanel: CouiPopupPanelState? = null
    private val mainTitles = mutableMapOf<Int, LayoutCoordinates>()
    private val subTitles = mutableMapOf<Int, LayoutCoordinates>()
    fun title(panel: CouiPopupPanelState, index: Int, coordinates: LayoutCoordinates) {
        if (panel === mainPanel) mainTitles[index] = coordinates
        if (panel === subPanel) subTitles[index] = coordinates
    }
    // Dev-only counters: how often the menu subtree recomposes vs how often the draw-time
    // graphicsLayer blocks run. If recomposition tracks the frame rate, the animation is
    // driving composition instead of draw.
    private val ticks = HashMap<String, Int>()
    private var tickFrames = 0

    fun tick(tag: String) {
        ticks[tag] = (ticks[tag] ?: 0) + 1
    }

    fun tickFrame() {
        tickFrames++
        if (tickFrames < 60) return
        tickFrames = 0
        val summary = ticks.entries.joinToString(" ") { it.key + "=" + it.value }
        ticks.clear()
        println("CouiPopupTick 60f $summary")
    }

    fun titles(): String {
        fun panel(map: Map<Int, LayoutCoordinates>): String = map.entries.filter { it.value.isAttached }.joinToString(";") { (index, c) ->
            val b = c.boundsInWindow()
            "$index,${b.left},${b.top},${b.width},${b.height}"
        }
        return "${panel(mainTitles)}|${panel(subTitles)}"
    }
    fun frameDetails(): String {
        fun v(state: CouiPopupVisualState?): String {
            if (state == null) return "null"
            val f = state.sampleFrame()
            return "${f.translation}|${f.scaleX}|${f.scaleY}|${f.alpha}|${f.contentAlpha}|${f.revealBounds}|${state.translation}|${state.contentAlpha}|${state.contentReveal}"
        }
        val d = driver?.domain
        return "${d?.mainMenu}|${d?.mainMenuRelocated}|${d?.subMenu}|${d?.subMenuAnchor}|${d?.window}|${v(main)}|${v(sub)}"
    }
    fun record(): String {
        fun visual(v: CouiPopupVisualState?, offset: Offset): String {
            if (v == null) return "null"
            val f = v.sampleFrame()
            val p = v.panelSize
            val rowY = offset.y + p.y / 2f + f.translation.y + (row.top - p.y / 2f) * f.scaleY
            // Trailing fields: the reveal the material path is built from, and the reveal the
            // content clip actually reads. They must be identical on the same frame.
            return "${offset.x},${offset.y},${p.x},${p.y},${f.translation.x},${f.translation.y},${f.scaleX},${f.scaleY},${f.contentAlpha},$rowY,${f.revealBounds},${v.contentReveal},${v.translation},${v.scaleY}"
        }
        return "${driver?.phase}|${driver?.mainProgress}|${driver?.subProgress}|${visual(main,mainOffset)}|${visual(sub,subOffset)}" +
            "|${driver?.revealActive}|${driver?.revealTop}|${driver?.revealHeight}|${driver?.outlineAlpha}" +
            "|${driver?.subTranslationY}|${driver?.subScaleY}|${driver?.mainTranslationY}|${driver?.mainScaleY}|${driver?.mainContentAlpha}"
    }
}
