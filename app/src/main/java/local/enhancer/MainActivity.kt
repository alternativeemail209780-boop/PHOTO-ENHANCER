package local.enhancer

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private lateinit var img: ImageView
    private lateinit var bar: ProgressBar
    private lateinit var status: TextView
    private lateinit var save: Button
    private var src: Bitmap? = null
    private var result: Bitmap? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 96, 32, 32) }
        val pick = Button(this).apply { text = "Choose photo" }
        img = ImageView(this)
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        status = TextView(this).apply { text = "Runs fully offline." }
        val go = Button(this).apply { text = "Enhance (4x)" }
        save = Button(this).apply { text = "Save to gallery"; isEnabled = false }
        root.addView(pick); root.addView(img, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bar); root.addView(status); root.addView(go); root.addView(save)
        setContentView(root)
        Ui.apply(this, root, "Photo Enhancer", "100% offline - nothing leaves this device")
        pick.setOnClickListener { startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 1) }
        go.setOnClickListener { enhance() }
        save.setOnClickListener { saveResult() }
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        val uri = d?.data ?: return
        val b = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { dec, info, _ ->
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val m = maxOf(info.size.width, info.size.height)
            if (m > 1600) { val k = 1600f / m; dec.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt()) }
        }
        src = b; result = null; save.isEnabled = false
        img.setImageBitmap(b); status.text = "${b.width}x${b.height} loaded"
    }

    private fun enhance() {
        val b = src ?: return
        status.text = "Loading model..."
        Thread {
            try {
                val eta = Eta()
                val r = Upscaler(this).run(b) { d, t ->
                    runOnUiThread { bar.progress = d * 100 / t; status.text = eta.text(d.toLong(), t.toLong()) }
                }
                result = r
                val k = 1600f / maxOf(r.width, r.height)
                val prev = Bitmap.createScaledBitmap(r, (r.width * k).toInt(), (r.height * k).toInt(), true)
                runOnUiThread { img.setImageBitmap(prev); save.isEnabled = true; status.text = "Done: ${r.width}x${r.height}" }
            } catch (e: Throwable) { runOnUiThread { status.text = "Error: $e" } }
        }.start()
    }

    private fun saveResult() {
        val r = result ?: return
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "enhanced_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Enhanced")
        }
        val u = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
        contentResolver.openOutputStream(u)!!.use { r.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        status.text = "Saved to Pictures/Enhanced"
    }
}
