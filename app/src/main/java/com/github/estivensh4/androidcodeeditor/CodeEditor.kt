package com.github.estivensh4.androidcodeeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupWindow
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class CodeEditor : WebView {

    // we define variables
    var mContext: Context
    private lateinit var pw: PopupWindow
    private lateinit var popupView: View
    private lateinit var inflater: LayoutInflater
    private lateinit var received: ResultReceivedListener
    private lateinit var onLoadedEditorListener: OnLoadedEditorListener
    private lateinit var onSelectionActionPerformedListener: OnSelectionActionPerformedListener
    private var mX = 0f
    private var mY = 0f
    private var actAfterSelect = false
    private var requestedValue = 0
    private var findString: String? = null
    private var loadedUI: Boolean
    private lateinit var scroller: OnTouchListener
    private lateinit var selector: OnTouchListener

    @SuppressLint("SetJavaScriptEnabled")
    constructor(context: Context) : super(context) {
        loadedUI = false
        this.mContext = context
        initialize()
    }

    @SuppressLint("SetJavaScriptEnabled")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        loadedUI = false
        this.mContext = context
        initialize()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return BaseInputConnection(this, false) //this is needed for #dispatchKeyEvent() to be notified.
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialize() {
        inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        actAfterSelect = true
        initPopup()
        setResultReceivedListener(object : ResultReceivedListener {
            override fun onReceived(FLAG_VALUE: Int, vararg results: String?) {}
        })
        setOnLoadedEditorListener(object : OnLoadedEditorListener {
            override fun onCreate() {}
        })
        setOnSelectionActionPerformedListener(object : OnSelectionActionPerformedListener {
            override fun onSelectionFinished(usingSelectAllOption: Boolean) {}
            override fun onCut() {}
            override fun onCopy() {}
            override fun onPaste() {}
            override fun onUndo() {}
            override fun onRedo() {}
        })
        webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult,
            ): Boolean {
                result.confirm()
                val results = LinkedList<String>()
                try {
                    val objArr = JSONArray(message)
                    for (i in 0 until objArr.length()) {
                        results.add(objArr[i].toString())
                    }
                } catch (e: JSONException) {
                    try {
                        val obj = JSONObject(message)
                        val keyIterator = obj.keys()
                        while (keyIterator.hasNext()) {
                            val key = keyIterator.next()
                            results.add(obj[key].toString())
                        }
                    } catch (e1: JSONException) {
                        results.add(message)
                    }
                }
                var res: Array<String?> = arrayOfNulls(results.size)
                res = results.toArray<String>(res)
                received.onReceived(requestedValue, *res)
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (!loadedUI) {
                    loadedUI = true
                    onLoadedEditorListener.onCreate()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }
        }
        selector = object : OnTouchListener {
            var downTime = 0f
            var xTimes = 0
            var yTimes = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = event.eventTime.toFloat()
                        mX = event.x
                        mY = event.y
                    }
                    MotionEvent.ACTION_UP -> {
                        val tot = SystemClock.uptimeMillis() - downTime
                        mX = event.x
                        mY = event.y
                        if (tot <= 500) v.performClick() else {
                            if (actAfterSelect) pw.showAtLocation(v,
                                Gravity.NO_GRAVITY,
                                mX.toInt() - resources.displayMetrics.widthPixels / 3,
                                resources.displayMetrics.heightPixels / 12 + mY.toInt())
                            onSelectionActionPerformedListener.onSelectionFinished(false)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        xTimes = (mX - event.x).toInt() / 25
                        yTimes = (mY - event.y).toInt() / 60
                        if (xTimes > 0) {
                            v.dispatchKeyEvent(KeyEvent(0,
                                0,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                xTimes,
                                KeyEvent.META_SHIFT_ON))
                            mX = event.x
                        } else if (xTimes < 0) {
                            v.dispatchKeyEvent(KeyEvent(0,
                                0,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_RIGHT,
                                -xTimes,
                                KeyEvent.META_SHIFT_ON))
                            mX = event.x
                        }
                        if (yTimes > 0) {
                            v.dispatchKeyEvent(KeyEvent(0,
                                0,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_UP,
                                yTimes,
                                KeyEvent.META_SHIFT_ON))
                            mY = event.y
                        } else if (yTimes < 0) {
                            v.dispatchKeyEvent(KeyEvent(0,
                                0,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                -yTimes,
                                KeyEvent.META_SHIFT_ON))
                            mY = event.y
                        }
                    }
                }
                return false
            }
        }
        scroller = object : OnTouchListener {
            var downTime = 0f
            var xTimes = 0
            var yTimes = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = event.eventTime.toFloat()
                        mX = event.x
                        mY = event.y
                    }
                    MotionEvent.ACTION_UP -> {
                        mX = event.x
                        mY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        xTimes = (mX - event.x).toInt()
                        yTimes = (mY - event.y).toInt()
                        scrollBy(xTimes, yTimes)
                    }
                }
                return false
            }
        }
        setOnTouchListener(selector)
        setOnLongClickListener { v ->
            if (!pw.isShowing) pw.showAtLocation(v,
                Gravity.NO_GRAVITY,
                mX.toInt() - resources.displayMetrics.widthPixels / 3,
                resources.displayMetrics.heightPixels / 12 + mY.toInt())
            true
        }
        settings.javaScriptEnabled = true
        loadUrl("file:///android_asset/index.html")
    }

    @SuppressLint("InflateParams")
    private fun initPopup() {
        pw = PopupWindow(mContext)
        pw.height = resources.displayMetrics.heightPixels / 15
        pw.width = 75 * resources.displayMetrics.widthPixels / 100
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pw.elevation = 50.0f
        }
        pw.isOutsideTouchable = true
        pw.isTouchable = true
        popupView = inflater.inflate(R.layout.webview_dialog_options, null)
        val optSet1 = popupView.findViewById<View>(R.id.optSet1)
        val optSet2 = popupView.findViewById<View>(R.id.optSet2)
        popupView.findViewById<View>(R.id.nextOptSet).setOnClickListener {
            optSet1.visibility = GONE
            optSet2.visibility = VISIBLE
        }
        popupView.findViewById<View>(R.id.prevOptSet).setOnClickListener {
            optSet2.visibility = GONE
            optSet1.visibility = VISIBLE
        }
        popupView.findViewById<View>(R.id.cut).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_X,
                0,
                KeyEvent.META_CTRL_ON))
            this@CodeEditor.requestFocus()
            pw.dismiss()
            onSelectionActionPerformedListener.onCut()
        }
        popupView.findViewById<View>(R.id.copy).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_C,
                0,
                KeyEvent.META_CTRL_ON))
            this@CodeEditor.requestFocus()
            pw.dismiss()
            onSelectionActionPerformedListener.onCopy()
        }
        popupView.findViewById<View>(R.id.paste).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_V,
                0,
                KeyEvent.META_CTRL_ON))
            this@CodeEditor.requestFocus()
            pw.dismiss()
            onSelectionActionPerformedListener.onPaste()
        }
        popupView.findViewById<View>(R.id.selectall).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A,
                0,
                KeyEvent.META_CTRL_ON))
            popupView.findViewById<View>(R.id.prevOptSet).performClick()
            onSelectionActionPerformedListener.onSelectionFinished(true)
        }
        popupView.findViewById<View>(R.id.undo).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_Z,
                0,
                KeyEvent.META_CTRL_ON))
            onSelectionActionPerformedListener.onUndo()
        }
        popupView.findViewById<View>(R.id.redo).setOnClickListener {
            dispatchKeyEvent(KeyEvent(0,
                0,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_Z,
                0,
                KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON))
            onSelectionActionPerformedListener.onRedo()
        }
        pw.setOnDismissListener {
            optSet2.visibility = GONE
            optSet1.visibility = VISIBLE
        }
        pw.contentView = popupView
    }

    fun setResultReceivedListener(listener: ResultReceivedListener) {
        received = listener
    }

    fun setOnLoadedEditorListener(listener: OnLoadedEditorListener) {
        onLoadedEditorListener = listener
    }

    private fun setOnSelectionActionPerformedListener(listener: OnSelectionActionPerformedListener) {
        onSelectionActionPerformedListener = listener
    }

    fun showOptionsAfterSelection(show: Boolean) {
        actAfterSelect = show
    }

    fun setText(text: String) {
        loadUrl("javascript:editor.getSession().setValue(\"$text\");")
    }

    fun setFontSize(size: Int) {
        loadUrl("javascript:editor.setFontSize($size);")
    }

    fun insertTextAtCursor(text: String) {
        loadUrl("javascript:editor.insert(\"$text\");")
    }

    fun getText() : String {
        var textResult = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("javascript:editor.getValue();") { text ->
                textResult = text
            }
        }
        return textResult
    }

    fun requestRowCount() {
        requestedValue = Request.ROW_COUNT_REQUEST
        loadUrl("javascript:alert(editor.getSession().getLength());")
    }

    fun requestSelectedText() {
        requestedValue = Request.TEXT_REQUEST
        loadUrl("javascript:alert(editor.getSelectedText());")
    }

    fun requestCursorCoordinates() {
        requestedValue = Request.CURSOR_COORDINATES_REQUEST
        loadUrl("javascript:alert(JSON.stringify(editor.getCursorPosition()))")
    }

    fun requestLine(lineNumber: Int) {
        requestedValue = Request.TEXT_REQUEST
        loadUrl("javascript:alert(editor.getSession().getLine($lineNumber));")
    }

    fun requestLinesBetween(startLine: Int, endLine: Int) {
        requestedValue = Request.MULTIPLE_LINES_REQUEST
        loadUrl("javascript:alert(JSON.stringify(editor.getSession().getLines($startLine, $endLine)));")
    }

    fun startFind(
        toFind: String,
        backwards: Boolean,
        wrap: Boolean,
        caseSensitive: Boolean,
        wholeWord: Boolean,
    ) {
        findString = toFind
        loadUrl("javascript:editor.find('" + toFind + "', backwards: " + backwards.toString() +
                ", wrap: " + wrap.toString() +
                ",caseSensitive: " + caseSensitive.toString() +
                ",wholeWord: " + wholeWord.toString() + ",regExp: false});")
    }

    fun findNext() {
        if (findString == null) {
            return
        }
        loadUrl("javascript:editor.findNext();")
    }

    fun findNext(errorToastMessage: String?, showFor: Int) {
        if (findString == null) {
            Toast.makeText(mContext, errorToastMessage, showFor).show()
            return
        }
        loadUrl("javascript:editor.findNext();")
    }

    fun findPrevious() {
        if (findString == null) {
            return
        }
        loadUrl("javascript:editor.findPrevious();")
    }

    fun findPrevious(toastMessage: String?, showFor: Int) {
        if (findString == null) {
            Toast.makeText(mContext, toastMessage, showFor).show()
            return
        }
        loadUrl("javascript:editor.findPrevious();")
    }

    fun replace(replaceText: String, replaceAll: Boolean) {
        if (replaceAll) loadUrl("javascript:editor.replaceAll('$replaceText');") else loadUrl("javascript:editor.replace('$replaceText');")
    }

    fun endFind() {
        findString = null
    }

    fun setSoftWrap(enabled: Boolean) {
        if (enabled) loadUrl("javascript:editor.getSession().setUseWrapMode(true);") else loadUrl("javascript:editor.getSession().setUseWrapMode(false);")
    }

    fun setTheme(theme: Theme) {
        loadUrl("javascript:editor.setTheme(\"ace/theme/${theme.name.lowercase(Locale.getDefault())}\");")
    }

    fun language(language: Language) {
        loadUrl("javascript:editor.getSession().setMode(\"ace/mode/${language.name.lowercase(Locale.getDefault())}\");")
    }

    fun setTouchAction(action: Int) {
        if (action == ACTION_SCROLL) setOnTouchListener(scroller) else setOnTouchListener(selector)
    }

    object Request {
        var GENERIC_REQUEST = 0
        var TEXT_REQUEST = 1
        var ROW_COUNT_REQUEST = 2
        var CURSOR_COORDINATES_REQUEST = 3
        var MULTIPLE_LINES_REQUEST = 4
    }

    enum class Theme {
        AMBIANCE,
        CHAOS,
        CHROME,
        CLOUDS,
        CLOUDS_MIDNIGHT,
        COBALT,
        CRIMSON_EDITOR,
        DAWN,
        DRACULA,
        DREAMWEAVER,
        ECLIPSE,
        GITHUB,
        GOB,
        GRUVBOX,
        IDLE_FINGERS,
        IPLASTIC,
        KATZENMILCH,
        KR_THEME,
        KUROIR,
        MERBIVORE,
        MERBIVORE_SOFT,
        MONO_INDUSTRIAL,
        MONOKAI,
        PASTEL_ON_DARK,
        SOLARIZED_DARK,
        SOLARIZED_LIGHT,
        SQLSERVER,
        TERMINAL,
        TEXTMATE,
        TOMORROW,
        TOMORROW_NIGHT,
        TOMORROW_NIGHT_BLUE,
        TOMORROW_NIGHT_BRIGHT,
        TOMORROW_NIGHT_EIGHTIES,
        TWILIGHT,
        VIBRANT_INK,
        XCODE
    }

    enum class Language {
        ABAP,
        ABC,
        ActionScript,
        ADA,
        Apache_Conf,
        AsciiDoc,
        Assembly_x86,
        AutoHotKey,
        BatchFile,
        C9Search,
        C_Cpp,
        Cirru,
        Clojure,
        Cobol,
        coffee,
        ColdFusion,
        CSharp,
        CSS,
        Curly,
        D,
        Dart,
        Diff,
        Dockerfile,
        Dot,
        Dummy,
        DummySyntax,
        Eiffel,
        EJS,
        Elixir,
        Elm,
        Erlang,
        Forth,
        FTL,
        Gcode,
        Gherkin,
        Gitignore,
        Glsl,
        golang,
        Groovy,
        HAML,
        Handlebars,
        Haskell,
        haXe,
        HTML,
        HTML_Ruby,
        INI,
        Io,
        Jack,
        Jade,
        Java,
        JavaScript,
        JSON,
        JSONiq,
        JSP,
        JSX,
        Julia,
        LaTeX,
        LESS,
        Liquid,
        Lisp,
        LiveScript,
        LogiQL,
        LSL,
        Lua,
        LuaPage,
        Lucene,
        Makefile,
        Markdown,
        Mask,
        MATLAB,
        MEL,
        MUSHCode,
        MySQL,
        Nix,
        ObjectiveC,
        OCaml,
        Pascal,
        Perl,
        pgSQL,
        PHP,
        Powershell,
        Praat,
        Prolog,
        Properties,
        Protobuf,
        Python,
        R,
        RDoc,
        RHTML,
        Ruby,
        Rust,
        SASS,
        SCAD,
        Scala,
        Scheme,
        SCSS,
        SH,
        SJS,
        Smarty,
        snippets,
        Soy_Template,
        Space,
        SQL,
        Stylus,
        SVG,
        Tcl,
        Tex,
        Text,
        Textile,
        Toml,
        Twig,
        Typescript,
        Vala,
        VBScript,
        Velocity,
        Verilog,
        VHDL,
        XML,
        XQuery,
        YAML
    }

    companion object {
        var ACTION_SCROLL = 1
        var ACTION_SELECT = 0
    }
}