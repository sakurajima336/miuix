package top.yukonga.miuix.uitest

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.AdapterView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.wfc35286.coui.kmp.component.*
import io.github.wfc35286.coui.kmp.theme.CouiTheme
import io.github.wfc35286.coui.kmp.token.CouiColorTokens
import io.github.wfc35286.coui.kmp.token.CouiComponentColors

/** Debug-only official COUIPopupListWindow / independent CouiPopupListWindow comparison. */
class PopupGestureProbeActivity : ComponentActivity() {
    private var officialPopup: Any? = null
    private var officialEntries: List<Any> = emptyList()
    private var subMenuDataIndex = -1
    private var started = false
    private var frameId = 0
    private var phase = "idle"
    private val lines = StringBuilder()
    private var epoch = 0L
    private fun field(o: Any, name: String): Any? {
        var c: Class<*>? = o.javaClass
        while(c!=null) {
            val candidate = c
            try { return candidate.getDeclaredField(name).apply { isAccessible=true }.get(o) } catch(_:NoSuchFieldException) { c=candidate.superclass }
        }
        return null
    }
    private fun all(v: View): List<View> = listOf(v) + if(v is android.view.ViewGroup) (0 until v.childCount).flatMap {all(v.getChildAt(it))} else emptyList()
    private fun startTrace() {
        if (started || !intent.getBooleanExtra("auto",false)) return
        started=true
        epoch=android.os.SystemClock.uptimeMillis()
        val handler=android.os.Handler(mainLooper)
        val debug=Class.forName("io.github.wfc35286.coui.kmp.component.CouiPopupDebug32").getField("INSTANCE").get(null)
        fun port(action:String) { (debug.javaClass.getMethod("get"+action.replaceFirstChar {it.uppercase()}).invoke(debug) as Function0<*>).invoke() }
        fun officialRoot():View? = officialPopup?.let {field(it,"mContentView") as? View}
        fun open() {
            phase="opening"
            officialPopup?.let { popup ->
                val list=field(popup,"mMainRecyclerView") as? View ?: field(popup,"mMainListView") as? View ?: error("no list")
                val title = if (intent.getBooleanExtra("mixed", false)) "Title 5" else "Item 4"
                val text=all(list).filterIsInstance<TextView>().first { it.text.toString()==title }
                var row:View=text
                while(row.parent is View && row.parent !== list)row=row.parent as View
                popup.javaClass.getDeclaredMethod("triggerShowSubMenu",View::class.java,Int::class.javaPrimitiveType).apply {isAccessible=true}.invoke(popup,row,subMenuDataIndex)
            } ?: port("open")
        }
        fun close() {
            phase="closing"
            officialRoot()?.let { it.javaClass.getMethod("hideSubMenu",Boolean::class.javaPrimitiveType).invoke(it,true) } ?: port("close")
        }
        handler.postDelayed({open()},1000)
        handler.postDelayed({close()},2400)
        if(intent.getBooleanExtra("reverse",false)) {
            handler.postDelayed({open()},2520)
            handler.postDelayed({close()},3900)
        }
        if(intent.getBooleanExtra("dismissAll",false)) {
            handler.postDelayed({open()},4200)
            handler.postDelayed({phase="dismiss";officialPopup?.let {it.javaClass.getMethod("dismiss").invoke(it)} ?: port("dismiss")},4500)
        }
        val duration=if(intent.getBooleanExtra("reverse",false)||intent.getBooleanExtra("dismissAll",false))6500L else 4500L
        val cb=object:android.view.Choreographer.FrameCallback {
            override fun doFrame(time:Long) {
                val elapsed=android.os.SystemClock.uptimeMillis()-epoch
                val record=org.json.JSONObject().put("i",frameId++).put("t",elapsed).put("phase",phase)
                fun view(v:View?):org.json.JSONObject {
                    if(v==null)return org.json.JSONObject()
                    val o=org.json.JSONObject().put("left",v.left).put("top",v.top).put("w",v.width).put("h",v.height).put("tx",v.translationX).put("ty",v.translationY).put("sx",v.scaleX).put("sy",v.scaleY).put("px",v.pivotX).put("py",v.pivotY).put("alpha",v.alpha).put("visible",v.visibility)
                    val texts=org.json.JSONArray()
                    for(text in all(v).filterIsInstance<TextView>()) {
                        val rect=android.graphics.Rect();text.getGlobalVisibleRect(rect)
                        texts.put(org.json.JSONObject().put("title",text.text.toString()).put("x",rect.left).put("y",rect.top).put("w",rect.width()).put("h",rect.height()).put("textPx",text.textSize))
                    }
                    return o.put("texts",texts)
                }
                try {
                    val root=officialRoot()
                    if(root!=null) {
                        record.put("main",view(field(root,"mMainMenuRootView") as? View)).put("sub",view(field(root,"mSubMenuRootView") as? View))
                        val locate=field(officialPopup!!,"mLocateHelper")!!
                        val domain=field(locate,"mDomain")
                        fun rect(o:Any?, name:String):String? = o?.let { field(it,name)?.toString() }
                        record.put("domain",org.json.JSONObject().put("main",rect(domain,"mMainMenu")).put("relocated",rect(domain,"mMainMenuRelocated")).put("sub",rect(domain,"mSubMenu")).put("row",rect(domain,"mSubMenuAnchor")).put("available",rect(locate,"mAvailableBounds")))
                        val ctrl=field(root,"mController")!!
                        record.put("blur",org.json.JSONObject().put("requested",field(officialPopup!!,"mUseBackgroundBlur")).put("mainBackground",(field(root,"mMainMenuRootView") as? View)?.background?.javaClass?.name).put("subBackground",(field(root,"mSubMenuRootView") as? View)?.background?.javaClass?.name))
                        fun detail(v:View?):org.json.JSONObject {
                            if(v==null)return org.json.JSONObject()
                            val o=org.json.JSONObject().put("override",field(v,"mOverrideRect")).put("outlineAlpha",field(v,"mOutlineAlpha"))
                            val rect=android.graphics.Rect();v.getGlobalVisibleRect(rect)
                            o.put("bounds",rect.toString())
                            val child=(v as? android.view.ViewGroup)?.getChildAt(0)
                            o.put("contentAlpha",child?.alpha)
                            val rows=org.json.JSONArray()
                            if(child is android.view.ViewGroup) for(i in 0 until child.childCount) {
                                val r=child.getChildAt(i)
                                rows.put(org.json.JSONObject().put("top",r.top).put("h",r.height).put("titles",all(r).filterIsInstance<TextView>().map {it.text.toString()}.joinToString(";")))
                            }
                            return o.put("rows",rows)
                        }
                        record.put("mainDetail",detail(field(root,"mMainMenuRootView") as? View)).put("subDetail",detail(field(root,"mSubMenuRootView") as? View))
                        record.put("controller",org.json.JSONObject().put("kind",ctrl.javaClass.name).put("progress",field(ctrl,"mLastRealProgress")).put("yStart",field(ctrl,"mSubMenuTranslateStartY")))
                    } else {
                        fun get(name:String):Any?=debug.javaClass.getMethod("get"+name.replaceFirstChar {it.uppercase()}).invoke(debug)
                        record.put("port",debug.javaClass.getMethod("record").invoke(debug)).put("titles",debug.javaClass.getMethod("titles").invoke(debug)).put("details",debug.javaClass.getMethod("frameDetails").invoke(debug))
                    }
                } catch(e:Throwable) {record.put("error",e.toString())}
                lines.append(record.toString()).append('\n')
                if(elapsed<duration)android.view.Choreographer.getInstance().postFrameCallback(this) else {
                    val name=intent.getStringExtra("traceName") ?: "popup32-trace"
                    java.io.File(getExternalFilesDir(null),name+".jsonl").writeText(lines.toString())
                    android.util.Log.d("PopupTrace32","saved $name frames=$frameId")
                }
            }
        }
        android.view.Choreographer.getInstance().postFrameCallback(cb)
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        // Debug: stretch the enter/exit springs so a recording can be stepped frame by frame.
        runCatching {
            val c = Class.forName("io.github.wfc35286.coui.kmp.component.CouiPopupDebug32")
            val inst = c.getField("INSTANCE").get(null)
            c.getMethod("setSlowMotion", Boolean::class.javaPrimitiveType)
                .invoke(inst, intent.getBooleanExtra("slow", false))
        }
        val dark = intent.getBooleanExtra("dark", true)
        val official = intent.getBooleanExtra("official", false)
        val mixed = intent.getBooleanExtra("mixed", false)
        val entries = if (mixed) listOf(
            // CouiCoreTestPage's CouiMixedMenuDemo, verbatim: the divider rows shift the indices
            // the same way they do in the app page that shows the "starts one row late" symptom.
            CouiPopupListItem("Title 1", checkable = true, checked = true, groupId = 0),
            CouiPopupListItem("Title 2", checkable = true, groupId = 0),
            CouiPopupListItem("Title 3", checkable = true, checked = true, groupId = 0),
            CouiPopupListItem("Title 4", checkable = true, groupId = 0),
            CouiPopupListItem(
                "Title 5",
                groupId = 1,
                subItems = listOf(
                    CouiPopupListItem("Item A"),
                    CouiPopupListItem("Item B"),
                    CouiPopupListItem("Item C"),
                ),
            ),
            CouiPopupListItem("Title 6", groupId = 1),
            CouiPopupListItem("Title 7", groupId = 2),
            CouiPopupListItem("Title 8", groupId = 2),
            CouiPopupListItem("Title 9", groupId = 2),
            CouiPopupListItem("Title 10", groupId = 2),
            CouiPopupListItem("Title 11", groupId = 2),
            CouiPopupListItem("Title 12", groupId = 2),
            CouiPopupListItem("Title 13", groupId = 2),
            CouiPopupListItem("Title 14", groupId = 2),
        ) else listOf(
            CouiPopupListItem("Group A", type = CouiPopupItemType.Header),
            CouiPopupListItem("Item 1"),
            CouiPopupListItem("Item 2"),
            CouiPopupListItem("Group B", type = CouiPopupItemType.Header),
            CouiPopupListItem("Item 3"),
            CouiPopupListItem("Item 4", subItems = listOf(CouiPopupListItem("Sub A"), CouiPopupListItem("Sub B"))),
            // popup_list_window_item_icon + popup_list_window_item_description coverage.
            CouiPopupListItem(
                "Item 5",
                leadingIcon = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF4C8DFF)),
                description = "Description line",
            ),
        ) + List(intent.getIntExtra("tailCount", 0)) { CouiPopupListItem("Tail ${it+1}") }
        if (official) {
            val settings = createPackageContext("com.android.settings", Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE)
            val config = android.content.res.Configuration(settings.resources.configuration).apply {
                uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or (if(dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO)
            }
            val theme = settings.resources.getIdentifier("AppBaseTheme.NoActionBar", "style", "com.android.settings")
            val ctx = ContextThemeWrapper(settings.createConfigurationContext(config),theme)
            val cl = ctx.classLoader
            val page = FrameLayout(this)
            page.addView(PopupProbeBackground(this, dark), FrameLayout.LayoutParams(-1, -1))
            val result = TextView(this).apply { text = "Value"; setTextColor(if (dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK); textSize = 15f }
            page.addView(result, FrameLayout.LayoutParams(530,238))
            val anchor = View(ctx)
            page.addView(anchor, FrameLayout.LayoutParams(1,1).apply {leftMargin=900; topMargin=intent.getIntExtra("anchorY",500)})
            setContentView(page)
            fun item(entry: CouiPopupListItem): Any {
                val b = cl.loadClass("com.coui.appcompat.poplist.PopupListItem\$Builder")
                val o = b.getConstructor().newInstance()
                b.getMethod("setTitle",String::class.java).invoke(o,entry.title)
                b.getMethod("setItemType",Int::class.javaPrimitiveType).invoke(o,if(entry.type==CouiPopupItemType.Header)3 else 0)
                b.getMethod("setIsEnable",Boolean::class.javaPrimitiveType).invoke(o,entry.enabled)
                b.getMethod("setIsCheckable",Boolean::class.javaPrimitiveType).invoke(o,entry.checkable)
                b.getMethod("setIsChecked",Boolean::class.javaPrimitiveType).invoke(o,entry.checked)
                b.getMethod("setGroupId",Int::class.javaPrimitiveType).invoke(o,entry.groupId)
                entry.description?.let { b.getMethod("setDescription",String::class.java).invoke(o,it) }
                if(entry.leadingIcon!=null)b.getMethod("setIcon",android.graphics.drawable.Drawable::class.java).invoke(o,android.graphics.drawable.ColorDrawable(0xFF4C8DFF.toInt()))
                if(entry.subItems.isNotEmpty())b.getMethod("setSubMenuItemList",ArrayList::class.java).invoke(o,ArrayList(entry.subItems.map {item(it)}))
                return b.getMethod("build").invoke(o)!!
            }
            val c=cl.loadClass("com.coui.appcompat.poplist.COUIPopupListWindow")
            val popup=c.getConstructor(Context::class.java).newInstance(ctx)
            officialPopup=popup
            c.getMethod("setUseBackgroundBlur",Boolean::class.javaPrimitiveType).invoke(popup,true)
            officialEntries = entries.map {item(it)}
            subMenuDataIndex = entries.indexOfFirst { it.subItems.isNotEmpty() }
            c.getMethod("setItemList",List::class.java).invoke(popup,officialEntries)
            c.getMethod("setOnItemClickListener",AdapterView.OnItemClickListener::class.java).invoke(popup,AdapterView.OnItemClickListener {_,_,index,_->result.text="Main $index"})
            c.getMethod("setSubMenuClickListener",AdapterView.OnItemClickListener::class.java).invoke(popup,AdapterView.OnItemClickListener {_,_,index,_->result.text="Sub 3/$index";c.getMethod("dismiss").invoke(popup)})
            anchor.postDelayed({c.getMethod("show",View::class.java).invoke(popup,anchor);startTrace()},700)
        } else {
            android.os.Handler(mainLooper).postDelayed({startTrace()},700)
            setContent {
                CouiTheme(colors=if(dark)CouiColorTokens.Dark else CouiColorTokens.Light,componentColors=if(dark)CouiComponentColors.Dark else CouiComponentColors.Light) {
                    var expanded by remember { mutableStateOf(true) }
                    var result by remember { mutableStateOf("Value") }
                    // `touchPoint` is what the popup anchors to. Default: the finger's own down
                    // position, so the menu opens where the user pressed (official
                    // `COUIPopupListWindow.showAtLocation` + `PopupMenuLocateHelper$12.locateX`).
                    // `--ez fixedAnchor true` restores the old fixed (900, anchorY) probe anchor,
                    // which the frame-by-frame trace comparisons rely on.
                    val fixedAnchor = intent.getBooleanExtra("fixedAnchor", false)
                    val anchorY = intent.getIntExtra("anchorY", 500).toFloat()
                    var pressPoint by remember { mutableStateOf<Offset?>(null) }
                    Box(Modifier.fillMaxSize()) {
                        androidx.compose.ui.viewinterop.AndroidView(factory={PopupProbeBackground(it,dark)},modifier=Modifier.fillMaxSize())
                        BasicText(result, style=androidx.compose.ui.text.TextStyle(color=if(dark)Color.White else Color.Black),modifier=Modifier.size(180.dp,80.dp))
                        Box(
                            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                awaitEachGesture {
                                    pressPoint = awaitFirstDown(requireUnconsumed = false).position
                                }
                            },
                        ) {
                            CouiPopupListWindow(expanded=expanded,items=entries,selectedIndex=-1,onItemClick={result="Main $it"},onSubItemClick={parent,index->result="Sub $parent/$index";expanded=false},onDismissRequest={expanded=false},touchPoint=if(fixedAnchor)Offset(900f,anchorY)else pressPoint,debugAutoOpenSubMenu=intent.getIntExtra("autoOpenSub",-1))
                        }
                    }
                }
            }
        }
    }
}


private class PopupProbeBackground(context: Context, private val dark: Boolean) : View(context) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: android.graphics.Canvas) {
        canvas.drawColor(if (dark) 0xFF000000.toInt() else 0xFFF0F1F2.toInt())
        for (i in 0..20) {
            paint.color = if (i % 2 == 0) 0x334C8DFF else 0x22FF9A4C
            canvas.drawRect(0f, i * 144f, width.toFloat(), i * 144f + 72f, paint)
        }
    }
}
