package local.enhancer

import ai.onnxruntime.*
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import java.nio.FloatBuffer

/** Percentage + estimated time remaining. */
class Eta {
    private val t0 = SystemClock.elapsedRealtime()
    fun text(done: Long, total: Long): String {
        val pct = if (total == 0L) 0 else (done * 100 / total).toInt()
        if (done == 0L) return "$pct% - estimating time..."
        val s = (SystemClock.elapsedRealtime() - t0) * (total - done) / done / 1000
        val t = if (s >= 3600) "${s / 3600}h ${s % 3600 / 60}m" else "${s / 60}m ${s % 60}s"
        return "$pct% - about $t left"
    }
}

/** Offline 4x upscaler: runs assets/model.onnx tile by tile. */
class Upscaler(ctx: Context, private val scale: Int = 4) {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val size: Int          // model window (tile + 2*pad)
    private val pad = 8
    val tile get() = size - 2 * pad

    init {
        val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(4) }
        session = env.createSession(ctx.assets.open("model.onnx").readBytes(), opts)
        val s = (session.inputInfo.values.first().info as TensorInfo).shape
        size = if (s.size == 4 && s[2] > 0) s[2].toInt() else 128
    }

    fun run(src: Bitmap, onTile: (Int, Int) -> Unit = { _, _ -> }): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w * scale, h * scale, Bitmap.Config.ARGB_8888)
        val nx = (w + tile - 1) / tile; val ny = (h + tile - 1) / tile
        var done = 0
        val n = size * size
        for (ty in 0 until ny) for (tx in 0 until nx) {
            val x0 = tx * tile; val y0 = ty * tile
            val tw = minOf(tile, w - x0); val th = minOf(tile, h - y0)
            val f = FloatArray(3 * n)
            for (j in 0 until size) {
                val sy = (y0 - pad + j).coerceIn(0, h - 1)
                for (i in 0 until size) {
                    val c = px[sy * w + (x0 - pad + i).coerceIn(0, w - 1)]
                    val k = j * size + i
                    f[k] = (c shr 16 and 255) / 255f
                    f[n + k] = (c shr 8 and 255) / 255f
                    f[2 * n + k] = (c and 255) / 255f
                }
            }
            val t = OnnxTensor.createTensor(env, FloatBuffer.wrap(f), longArrayOf(1, 3, size.toLong(), size.toLong()))
            session.run(mapOf(session.inputNames.first() to t)).use { res ->
                val o = (res.get(0) as OnnxTensor).floatBuffer
                val ow = size * scale; val on = ow * ow
                val cw = tw * scale; val ch = th * scale
                val op = IntArray(cw * ch)
                for (y in 0 until ch) for (x in 0 until cw) {
                    val idx = (pad * scale + y) * ow + pad * scale + x
                    val r = (o.get(idx).coerceIn(0f, 1f) * 255).toInt()
                    val g = (o.get(on + idx).coerceIn(0f, 1f) * 255).toInt()
                    val b = (o.get(2 * on + idx).coerceIn(0f, 1f) * 255).toInt()
                    op[y * cw + x] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                }
                out.setPixels(op, 0, cw, x0 * scale, y0 * scale, cw, ch)
            }
            t.close()
            onTile(++done, nx * ny)
        }
        return out
    }
}
