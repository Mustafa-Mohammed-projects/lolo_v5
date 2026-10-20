package com.abololo.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLUtils
import java.util.Random
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class GameRenderer : GLSurfaceView.Renderer {
    private val gfx = Gfx()
    private val f = Frame()
    private var aspect = 0.5f
    private var ready = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        gfx.init()
        makeTextures()
        GLES20.glClearColor(Gfx.FOG_R, Gfx.FOG_G, Gfx.FOG_B, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        ready = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 0.5f
    }

    private fun upload(bm: Bitmap, repeat: Boolean): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val w = if (repeat) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, w)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, w)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bm, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bm.recycle()
        return ids[0]
    }

    private fun makeTextures() {
        // detail noise (power of two)
        val rnd = Random(7)
        val n = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) for (x in 0 until 64) {
            val v = 120 + rnd.nextInt(110)
            n.setPixel(x, y, Color.rgb(v, v, v))
        }
        gfx.detailTex = upload(n, true)
        // sand-plaster wall texture
        val wl = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val wc = Canvas(wl)
        wc.drawColor(Color.rgb(240, 240, 240))
        val wp = Paint(Paint.ANTI_ALIAS_FLAG)
        val wr2 = Random(31)
        for (i in 0 until 70) {
            val v = 205 + wr2.nextInt(50)
            wp.color = Color.argb(70, v - 40, v - 40, v - 40)
            val cx = wr2.nextInt(128).toFloat(); val cy = wr2.nextInt(128).toFloat(); val r = 4f + wr2.nextInt(16)
            for (dx in -128..128 step 128) for (dy in -128..128 step 128) wc.drawCircle(cx + dx, cy + dy, r, wp)
        }
        wp.color = Color.argb(110, 90, 80, 70); wp.strokeWidth = 1f
        for (i in 0 until 9) {
            var x0 = wr2.nextInt(128).toFloat(); var y0 = wr2.nextInt(128).toFloat()
            for (sg in 0 until 5) { val x1 = x0 + wr2.nextInt(15) - 7; val y1 = y0 + 4 + wr2.nextInt(8); wc.drawLine(x0, y0, x1, y1, wp); x0 = x1; y0 = y1 }
        }
        for (i in 0 until 1200) { val x = wr2.nextInt(128); val y = wr2.nextInt(128); wl.setPixel(x, y, Models3D.mix(wl.getPixel(x, y), Color.BLACK, 0.12f)) }
        gfx.sandTex = upload(wl, true)
        for (i in 0 until Looks.ALL.size) {
            if (i >= 32) break
            gfx.faceTex[i] = upload(faceBitmap(Looks.ALL[i]), false)
            gfx.camoTex[i] = upload(camoBitmap(Looks.ALL[i], i), true)
        }
    }

    private fun camoBitmap(lk: Look, seed: Int): Bitmap {
        val bm = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val base = if (lk.suit) lk.cloth else lk.cloth
        c.drawColor(base)
        val rnd = Random(100L + seed)
        val cols = intArrayOf(lk.clothD, if (lk.camo != 0) lk.camo else Models3D.mix(lk.cloth, Color.WHITE, 0.12f), Models3D.mix(lk.cloth, Color.BLACK, 0.3f))
        val blots = if (lk.suit) 0 else 26
        for (i in 0 until blots) {
            p.color = cols[rnd.nextInt(cols.size)]
            val x = rnd.nextInt(64).toFloat()
            val y = rnd.nextInt(64).toFloat()
            val r = 4f + rnd.nextInt(8)
            for (dx in -64..64 step 64) for (dy in -64..64 step 64) c.drawOval(x + dx - r, y + dy - r * 0.7f, x + dx + r, y + dy + r * 0.7f, p)
        }
        // cloth weave speckle
        for (i in 0 until 260) {
            val x = rnd.nextInt(64); val y = rnd.nextInt(64)
            val col = bm.getPixel(x, y)
            bm.setPixel(x, y, Models3D.mix(col, if (rnd.nextBoolean()) Color.BLACK else Color.WHITE, 0.10f))
        }
        return bm
    }

    /** painted face: lon -62..62 across, lat 48 (top) .. -52 (bottom) down. */
    private fun faceBitmap(lk: Look): Bitmap {
        val w = 128
        val bm = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(lk.skin)
        // soft light on cheeks / forehead, shade at the sides
        p.shader = RadialGradient(64f, 58f, 70f, intArrayOf(lk.skinHi, lk.skin, Models3D.mix(lk.skin, Color.BLACK, 0.35f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), w.toFloat(), p)
        p.shader = null
        val dark = Models3D.mix(lk.skin, Color.BLACK, 0.5f)
        val redEyes = Color.red(lk.eye) > 200 && Color.green(lk.eye) < 80
        // eyes at u=.5+-.16, v=.40
        for (s in intArrayOf(-1, 1)) {
            val ex = 64f + s * 21f
            val ey = 51f
            // socket shadow
            p.color = Models3D.mix(lk.skin, Color.BLACK, 0.28f); p.alpha = 150
            c.drawOval(ex - 12f, ey - 7f, ex + 12f, ey + 7f, p)
            p.alpha = 255
            p.color = if (redEyes) Color.rgb(255, 90, 80) else Color.rgb(246, 244, 238)
            c.drawOval(ex - 9.5f, ey - 4.2f, ex + 9.5f, ey + 4.2f, p)
            p.color = if (redEyes) Color.rgb(200, 0, 0) else lk.eye
            c.drawCircle(ex, ey, 3.9f, p)
            p.color = if (redEyes) Color.rgb(50, 0, 0) else Color.rgb(10, 8, 8)
            c.drawCircle(ex, ey, 2.0f, p)
            p.color = Color.WHITE
            c.drawCircle(ex - 1.3f, ey - 1.4f, 1.0f, p)
            // lids and brows
            p.color = dark; p.strokeWidth = 1.6f; p.style = Paint.Style.STROKE
            c.drawArc(ex - 10f, ey - 6f, ex + 10f, ey + 6f, 190f, 160f, false, p)
            p.style = Paint.Style.FILL
            p.color = lk.hair
            val bx = ex - 11f
            c.drawRoundRect(bx, 38f + (if (s < 0) 0f else 0f), bx + 22f, 42.5f, 2f, 2f, p)
        }
        // nose shading
        p.color = Models3D.mix(lk.skin, Color.BLACK, 0.3f); p.alpha = 120
        c.drawOval(54f, 66f, 62f, 78f, p); c.drawOval(66f, 66f, 74f, 78f, p)
        p.alpha = 255
        p.color = dark
        c.drawOval(56.5f, 75f, 60.5f, 78f, p); c.drawOval(67.5f, 75f, 71.5f, 78f, p)
        // mouth
        p.color = Models3D.mix(lk.skin, Color.rgb(150, 40, 40), 0.45f)
        c.drawRoundRect(52f, 92f, 76f, 97f, 3f, 3f, p)
        p.color = Models3D.mix(lk.skin, Color.BLACK, 0.55f)
        c.drawRect(53f, 94.2f, 75f, 95.2f, p)
        // facial hair
        p.color = lk.hair
        if (lk.beard == 1) {
            p.alpha = 235
            c.drawRoundRect(46f, 83f, 82f, 91f, 5f, 5f, p)
            p.alpha = 90
            c.drawRect(30f, 86f, 98f, 128f, p)
            p.alpha = 255
        } else if (lk.beard == 2) {
            c.drawRect(0f, 80f, 128f, 128f, p)
            c.drawOval(-10f, 58f, 22f, 128f, p); c.drawOval(106f, 58f, 138f, 128f, p)
            // mouth opening through the beard
            p.color = Models3D.mix(lk.skin, Color.rgb(150, 40, 40), 0.45f)
            c.drawRoundRect(54f, 95f, 74f, 99f, 3f, 3f, p)
            p.color = lk.hair; p.alpha = 255
            c.drawRoundRect(44f, 83f, 84f, 93f, 6f, 6f, p)
        }
        return bm
    }

    private fun hash(a: Int, b: Int): Int {
        var h = a * 73856093 xor b * 19349663
        h = h xor (h ushr 13)
        h *= 1274126177
        return h xor (h ushr 16)
    }

    private fun frac(h: Int, sh: Int): Float = ((h ushr sh) and 255) / 255f

    override fun onDrawFrame(gl: GL10?) {
        if (!ready) return
        FrameBus.fetch(f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (!f.has) return
        gfx.cam.set(f.ex, f.ey, f.ez, f.cx, f.cy, f.cz, f.fov, aspect)
        gfx.begin()
        GLES20.glDepthMask(false)
        gfx.sky(f.ex, f.ey, f.ez)
        GLES20.glDepthMask(true)
        drawGround()
        drawDecor()
        // shadows (blob under each entity)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glDepthMask(false)
        gfx.unlit(true)
        gfx.colorF(0f, 0f, 0f, 0.32f)
        for (i in 0 until f.n) {
            val b = i * Frame.REC
            val k = f.d[b].toInt()
            if (k == Frame.K_SOLDIER) {
                if (f.d[b + 11] > 0.5f) continue
                gfx.cylinder(f.d[b + 1], 0.15f, f.d[b + 3], 7f, 0.1f)
            } else if (k == Frame.K_VEHICLE) {
                gfx.cylinder(f.d[b + 1], 0.15f, f.d[b + 3], 28f, 0.1f)
            } else if (k == Frame.K_PLANE) {
                gfx.cylinder(f.d[b + 1], 0.15f, f.d[b + 3], 26f, 0.1f)
            }
        }
        gfx.unlit(false)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
        // opaque entities
        for (i in 0 until f.n) {
            val b = i * Frame.REC
            val d = f.d
            when (d[b].toInt()) {
                Frame.K_SOLDIER -> {
                    val li = d[b + 5].toInt().coerceIn(0, Looks.ALL.size - 1)
                    Models3D.soldier(
                        gfx, Looks.ALL[li], d[b + 1], d[b + 2], d[b + 3], d[b + 4],
                        d[b + 6], d[b + 7] > 0.5f, d[b + 8].toInt(), d[b + 9], d[b + 10], d[b + 11], d[b + 12], d[b + 13],
                        d[b + 14], d[b + 15]
                    )
                }
                Frame.K_VEHICLE -> Models3D.vehicle(
                    gfx, d[b + 5].toInt(), d[b + 1], d[b + 2], d[b + 3], d[b + 4], d[b + 7],
                    d[b + 6], d[b + 8], d[b + 9], d[b + 10] > 0.5f, d[b + 11]
                )
                Frame.K_PLANE -> Models3D.plane(gfx, d[b + 1], d[b + 2], d[b + 3], d[b + 4], d[b + 5], f.t)
                Frame.K_BUILDING -> Models3D.building(
                    gfx, d[b + 5].toInt(), d[b + 1], d[b + 3], d[b + 6], d[b + 7], d[b + 8], d[b + 9], d[b + 10], d[b + 11] > 0.5f, f.t
                )
                Frame.K_COVER -> Models3D.cover(gfx, d[b + 5].toInt(), d[b + 1], d[b + 3], d[b + 6], d[b + 7], d[b + 8])
                Frame.K_PROP -> {
                    // d[b+5] kind: 0 tree
                    Models3D.tree(gfx, d[b + 1], d[b + 3], d[b + 6])
                }
                else -> {}
            }
        }
        // tracers and effects
        gfx.unlit(true)
        for (i in 0 until f.n) {
            val b = i * Frame.REC
            val d = f.d
            if (d[b].toInt() == Frame.K_BULLET) {
                val kind = d[b + 5].toInt()
                gfx.push()
                gfx.translate(d[b + 1], d[b + 2], d[b + 3])
                gfx.rotY(Models3D.yawDeg(d[b + 4]))
                if (kind == 2) {
                    gfx.color(Color.rgb(90, 90, 80))
                    gfx.box(0f, 0f, 0f, 2.6f, 2.6f, 16f)
                    gfx.color(Color.rgb(255, 150, 60))
                    gfx.sphere(0f, 0f, -9f, 2.4f, 2.4f, 5f)
                } else {
                    if (kind == 1) gfx.color(Color.rgb(255, 190, 80)) else if (d[b + 7] > 0.5f) gfx.color(Color.rgb(255, 110, 90)) else gfx.color(Color.rgb(255, 240, 170))
                    val w = if (kind == 1) 1.5f else 0.9f
                    gfx.box(0f, 0f, 0f, w, w, d[b + 6])
                }
                gfx.pop()
            }
        }
        gfx.unlit(false)
        // transparent effects
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glDepthMask(false)
        gfx.unlit(true)
        for (i in 0 until f.n) {
            val b = i * Frame.REC
            val d = f.d
            when (d[b].toInt()) {
                Frame.K_BOOM -> {
                    val r = d[b + 5]
                    val pr = d[b + 6]
                    gfx.colorF(1f, 0.55f, 0.15f, 0.85f * (1f - pr))
                    val rr = r * (0.4f + pr * 0.9f)
                    gfx.sphere(d[b + 1], d[b + 2] + rr * 0.5f, d[b + 3], rr, rr * 0.85f, rr)
                    gfx.colorF(1f, 0.95f, 0.6f, max(0f, 1f - pr * 1.7f))
                    gfx.sphere(d[b + 1], d[b + 2] + rr * 0.5f, d[b + 3], rr * 0.55f, rr * 0.5f, rr * 0.55f)
                }
                Frame.K_PART -> {
                    gfx.colorF(d[b + 7], d[b + 8], d[b + 9], d[b + 10])
                    val s = d[b + 6]
                    if (d[b + 5].toInt() == 3) gfx.box(d[b + 1], d[b + 2], d[b + 3], s, s, s) else gfx.sphere(d[b + 1], d[b + 2], d[b + 3], s, s, s)
                }
                Frame.K_SHELL -> {
                    val r = d[b + 5]
                    val pulse = 0.5f + 0.5f * sin(f.t * 12f)
                    if (d[b + 6] > 0.5f) gfx.colorF(1f, 0.28f, 0.22f, 0.35f + 0.3f * pulse) else gfx.colorF(1f, 0.9f, 0.3f, 0.35f + 0.3f * pulse)
                    gfx.cylinder(d[b + 1], 0.3f, d[b + 3], r, 0.3f)
                    // the falling shell
                    val z = d[b + 7]
                    if (z > 0f) {
                        gfx.colorF(0.15f, 0.15f, 0.16f, 1f)
                        gfx.box(d[b + 1], z, d[b + 3], 3f, 9f, 3f)
                    }
                }
                Frame.K_RING -> {
                    gfx.colorF(0.45f, 1f, 0.55f, 0.55f)
                    gfx.cylinder(d[b + 1], 0.35f, d[b + 3], d[b + 5], 0.2f)
                    gfx.colorF(0f, 0f, 0f, 0.5f)
                    gfx.cylinder(d[b + 1], 0.25f, d[b + 3], d[b + 5] - 1.6f, 0.2f)
                }
                else -> {}
            }
        }
        gfx.unlit(false)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawGround() {
        val w = f.worldW
        val dd = f.worldD
        gfx.color(Color.rgb(94, 78, 62))
        gfx.box(w / 2f, -4f, dd / 2f, w + 6000f, 2f, dd + 6000f)
        gfx.tex(gfx.sandTex, w / 140f, dd / 140f)
        gfx.color(Color.rgb(226, 196, 146))
        gfx.box(w / 2f, -1f, dd / 2f, w, 2f, dd)
        gfx.noTex()
        // darker packed-earth patches
        gfx.color(Color.rgb(196, 166, 118))
        val cell = 150
        val cx0 = max(0, floor((f.cx - 900f) / cell).toInt())
        val cx1 = min((w / cell).toInt(), floor((f.cx + 900f) / cell).toInt())
        val cz0 = max(0, floor((f.cz - 1200f) / cell).toInt())
        val cz1 = min((dd / cell).toInt(), floor((f.cz + 1200f) / cell).toInt())
        for (ix in cx0..cx1) {
            for (iz in cz0..cz1) {
                val h = hash(ix, iz)
                if ((h and 3) == 0) {
                    val px = ix * cell + frac(h, 4) * cell
                    val pz = iz * cell + frac(h, 12) * cell
                    val sx = 50f + frac(h, 20) * 70f
                    val sz = 40f + frac(h, 6) * 60f
                    if (px > 0f && px < w && pz > 0f && pz < dd) gfx.cylinder(px, 0.1f, pz, sx * 0.5f, 0.3f)
                }
            }
        }
        // mountain wall of rocks around the battlefield
        val step = 110
        var i = -2
        while (i * step < w + 300) {
            val h = hash(i, 1)
            val px = i * step.toFloat()
            val s = 50f + frac(h, 3) * 60f
            if (abs(px - f.cx) < 1400f) {
                if (abs(0f - f.cz) < 1800f || true) {
                    Models3D.rock(gfx, px, -40f - frac(h, 9) * 30f, s, (h and 255))
                    Models3D.rock(gfx, px, dd + 40f + frac(h, 11) * 30f, s, ((h shr 5) and 255))
                }
            }
            i++
        }
        var j = -2
        while (j * step < dd + 300) {
            val h = hash(2, j)
            val pz = j * step.toFloat()
            val s = 50f + frac(h, 3) * 60f
            if (abs(pz - f.cz) < 1800f) {
                Models3D.rock(gfx, -50f - frac(h, 9) * 30f, pz, s, (h and 255))
                Models3D.rock(gfx, w + 50f + frac(h, 11) * 30f, pz, s, ((h shr 5) and 255))
            }
            j++
        }
    }

    private fun drawDecor() {
        val cell = 70
        val cx0 = max(0, floor((f.cx - 500f) / cell).toInt())
        val cx1 = min((f.worldW / cell).toInt(), floor((f.cx + 500f) / cell).toInt())
        val cz0 = max(0, floor((f.cz - 700f) / cell).toInt())
        val cz1 = min((f.worldD / cell).toInt(), floor((f.cz + 700f) / cell).toInt())
        for (ix in cx0..cx1) {
            for (iz in cz0..cz1) {
                val h = hash(ix + 100, iz + 200)
                val m = h and 7
                val px = ix * cell + frac(h, 4) * cell
                val pz = iz * cell + frac(h, 12) * cell
                if (m == 0) Models3D.rock(gfx, px, pz, 3f + frac(h, 20) * 4f, (h shr 8) and 255)
                else if (m == 1 || m == 2) Models3D.tuft(gfx, px, pz, 4f + frac(h, 20) * 5f)
            }
        }
    }
}
