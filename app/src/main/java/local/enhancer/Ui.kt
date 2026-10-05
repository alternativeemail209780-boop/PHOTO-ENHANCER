package local.enhancer

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.*

/** Dark, card-style theme applied to a programmatically built screen. */
object Ui {
    val BG = 0xFF0E1116.toInt(); val CARD = 0xFF181C23.toInt(); val LINE = 0xFF2A303B.toInt()
    val ACCENT = 0xFF2DD4A7.toInt(); val TEXT = 0xFFE6E9EF.toInt(); val MUTED = 0xFF9AA3B2.toInt()

    private fun rect(c: Int, r: Float, stroke: Int = 0) = GradientDrawable().apply {
        setColor(c); cornerRadius = r; if (stroke != 0) setStroke(3, stroke)
    }

    fun apply(a: Activity, root: LinearLayout, title: String, subtitle: String) {
        val d = a.resources.displayMetrics.density
        a.window.statusBarColor = BG; a.window.navigationBarColor = BG
        root.setBackgroundColor(BG)
        root.setPadding((20 * d).toInt(), (44 * d).toInt(), (20 * d).toInt(), (20 * d).toInt())
        val head = TextView(a).apply { text = title; textSize = 28f; setTextColor(TEXT); typeface = Typeface.DEFAULT_BOLD }
        val sub = TextView(a).apply { text = "● $subtitle"; textSize = 13f; setTextColor(ACCENT) }
        root.addView(head, 0); root.addView(sub, 1)
        for (i in 2 until root.childCount) style(root.getChildAt(i), d)
    }

    private fun style(v: View, d: Float) {
        (v.layoutParams as? LinearLayout.LayoutParams)?.topMargin = (12 * d).toInt()
        when (v) {
            is Button -> {
                val primary = v.text.contains("Enhance") || v.text.contains("Save")
                v.isAllCaps = false; v.textSize = 16f; v.typeface = Typeface.DEFAULT_BOLD
                v.minHeight = (54 * d).toInt(); v.stateListAnimator = null
                val on = if (primary) rect(ACCENT, 16 * d) else rect(CARD, 16 * d, LINE)
                v.background = StateListDrawable().apply {
                    addState(intArrayOf(-android.R.attr.state_enabled), rect(0xFF20242C.toInt(), 16 * d))
                    addState(intArrayOf(), on)
                }
                v.setTextColor(ColorStateList(
                    arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                    intArrayOf(0xFF5A6272.toInt(), if (primary) 0xFF06241C.toInt() else TEXT)))
            }
            is ProgressBar -> {
                v.progressTintList = ColorStateList.valueOf(ACCENT)
                v.progressBackgroundTintList = ColorStateList.valueOf(LINE)
                if (v is SeekBar) v.thumbTintList = ColorStateList.valueOf(ACCENT)
            }
            is TextView -> { v.setTextColor(MUTED); v.textSize = 14f }
            is ImageView -> {
                v.background = rect(CARD, 20 * d); v.scaleType = ImageView.ScaleType.FIT_CENTER
                val p = (10 * d).toInt(); v.setPadding(p, p, p, p)
            }
        }
    }
}
