package com.abololo.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

class GameView(
    context: Context,
    private val sfx: Sfx,
    private val onQuit: () -> Unit
) : View(context), Choreographer.FrameCallback {

    companion object {
        const val S_MENU = 0
        const val S_SEL = 1
        const val S_BRIEF = 2
        const val S_PLAY = 3
        const val S_PAUSE = 4
        const val S_WIN = 5
        const val S_OVER = 6
        const val S_END = 7

        const val B_PLAY = 1
        const val B_MISS = 2
        const val B_SOUND = 3
        const val B_LANG = 4
        const val B_BACK = 5
        const val B_PAUSE = 6
        const val B_RESUME = 7
        const val B_RESTART = 8
        const val B_MENU = 9
        const val B_NEXT = 10
        const val B_START = 11
        const val B_CUT = 12
        const val B_DANCE = 13
        const val B_M0 = 100
        const val B_W0 = 300

        const val PI_F = 3.14159265f
        const val BEAT = World.BEAT
    }

    private class Hit(val id: Int, val l: Float, val t: Float, val r: Float, val b: Float)

    private val prefs = context.getSharedPreferences("abololo", Context.MODE_PRIVATE)
    private val world = World(sfx)
    private val hits = ArrayList<Hit>()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()
    private val pth = Path()
    private val srcR = Rect()
    private val frame = Frame()
    private val cam = Cam()
    private val res = FloatArray(2)
    private val confetti = ArrayList<FloatArray>()
    private val confCols = intArrayOf(Color.rgb(255, 210, 60), Color.rgb(230, 70, 70), Color.rgb(80, 200, 120), Color.rgb(90, 160, 255))

    private var bmQubba: Bitmap? = null
    private var bmSavanna: Bitmap? = null

    // layout
    private var sc = 1f
    private var offX = 0f
    private var wh = 800f
    private var pw = 1080f
    private var ph = 1920f

    // state
    private var state = S_MENU
    private var pendingState = -1
    private var fade = 1f
    private var mission = 1
    private var unlocked = 1
    private var menuT = 0f
    private var running = false
    private var last = 0L
    private var curCut = 0
    private var cutIdx = 0
    private var cutTime = 0f
    private var wmenu = false

    // camera
    private var fx = 450f
    private var fz = 1800f
    private var camInit = true
    private var camPitch = 56f
    private var camDist = 330f

    // input
    private var joyId = -1
    private var joyOx = 0f
    private var joyOy = 0f
    private var joyKx = 0f
    private var joyKy = 0f
    private var joyVX = 0f
    private var joyVY = 0f
    private var aimId = -1
    private var aimLx = 0f
    private var aimLy = 0f
    private var fireId = -1
    private var breathId = -1
    private var fireLx = 0f
    private var fireLy = 0f
    private var fireHeld = false
    private var btnId = -1
    private var pressedId = -1
    private var pressedPtr = -1

    init {
        unlocked = prefs.getInt("unlocked", 1).coerceIn(1, 5)
        L.ar = prefs.getBoolean("ar", true)
        sfx.enabled = prefs.getBoolean("sound", true)
        mission = unlocked
        try {
            bmQubba = BitmapFactory.decodeResource(context.resources, R.drawable.portrait_qubba)
            bmSavanna = BitmapFactory.decodeResource(context.resources, R.drawable.portrait_savanna)
        } catch (e: Exception) {
        }
    }

    // ------------------------------------------------------------------ lifecycle
    fun resumeLoop() {
        running = true
        last = 0L
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun pauseLoop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        if (state == S_PLAY && !world.waitCut && !world.dancing) state = S_PAUSE
        clearInput()
    }

    fun handleBack(): Boolean {
        when (state) {
            S_PLAY -> {
                if (wmenu) wmenu = false
                else if (!world.waitCut && !world.dancing) {
                    state = S_PAUSE
                    clearInput()
                }
            }
            S_PAUSE -> state = S_PLAY
            S_BRIEF -> go(S_SEL)
            S_SEL, S_WIN, S_OVER, S_END -> go(S_MENU)
            else -> return false
        }
        return true
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (last == 0L) last = frameTimeNanos
        var dt = (frameTimeNanos - last) / 1_000_000_000f
        last = frameTimeNanos
        if (dt > 0.05f) dt = 0.05f
        if (dt < 0f) dt = 0f
        update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        pw = w.toFloat()
        ph = h.toFloat()
        sc = min(w / 540f, h / 800f)
        val ww = w / sc
        offX = (ww - 540f) / 2f
        wh = h / sc
    }

    private fun clearInput() {
        joyId = -1
        joyVX = 0f
        joyVY = 0f
        aimId = -1
        fireId = -1
        fireHeld = false
        btnId = -1
        breathId = -1
        world.holdBreath = false
        pressedId = -1
        pressedPtr = -1
    }

    // ------------------------------------------------------------------ states
    private fun go(s: Int) {
        if (pendingState != -1) return
        pendingState = s
    }

    private fun applyState(s: Int) {
        clearInput()
        wmenu = false
        when (s) {
            S_PLAY -> {
                sfx.stopMusic()
                world.loadMission(mission)
                camInit = true
                curCut = 0
                camPitch = 56f
                state = S_PLAY
            }
            S_WIN, S_OVER -> {
                sfx.stopMusic()
                state = s
            }
            S_MENU, S_SEL, S_BRIEF -> {
                sfx.stopMusic()
                FrameBus.hide()
                state = s
            }
            S_END -> {
                sfx.stopMusic()
                sfx.startMusic(Sfx.MUSIC_END)
                FrameBus.hide()
                menuT = 0f
                state = S_END
            }
            else -> state = s
        }
    }

    private fun saveProgress() {
        prefs.edit().putInt("unlocked", unlocked).apply()
    }

    private fun onButton(id: Int) {
        sfx.play(Sfx.CLICK, 0.7f)
        when {
            id >= B_W0 -> {
                world.selectWeapon(id - B_W0)
                wmenu = false
            }
            id == B_PLAY -> {
                mission = unlocked
                go(S_BRIEF)
            }
            id == B_MISS -> go(S_SEL)
            id == B_START -> go(S_PLAY)
            id == B_SOUND -> {
                sfx.enabled = !sfx.enabled
                prefs.edit().putBoolean("sound", sfx.enabled).apply()
            }
            id == B_LANG -> {
                L.ar = !L.ar
                prefs.edit().putBoolean("ar", L.ar).apply()
            }
            id == B_BACK -> go(if (state == S_BRIEF) S_SEL else S_MENU)
            id == B_PAUSE -> {
                if (state == S_PLAY && !world.waitCut && !world.dancing) {
                    state = S_PAUSE
                    clearInput()
                }
            }
            id == B_RESUME -> state = S_PLAY
            id == B_RESTART -> go(S_PLAY)
            id == B_MENU -> go(S_MENU)
            id == B_NEXT -> {
                mission = min(5, mission + 1)
                go(S_BRIEF)
            }
            id == B_CUT -> advanceCut()
            id == B_DANCE -> world.finishDance()
            id >= B_M0 -> {
                val n = id - B_M0
                if (n in 1..unlocked) {
                    mission = n
                    go(S_BRIEF)
                }
            }
        }
    }

    private fun advanceCut() {
        if (cutTime < 0.25f) return
        val lines = Story.cut(curCut)
        cutIdx++
        cutTime = 0f
        if (cutIdx >= lines.size) {
            curCut = 0
            world.cutDone()
        }
    }

    // ------------------------------------------------------------------ input
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val action = e.actionMasked
        val idx = e.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                touchDown(e.getPointerId(idx), e.getX(idx) / sc - offX, e.getY(idx) / sc)
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) touchMove(e.getPointerId(i), e.getX(i) / sc - offX, e.getY(i) / sc)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP ->
                touchUp(e.getPointerId(idx), e.getX(idx) / sc - offX, e.getY(idx) / sc, false)
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until e.pointerCount) touchUp(e.getPointerId(i), e.getX(i) / sc - offX, e.getY(i) / sc, true)
            }
        }
        return true
    }

    private fun hitAt(x: Float, y: Float): Hit? {
        var i = hits.size - 1
        while (i >= 0) {
            val h = hits[i]
            if (x >= h.l && x <= h.r && y >= h.t && y <= h.b) return h
            i--
        }
        return null
    }

    private fun rEdge() = 540f + offX
    private fun fireX() = rEdge() - 80f
    private fun fireY() = wh - 130f
    private fun reloadX() = rEdge() - 180f
    private fun reloadY() = wh - 84f
    private fun scopeX() = rEdge() - 186f
    private fun scopeY() = wh - 170f
    private fun crouchX() = rEdge() - 254f
    private fun crouchY() = wh - 128f
    private fun breathX() = rEdge() - 250f
    private fun breathY() = wh - 214f
    private fun wpnX() = rEdge() - 56f
    private fun wpnY() = wh - 250f
    private fun supX() = rEdge() - 56f
    private fun supY() = wh - 330f

    private fun rotateView(dx: Float, dy: Float) {
        val k = 0.0068f / (1f + (world.zoom() - 1f) * 0.7f)
        world.camYaw += dx * k
        camPitch = (camPitch - dy * 0.12f).coerceIn(40f, 70f)
    }

    private fun touchDown(ptr: Int, x: Float, y: Float) {
        if (pendingState != -1) return
        val h = hitAt(x, y)
        if (h != null) {
            if (pressedPtr == -1) {
                pressedId = h.id
                pressedPtr = ptr
            }
            return
        }
        if (state != S_PLAY || world.waitCut || world.dancing) return
        val inVeh = world.pveh != null
        if (fireId == -1 && hypot(x - fireX(), y - fireY()) < 64f) {
            fireId = ptr
            fireHeld = true
            fireLx = x
            fireLy = y
            return
        }
        if (btnId == -1) {
            if (!inVeh && hypot(x - reloadX(), y - reloadY()) < 38f) { btnId = ptr; world.startReload(); return }
            if (!inVeh && World.canScope(world.weapon) && hypot(x - scopeX(), y - scopeY()) < 38f) { btnId = ptr; world.toggleScope(); return }
            if (!inVeh && hypot(x - crouchX(), y - crouchY()) < 36f) { btnId = ptr; world.toggleCrouch(); return }
            if (!inVeh && world.scoped && breathId == -1 && hypot(x - breathX(), y - breathY()) < 38f) { breathId = ptr; world.holdBreath = true; return }
            if (!inVeh && hypot(x - wpnX(), y - wpnY()) < 40f) { btnId = ptr; wmenu = !wmenu; return }
            if (hypot(x - supX(), y - supY()) < 40f) { btnId = ptr; world.callSupport(); return }
        }
        if (wmenu) { wmenu = false; return }
        if (joyId == -1 && x < 250f - offX && y > wh * 0.4f) {
            joyId = ptr
            joyOx = x; joyOy = y; joyKx = x; joyKy = y
            joyVX = 0f; joyVY = 0f
            return
        }
        if (aimId == -1 && y > 80f) {
            aimId = ptr
            aimLx = x
            aimLy = y
        }
    }

    private fun touchMove(ptr: Int, x: Float, y: Float) {
        if (ptr == joyId) {
            var dx = x - joyOx
            var dy = y - joyOy
            val d = hypot(dx, dy)
            val rad = 56f
            if (d > rad) {
                dx = dx / d * rad
                dy = dy / d * rad
            }
            joyKx = joyOx + dx
            joyKy = joyOy + dy
            joyVX = dx / rad
            joyVY = dy / rad
        }
        if (ptr == aimId) {
            rotateView(x - aimLx, y - aimLy)
            aimLx = x
            aimLy = y
        }
        if (ptr == fireId) {
            rotateView(x - fireLx, y - fireLy)
            fireLx = x
            fireLy = y
        }
    }

    private fun touchUp(ptr: Int, x: Float, y: Float, cancel: Boolean) {
        if (ptr == joyId) { joyId = -1; joyVX = 0f; joyVY = 0f }
        if (ptr == aimId) aimId = -1
        if (ptr == fireId) { fireId = -1; fireHeld = false }
        if (ptr == btnId) btnId = -1
        if (ptr == breathId) { breathId = -1; world.holdBreath = false }
        if (ptr == pressedPtr) {
            val id = pressedId
            pressedId = -1
            pressedPtr = -1
            if (!cancel && pendingState == -1) {
                val h = hitAt(x, y)
                if (h != null && h.id == id) onButton(id)
            }
        }
    }

    // ------------------------------------------------------------------ update
    private fun update(dt: Float) {
        menuT += dt
        if (pendingState != -1) {
            fade += dt * 4.5f
            if (fade >= 1f) {
                fade = 1f
                val s = pendingState
                pendingState = -1
                applyState(s)
            }
        } else if (fade > 0f) {
            fade = max(0f, fade - dt * 3.5f)
        }
        if (state != S_PLAY) return
        cutTime += dt
        if (world.waitCut) {
            if (world.cutId != curCut) {
                curCut = world.cutId
                cutIdx = 0
                cutTime = 0f
            }
        }
        world.moveX = joyVX
        world.moveY = joyVY
        world.fireHeld = fireHeld
        world.update(dt)
        buildFrame(dt)
        if (world.dancing) {
            while (confetti.size < 140) {
                confetti.add(floatArrayOf(Random.nextFloat() * 540f, -10f - Random.nextFloat() * 200f, Random.nextFloat() * 40f - 20f, 60f + Random.nextFloat() * 90f, Random.nextInt(4).toFloat()))
            }
            var i = 0
            while (i < confetti.size) {
                val c = confetti[i]
                c[0] += c[2] * dt
                c[1] += c[3] * dt
                if (c[1] > wh + 10f) { c[1] = -10f; c[0] = Random.nextFloat() * 540f }
                i++
            }
        }
        if (pendingState != -1) return
        if (world.result == 1) {
            if (mission >= unlocked && mission < 5) {
                unlocked = mission + 1
                saveProgress()
            }
            sfx.play(Sfx.WIN, 1f)
            go(if (mission >= 5) S_END else S_WIN)
        } else if (world.result == 2) {
            sfx.play(Sfx.LOSE, 1f)
            go(S_OVER)
        }
    }

    // ------------------------------------------------------------------ camera + frame for the OpenGL thread
    private fun buildFrame(dt: Float) {
        val f = frame
        f.clear()
        f.has = true
        f.t = world.t
        f.worldW = world.W
        f.worldD = world.D
        val v = world.pveh
        val pl = world.player
        var tx = world.W / 2f
        var tz = world.D / 2f
        if (v != null && !v.dead) { tx = v.x; tz = v.y }
        else if (pl != null) { tx = pl.x; tz = pl.y }
        if (world.dancing) {
            buildDance(f)
            return
        }
        if (camInit) { fx = tx; fz = tz; camInit = false } else {
            val k = min(1f, dt * 9f)
            fx += (tx - fx) * k
            fz += (tz - fz) * k
        }
        val yaw = world.camYaw
        val dirx = cos(yaw)
        val dirz = sin(yaw)
        val pitchRad = camPitch * PI_F / 180f
        val scoped = world.scoped && v == null
        val zm = if (v == null) world.zoom() else 1f
        val dist = if (scoped) 380f else if (v != null) 430f else camDist
        val fov0 = if (v == null && world.sprinting) 59f else 55f
        val fov = if (scoped) (2f * atan(tan(fov0 * 0.5f * PI_F / 180f) / zm) * 180f / PI_F) else fov0
        val hd = dist * cos(pitchRad)
        var ex = fx - dirx * hd
        var ey = dist * sin(pitchRad) + 6f
        var ez = fz - dirz * hd
        val ahead = if (scoped) 220f else if (v != null) 70f else 110f
        var cx = fx + dirx * ahead
        var cy = 10f
        var cz = fz + dirz * ahead
        if (world.shake > 0f) {
            val s = world.shake * 7f
            val ox = (Random.nextFloat() - 0.5f) * s
            val oy = (Random.nextFloat() - 0.5f) * s
            ex += ox; cx += ox; ey += oy; cy += oy
        }
        f.ex = ex; f.ey = ey; f.ez = ez; f.cx = cx; f.cy = cy; f.cz = cz; f.fov = fov
        cam.set(ex, ey, ez, cx, cy, cz, fov, pw / ph)

        for (b in world.buildings) {
            val i = f.begin(Frame.K_BUILDING, b.x, 0f, b.y, 0f)
            if (i < 0) break
            f.d[i + 5] = b.type.toFloat()
            f.d[i + 6] = b.w
            f.d[i + 7] = b.d
            f.d[i + 8] = b.h
            f.d[i + 9] = if (b.side == 1) b.hp / b.maxHp else 1f
            f.d[i + 10] = b.flash
            f.d[i + 11] = if (b.dead && b.side == 1) 1f else 0f
        }
        for (c in world.covers) {
            val i = f.begin(Frame.K_COVER, c.x, 0f, c.y, 0f)
            if (i < 0) break
            f.d[i + 5] = c.kind.toFloat()
            f.d[i + 6] = c.w
            f.d[i + 7] = c.d
            f.d[i + 8] = c.h
        }
        for (s in world.soldiers) {
            if (s.inVeh) continue
            val i = f.begin(Frame.K_SOLDIER, s.x, 0f, s.y, atan2(s.aimY, s.aimX))
            if (i < 0) break
            f.d[i + 5] = Looks.id(s.look).toFloat()
            f.d[i + 6] = s.phase
            f.d[i + 7] = if (s.moving) 1f else 0f
            val gun = if (s.isPlayer) world.weapon else if (!s.armed) -1 else s.weapon.coerceIn(0, 11)
            f.d[i + 8] = gun.toFloat()
            f.d[i + 9] = s.muzzle
            f.d[i + 10] = max(0f, s.flash)
            f.d[i + 11] = if (s.dead) min(1f, (6f - s.deadT) / 0.35f) else 0f
            f.d[i + 12] = -1f
            f.d[i + 14] = if (s.isPlayer) world.crouchT else 0f
            f.d[i + 15] = if (s.isPlayer) world.kick else 0f
        }
        for (vh in world.vehicles) {
            val i = f.begin(Frame.K_VEHICLE, vh.x, 0f, vh.y, vh.yaw)
            if (i < 0) break
            f.d[i + 5] = vh.type.toFloat()
            f.d[i + 6] = vh.wheel
            f.d[i + 7] = vh.turret
            f.d[i + 8] = vh.muzzle
            f.d[i + 9] = max(0f, vh.flash)
            f.d[i + 10] = if (vh.dead) 1f else 0f
            f.d[i + 11] = vh.hp / vh.maxHp
        }
        for (a in world.planes) {
            val i = f.begin(Frame.K_PLANE, a.x, 115f, a.y, if (a.vx > 0f) 0f else PI_F)
            if (i < 0) break
            f.d[i + 5] = max(0f, a.flash)
        }
        if (pl != null && !pl.dead) {
            val px = if (v != null) v.x else pl.x
            val pz = if (v != null) v.y else pl.y
            val i = f.begin(Frame.K_RING, px, 0f, pz, 0f)
            if (i >= 0) f.d[i + 5] = if (v != null) 30f else 13f
        }
        for (sh in world.shells) {
            val i = f.begin(Frame.K_SHELL, sh.x, 0f, sh.y, 0f)
            if (i < 0) break
            f.d[i + 5] = sh.r
            f.d[i + 6] = sh.srcSide.toFloat()
            f.d[i + 7] = if (sh.t < 1.4f) sh.t * 280f else 0f
        }
        for (b in world.bullets) {
            val i = f.begin(Frame.K_BULLET, b.x, 16f, b.y, atan2(b.vy, b.vx))
            if (i < 0) break
            f.d[i + 5] = b.kind.toFloat()
            f.d[i + 6] = if (b.kind == 0) 14f else if (b.kind == 1) 22f else 20f
            f.d[i + 7] = World.sideOf(b.team).toFloat()
        }
        for (bm in world.booms) {
            val i = f.begin(Frame.K_BOOM, bm.x, 0f, bm.y, 0f)
            if (i < 0) break
            f.d[i + 5] = bm.r * 0.9f
            f.d[i + 6] = bm.t / 0.6f
        }
        var pc = 0
        for (q in world.parts) {
            if (pc >= 220) break
            val fl = q.life / q.maxLife
            val i = f.begin(Frame.K_PART, q.x, q.z, q.y, 0f)
            if (i < 0) break
            pc++
            f.d[i + 5] = q.kind.toFloat()
            f.d[i + 6] = q.size * (if (q.kind == 1) (1.8f - fl) * 0.9f else 0.55f)
            f.d[i + 7] = Color.red(q.color) / 255f
            f.d[i + 8] = Color.green(q.color) / 255f
            f.d[i + 9] = Color.blue(q.color) / 255f
            f.d[i + 10] = if (q.kind == 1) 0.6f * fl else min(1f, fl * 2f)
        }
        FrameBus.publish(f)
    }

    private fun buildDance(f: Frame) {
        val cxw = 300f
        val czw = 200f
        f.worldW = 600f
        f.worldD = 400f
        f.ex = cxw
        f.ey = 70f
        f.ez = czw + 190f
        f.cx = cxw
        f.cy = 28f
        f.cz = czw
        f.fov = 52f
        cam.set(f.ex, f.ey, f.ez, f.cx, f.cy, f.cz, f.fov, pw / ph)
        val m = world.mission
        val st = world.sceneT
        val beat = st / BEAT * PI_F
        val sn = sin(beat)
        if (m == 4) {
            val ids = intArrayOf(9, 5, 10, 4, 1, 5)
            for (n in ids.indices) {
                val x = cxw + (n - 2.5f) * 40f
                val z = czw + (n % 2) * 26f
                val i = f.begin(Frame.K_SOLDIER, x, 0f, z, PI_F / 2f)
                if (i < 0) break
                val alt = if (n % 2 == 0) sn else -sn
                f.d[i + 5] = ids[n].toFloat()
                f.d[i + 8] = -1f
                f.d[i + 12] = 0.5f + 0.5f * alt
                f.d[i + 13] = abs(alt) * 4f
            }
        } else {
            val tt = World.TREATY_T
            val k = ((st - tt) / 1.5f).coerceIn(0f, 1f)
            val sm = k * k * (3f - 2f * k)
            val ids = intArrayOf(9, 11, 10, 0, 0, 6, 6, 7)
            val tx = floatArrayOf(262f, 300f, 338f, 250f, 276f, 324f, 350f, 300f)
            val tz = floatArrayOf(168f, 168f, 168f, 140f, 140f, 140f, 140f, 126f)
            for (n in ids.indices) {
                val dx = cxw + (n - 3.5f) * 34f
                val dz = czw + 10f + (n % 2) * 26f
                val x = tx[n] + (dx - tx[n]) * sm
                val z = tz[n] + (dz - tz[n]) * sm
                val i = f.begin(Frame.K_SOLDIER, x, 0f, z, PI_F / 2f)
                if (i < 0) break
                f.d[i + 5] = ids[n].toFloat()
                f.d[i + 8] = -1f
                if (st < tt) {
                    if (n < 3) {
                        val w = if (st < 10f) 0.08f + 0.05f * sin(st * 7f + n) else 0.5f + 0.1f * sin(st * 5f + n)
                        f.d[i + 12] = w
                    } else {
                        f.d[i + 12] = -1f
                        f.d[i + 13] = 0.4f * abs(sin(st * 2f + n))
                    }
                } else {
                    val alt = if (n % 2 == 0) sn else -sn
                    f.d[i + 12] = 0.5f + 0.5f * alt
                    f.d[i + 13] = abs(alt) * 4f
                }
            }
            if (st < tt + 0.3f) {
                val i = f.begin(Frame.K_BUILDING, cxw, 0f, 186f, 0f)
                if (i >= 0) {
                    f.d[i + 5] = 6f
                    f.d[i + 6] = 90f
                    f.d[i + 7] = 34f
                    f.d[i + 8] = 16f
                    f.d[i + 9] = 1f
                }
            } else {
                // fireworks
                val cols = arrayOf(floatArrayOf(1f, 0.8f, 0.2f), floatArrayOf(1f, 0.3f, 0.3f), floatArrayOf(0.4f, 1f, 0.5f), floatArrayOf(0.4f, 0.7f, 1f), floatArrayOf(1f, 0.5f, 1f), floatArrayOf(1f, 1f, 1f))
                for (kk in 0 until 6) {
                    val ph = ((st - tt) * 0.45f + kk * 0.31f) % 1f
                    val bx = cxw + (kk - 2.5f) * 55f
                    val by = 150f + (kk % 3) * 22f
                    val bz = czw - 50f
                    for (j in 0 until 14) {
                        val a = j * 6.2831853f / 14f
                        val r = ph * 46f
                        val i = f.begin(Frame.K_PART, bx + cos(a) * r, by + sin(a) * r * 0.8f - ph * ph * 22f, bz + sin(a * 2f) * r * 0.4f, 0f)
                        if (i < 0) break
                        f.d[i + 5] = 0f
                        f.d[i + 6] = 2.4f
                        f.d[i + 7] = cols[kk][0]
                        f.d[i + 8] = cols[kk][1]
                        f.d[i + 9] = cols[kk][2]
                        f.d[i + 10] = 1f - ph
                    }
                }
            }
        }
        FrameBus.publish(f)
    }

    // ------------------------------------------------------------------ projection for the HUD
    /** world point -> virtual screen coordinates; false when behind the camera */
    private fun proj(x: Float, y: Float, z: Float, out: FloatArray): Boolean {
        if (!cam.project(x, y, z, res)) return false
        out[0] = res[0] * pw / sc - offX
        out[1] = res[1] * ph / sc
        return true
    }

    // ------------------------------------------------------------------ helpers
    private fun fillRect(c: Canvas, l: Float, tt: Float, r: Float, b: Float, color: Int, alpha: Float = 1f) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        c.drawRect(l, tt, r, b, p)
    }

    private fun fullRect(c: Canvas, color: Int, alpha: Float) {
        fillRect(c, -offX - 4f, -4f, 544f + offX, wh + 4f, color, alpha)
    }

    private fun txt(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.CENTER, alpha: Float = 1f, shadow: Boolean = true
    ) {
        tp.shader = null
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = align
        tp.letterSpacing = 0f
        if (shadow) {
            tp.color = Color.BLACK
            tp.alpha = (150 * alpha).toInt().coerceIn(0, 255)
            c.drawText(s, x + 1.5f, y + 2.2f, tp)
        }
        tp.color = color
        tp.alpha = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        c.drawText(s, x, y, tp)
    }

    private fun fitTxt(c: Canvas, s: String, x: Float, y: Float, size: Float, maxW: Float, color: Int) {
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        val w = tp.measureText(s)
        val k = if (w > maxW) size * maxW / w else size
        txt(c, s, x, y, k, color)
    }

    private fun wrapLines(s: String, size: Float, maxW: Float): ArrayList<String> {
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        val out = ArrayList<String>()
        for (para in s.split("\n")) {
            if (para.isEmpty()) { out.add(""); continue }
            var cur = ""
            for (w in para.split(" ")) {
                val t = if (cur.isEmpty()) w else cur + " " + w
                if (tp.measureText(t) > maxW && cur.isNotEmpty()) {
                    out.add(cur)
                    cur = w
                } else {
                    cur = t
                }
            }
            if (cur.isNotEmpty()) out.add(cur)
        }
        return out
    }

    private fun paragraph(c: Canvas, s: String, cx: Float, y: Float, size: Float, maxW: Float, color: Int): Float {
        val lines = wrapLines(s, size, maxW)
        var yy = y
        for (ln in lines) {
            if (ln.isNotEmpty()) txt(c, ln, cx, yy, size, color)
            yy += size * 1.45f
        }
        return yy
    }

    private fun heart(c: Canvas, x: Float, y: Float, s: Float, color: Int) {
        pth.reset()
        pth.moveTo(x, y + s * 0.95f)
        pth.cubicTo(x - s * 1.5f, y + s * 0.1f, x - s * 0.85f, y - s * 0.95f, x, y - s * 0.3f)
        pth.cubicTo(x + s * 0.85f, y - s * 0.95f, x + s * 1.5f, y + s * 0.1f, x, y + s * 0.95f)
        pth.close()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = 255
        c.drawPath(pth, p)
    }

    private fun star(c: Canvas, x: Float, y: Float, rad: Float, color: Int) {
        pth.reset()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) rad else rad * 0.45f
            val a = (-90f + i * 36f) * PI_F / 180f
            val px = x + cos(a) * rr
            val py = y + sin(a) * rr
            if (i == 0) pth.moveTo(px, py) else pth.lineTo(px, py)
        }
        pth.close()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = 255
        c.drawPath(pth, p)
    }

    private fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, frac: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 130
        rf.set(x, y, x + w, y + h)
        c.drawRoundRect(rf, h / 2f, h / 2f, p)
        val f = frac.coerceIn(0f, 1f)
        if (f > 0.01f) {
            p.color = color
            p.alpha = 255
            rf.set(x, y, x + max(h, w * f), y + h)
            c.drawRoundRect(rf, h / 2f, h / 2f, p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.WHITE
        p.alpha = 90
        rf.set(x, y, x + w, y + h)
        c.drawRoundRect(rf, h / 2f, h / 2f, p)
    }

    /** kind 0 = gold primary, 1 = dark secondary, 2 = red */
    private fun button(
        c: Canvas, id: Int, cx: Float, cy: Float, w: Float, h: Float,
        label: String, kind: Int, size: Float = 24f, enabled: Boolean = true
    ) {
        val pressed = pressedId == id && enabled
        val s = if (pressed) 0.95f else 1f
        c.save()
        c.scale(s, s, cx, cy)
        val rad = h / 2f
        rf.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 90
        rf.offset(0f, 4f)
        c.drawRoundRect(rf, rad, rad, p)
        rf.offset(0f, -4f)
        var top = Color.rgb(255, 198, 66)
        var bot = Color.rgb(232, 112, 30)
        var tc = Color.rgb(54, 22, 6)
        if (kind == 1) {
            top = Color.rgb(78, 78, 98); bot = Color.rgb(40, 40, 54); tc = Color.WHITE
        } else if (kind == 2) {
            top = Color.rgb(226, 74, 64); bot = Color.rgb(150, 30, 36); tc = Color.WHITE
        }
        p.shader = LinearGradient(0f, rf.top, 0f, rf.bottom, top, bot, Shader.TileMode.CLAMP)
        p.alpha = if (enabled) 255 else 110
        c.drawRoundRect(rf, rad, rad, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = Color.WHITE
        p.alpha = if (kind == 0) 130 else 70
        c.drawRoundRect(rf, rad, rad, p)
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        val tw = tp.measureText(label)
        val fs = if (tw > w - 24f) size * (w - 24f) / tw else size
        txt(c, label, cx, cy + fs * 0.35f, fs, tc, shadow = kind != 0, alpha = if (enabled) 1f else 0.5f)
        c.restore()
        if (enabled) hits.add(Hit(id, cx - w / 2f - 4f, cy - h / 2f - 4f, cx + w / 2f + 4f, cy + h / 2f + 4f))
    }

    private fun panel(c: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        rf.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, rf.top, 0f, rf.bottom, Color.rgb(42, 36, 56), Color.rgb(20, 18, 30), Shader.TileMode.CLAMP)
        p.alpha = 245
        c.drawRoundRect(rf, 26f, 26f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = Color.rgb(255, 196, 80)
        p.alpha = 150
        c.drawRoundRect(rf, 26f, 26f, p)
    }

    private fun drawTitle(c: Canvas, cx: Float, y: Float, size: Float) {
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
        tp.letterSpacing = 0f
        tp.shader = null
        tp.color = Color.BLACK
        tp.alpha = 170
        c.drawText("AboLolo", cx + 3f, y + 5f, tp)
        tp.shader = LinearGradient(
            0f, y - size * 0.75f, 0f, y + size * 0.05f,
            Color.rgb(255, 232, 130), Color.rgb(240, 110, 30), Shader.TileMode.CLAMP
        )
        tp.alpha = 255
        c.drawText("AboLolo", cx, y, tp)
        tp.shader = null
    }

    private fun weaponName(w: Int): String = when (w) {
        0 -> L.t("مسدس", "Pistol")
        1 -> "AK-47"
        2 -> "G3"
        3 -> "Magnum"
        4 -> "RPG"
        5 -> "MP5"
        6 -> L.t("بومبكشن", "Shotgun")
        7 -> L.t("قناص SVD", "SVD Sniper")
        8 -> "PKM"
        9 -> L.t("كاربين بمنظار", "Scoped carbine")
        10 -> L.t("قنّاصة DMR", "DMR")
        else -> L.t("قنّاصة ثقيلة", "AMR .50")
    }

    // ------------------------------------------------------------------ drawing
    override fun onDraw(canvas: Canvas) {
        hits.clear()
        canvas.save()
        canvas.scale(sc, sc)
        canvas.translate(offX, 0f)
        when (state) {
            S_MENU -> drawMenu(canvas)
            S_SEL -> drawSelect(canvas)
            S_BRIEF -> drawBrief(canvas)
            S_PLAY -> drawPlay(canvas)
            S_PAUSE -> { drawHud(canvas); drawPause(canvas) }
            S_WIN -> drawWin(canvas)
            S_OVER -> drawOver(canvas)
            S_END -> drawEnd(canvas)
        }
        if (fade > 0f) fullRect(canvas, Color.BLACK, fade)
        canvas.restore()
    }

    private fun drawBackdrop(c: Canvas) {
        p.shader = LinearGradient(0f, 0f, 0f, wh, Color.rgb(24, 22, 40), Color.rgb(140, 96, 52), Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL
        p.alpha = 255
        c.drawRect(-offX - 4f, -4f, 544f + offX, wh + 4f, p)
        p.shader = null
        p.color = Color.rgb(96, 68, 40)
        p.alpha = 255
        pth.reset()
        pth.moveTo(-offX - 4f, wh * 0.78f)
        var x = -offX - 4f
        while (x <= 544f + offX) {
            pth.lineTo(x, wh * 0.78f - 26f * sin(x * 0.013f + 1f) - 12f * sin(x * 0.04f))
            x += 12f
        }
        pth.lineTo(544f + offX, wh + 4f)
        pth.lineTo(-offX - 4f, wh + 4f)
        pth.close()
        c.drawPath(pth, p)
    }

    /** draws a character card: the supplied artwork for Qubba / Savanna, a drawn figure for the others */
    private fun portrait(c: Canvas, which: Int, l: Float, t: Float, r: Float, b: Float) {
        val bm = if (which == 0) bmQubba else if (which == 1) bmSavanna else null
        rf.set(l, t, r, b)
        if (bm != null) {
            val dw = r - l
            val dh = b - t
            val bw = bm.width
            val bh = bm.height
            var sh = (bw * dh / dw).toInt()
            if (sh > bh) sh = bh
            srcR.set(0, 0, bw, sh)
            c.save()
            pth.reset()
            pth.addRoundRect(rf, 18f, 18f, Path.Direction.CW)
            c.clipPath(pth)
            p.shader = null
            p.style = Paint.Style.FILL
            p.alpha = 255
            c.drawBitmap(bm, srcR, rf, p)
            c.restore()
        } else {
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, t, 0f, b, Color.rgb(70, 64, 56), Color.rgb(34, 30, 28), Shader.TileMode.CLAMP)
            p.alpha = 255
            c.drawRoundRect(rf, 18f, 18f, p)
            p.shader = null
            val lk = if (which == 2) Looks.APOLLO else Looks.SAVANNA
            Sprites.soldier(c, (l + r) / 2f, b - 14f, (b - t) / 95f, lk, 0f, false, 1f, 0f, 6, 0f, 0f, 1f)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        p.color = Color.rgb(255, 196, 80)
        p.alpha = 200
        c.drawRoundRect(rf, 18f, 18f, p)
    }

    private fun heroPortrait(): Int = when (mission) {
        2, 4 -> 0
        3 -> 2
        else -> 1
    }

    private fun drawMenu(c: Canvas) {
        drawBackdrop(c)
        drawTitle(c, 270f, wh * 0.14f, 88f)
        txt(c, L.t("معارك تكتيكية ثلاثية الأبعاد", "3D Tactical Combat"), 270f, wh * 0.14f + 38f, 22f, Color.rgb(255, 226, 160))
        val top = wh * 0.22f
        val ph2 = min(250f, wh * 0.3f)
        portrait(c, 0, 40f, top, 262f, top + ph2)
        portrait(c, 1, 278f, top, 500f, top + ph2)
        txt(c, L.t("القبة", "Qubba"), 151f, top + ph2 + 24f, 20f, Color.WHITE)
        txt(c, L.t("السافنا", "Savanna"), 389f, top + ph2 + 24f, 20f, Color.WHITE)
        val by = top + ph2 + 80f
        button(c, B_PLAY, 270f, by, 300f, 62f, if (unlocked > 1) L.t("تابع - المعركة $unlocked", "Continue - Battle $unlocked") else L.t("ابدأ", "Start"), 0, 26f)
        button(c, B_MISS, 270f, by + 78f, 300f, 54f, L.t("المعارك", "Battles"), 1, 22f)
        button(c, B_SOUND, 270f - 80f, by + 148f, 140f, 44f, if (sfx.enabled) L.t("الصوت: تشغيل", "Sound: On") else L.t("الصوت: إيقاف", "Sound: Off"), 1, 16f)
        button(c, B_LANG, 270f + 80f, by + 148f, 140f, 44f, if (L.ar) "English" else "العربية", 1, 16f)
    }

    private fun drawSelect(c: Canvas) {
        drawBackdrop(c)
        txt(c, L.t("المعارك", "Battles"), 270f, 66f, 38f, Color.rgb(255, 226, 160))
        val top = 110f
        val step = min(120f, (wh - 230f) / 5f)
        for (n in 1..5) {
            val y = top + (n - 1) * step + step * 0.45f
            val ok = n <= unlocked
            panel(c, 270f, y, 470f, step - 12f)
            txt(c, "$n", 70f, y + 14f, 44f, if (ok) Color.rgb(255, 196, 80) else Color.GRAY)
            fitTxt(c, Story.missionTitle(n), 290f, y - 4f, 24f, 300f, if (ok) Color.WHITE else Color.GRAY)
            fitTxt(c, Story.missionHero(n), 290f, y + 24f, 17f, 300f, Color.rgb(200, 190, 170))
            if (ok) hits.add(Hit(B_M0 + n, 35f, y - step / 2f, 505f, y + step / 2f))
            else txt(c, L.t("مقفلة", "Locked"), 470f, y + 8f, 16f, Color.GRAY)
        }
        button(c, B_BACK, 270f, wh - 50f, 220f, 52f, L.t("رجوع", "Back"), 1, 20f)
    }

    private fun drawBrief(c: Canvas) {
        drawBackdrop(c)
        val top = 40f
        panel(c, 270f, wh * 0.5f, 500f, wh - 90f)
        txt(c, L.t("المعركة", "Battle") + " " + mission, 270f, top + 40f, 20f, Color.rgb(255, 196, 80))
        txt(c, Story.missionTitle(mission), 270f, top + 78f, 32f, Color.WHITE)
        txt(c, Story.missionHero(mission), 270f, top + 106f, 17f, Color.rgb(200, 190, 170))
        val pt = top + 124f
        portrait(c, heroPortrait(), 195f, pt, 345f, pt + 180f)
        paragraph(c, Story.brief(mission), 270f, pt + 212f, 17f, 440f, Color.WHITE)
        button(c, B_START, 270f, wh - 82f, 280f, 56f, L.t("انطلق", "Go"), 0, 26f)
        button(c, B_BACK, 76f, 40f, 110f, 42f, L.t("رجوع", "Back"), 1, 16f)
    }

    private fun drawPlay(c: Canvas) {
        if (world.dancing) {
            drawDance(c)
            return
        }
        drawHud(c)
        if (world.waitCut) drawCut(c) else drawControls(c)
    }

    // ---- HUD ----
    private fun targetPos(): FloatArray? {
        return when (world.objId) {
            1, 2 -> floatArrayOf(world.tankX(), world.tankY() + 60f)
            3 -> if (world.phase == 0) floatArrayOf(world.surveyX(), world.surveyY() + 30f) else null
            7, 9 -> floatArrayOf(world.goalX(), world.goalY())
            4, 10 -> {
                val pl = world.player
                var best: FloatArray? = null
                var bd = 1e9f
                if (pl != null) {
                    for (b in world.buildings) {
                        if (b.type == 1 && b.side == 1 && !b.dead) {
                            val d = hypot(b.x - pl.x, b.y - pl.y)
                            if (d < bd) { bd = d; best = floatArrayOf(b.x, b.y) }
                        }
                    }
                }
                best
            }
            else -> null
        }
    }

    private val pt = FloatArray(2)

    private fun drawHud(c: Canvas) {
        val pl = world.player ?: return
        // objective
        val ob = Story.objective(world.objId)
        rf.set(10f, 8f, 470f, 62f)
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = Color.BLACK
        p.alpha = 110
        c.drawRoundRect(rf, 14f, 14f, p)
        fitTxt(c, ob, 240f, 30f, 19f, 430f, Color.rgb(255, 226, 160))
        var sub = ""
        var frac = -1f
        when (world.objId) {
            2, 3 -> frac = world.progress
            5, 6, 8, 11, 4 -> sub = L.t("الأعداء: ", "Enemies: ") + world.remaining()
            10 -> sub = L.t("غرف التحكم: ", "Control rooms: ") + world.controlRoomsLeft() + "   " + L.t("الأعداء: ", "Enemies: ") + world.remaining()
            7, 9 -> sub = (world.distToGoal() / 10f).toInt().toString() + " m"
            else -> {}
        }
        if (world.objId == 3) sub = L.t("الخبراء: ", "Experts: ") + world.expertsAlive()
        if (frac >= 0f) bar(c, 20f, 44f, 200f, 10f, frac, Color.rgb(120, 220, 140))
        if (sub.isNotEmpty()) txt(c, sub, if (frac >= 0f) 340f else 240f, 54f, 15f, Color.WHITE)
        if (state == S_PLAY) button(c, B_PAUSE, rEdge() - 34f, 36f, 52f, 46f, "II", 1, 22f)
        // health, weapon, ammo
        val v = world.pveh
        if (v != null && !v.dead) {
            bar(c, 16f - offX, 74f, 180f, 12f, v.hp / v.maxHp, Color.rgb(120, 220, 130))
            txt(c, L.t("المركبة - مدفع 23 ملم", "Vehicle - 23mm cannon"), 204f - offX, 85f, 14f, Color.WHITE, Paint.Align.LEFT)
        } else {
            bar(c, 16f - offX, 74f, 180f, 12f, pl.hp / pl.maxHp, if (pl.hp / pl.maxHp > 0.35f) Color.rgb(120, 220, 130) else Color.rgb(230, 70, 60))
            val w = world.weapon
            val am = if (world.reloadT > 0f) L.t("تعبئة...", "reloading...") else world.ammo[w].toString() + " / " + world.reserve[w]
            txt(c, weaponName(w) + "   " + am, 204f - offX, 85f, 14f, Color.WHITE, Paint.Align.LEFT)
        }
        if (world.objId == 5) {
            for (i in 0 until 6) heart(c, 26f - offX + i * 26f, 108f, 9f, if (i < world.hearts) Color.rgb(230, 60, 70) else Color.rgb(80, 60, 66))
        }
        if (world.waitCut) return
        // tags and bars over the 3D world
        for (s in world.soldiers) {
            if (s.dead || s.inVeh) continue
            if (s.tagAr.isNotEmpty() || s.expert) {
                if (proj(s.x, 56f, s.y, pt)) {
                    if (s.tagAr.isNotEmpty()) txt(c, L.t(s.tagAr, s.tagEn), pt[0], pt[1], 14f, if (s.isPlayer) Color.rgb(140, 255, 150) else Color.rgb(255, 226, 160))
                    if (s.expert) bar(c, pt[0] - 14f, pt[1] + 2f, 28f, 5f, s.hp / s.maxHp, Color.rgb(255, 170, 60))
                }
            }
        }
        for (vh in world.vehicles) {
            if (vh.dead || vh.isPlayer) continue
            if (vh.hp < vh.maxHp && proj(vh.x, 44f, vh.y, pt)) bar(c, pt[0] - 24f, pt[1], 48f, 6f, vh.hp / vh.maxHp, Color.rgb(230, 80, 70))
        }
        for (b in world.buildings) {
            if (b.dead || b.side != 1) continue
            if (proj(b.x, b.h + 44f, b.y, pt)) {
                bar(c, pt[0] - 30f, pt[1], 60f, 8f, b.hp / b.maxHp, Color.rgb(230, 80, 70))
                txt(c, L.t("غرفة تحكم", "Control room"), pt[0], pt[1] - 6f, 13f, Color.rgb(255, 150, 140))
            }
        }
        // crosshair
        if (world.pveh != null || state == S_PLAY) {
            val ax = pl.aimX
            val ay = pl.aimY
            val cd = if (world.pveh != null) 300f else world.crossDist
            val base = if (world.pveh != null) world.pveh!!.y else pl.y
            val bx = if (world.pveh != null) world.pveh!!.x else pl.x
            if (proj(bx + ax * cd, 12f, base + ay * cd, pt)) {
                val rad = (10f + world.recoil * 220f + (if (pl.moving) 4f else 0f)) * (if (world.crouching) 0.8f else 1f)
                p.style = Paint.Style.STROKE
                p.shader = null
                p.strokeWidth = 2.4f
                p.color = if (world.assistLock) Color.rgb(255, 70, 60) else Color.WHITE
                p.alpha = 230
                c.drawCircle(pt[0], pt[1], rad, p)
                c.drawLine(pt[0] - rad - 7f, pt[1], pt[0] - rad + 3f, pt[1], p)
                c.drawLine(pt[0] + rad - 3f, pt[1], pt[0] + rad + 7f, pt[1], p)
                c.drawLine(pt[0], pt[1] - rad - 7f, pt[0], pt[1] - rad + 3f, p)
                c.drawLine(pt[0], pt[1] + rad - 3f, pt[0], pt[1] + rad + 7f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(pt[0], pt[1], 1.8f, p)
            }
        }
        // hit marker and damage numbers
        if (world.hitT > 0f && state == S_PLAY && world.pveh == null) {
            val hx = 270f
            val hy = wh * 0.5f
            val a = (world.hitT / 0.28f).coerceIn(0f, 1f)
            p.style = Paint.Style.STROKE
            p.shader = null
            p.strokeWidth = 3f
            p.color = if (world.hitKill) Color.rgb(255, 60, 50) else if (world.hitCrit) Color.rgb(255, 210, 60) else Color.WHITE
            p.alpha = (255 * a).toInt()
            val o = 9f + (1f - a) * 6f
            c.drawLine(hx - o - 8f, hy - o - 8f, hx - o, hy - o, p); c.drawLine(hx + o + 8f, hy - o - 8f, hx + o, hy - o, p)
            c.drawLine(hx - o - 8f, hy + o + 8f, hx - o, hy + o, p); c.drawLine(hx + o + 8f, hy + o + 8f, hx + o, hy + o, p)
            p.style = Paint.Style.FILL
        }
        for (pp in world.pops) {
            if (proj(pp.x, 50f + pp.t * 40f, pp.y, pt)) {
                val a = (1f - pp.t / 0.9f).coerceIn(0f, 1f)
                txt(c, (if (pp.crit) "! " else "") + pp.v, pt[0], pt[1], if (pp.crit) 22f else 16f, if (pp.crit) Color.rgb(255, 210, 60) else Color.WHITE, alpha = a)
            }
        }
        // scope overlay
        if (world.scoped && world.pveh == null && state == S_PLAY) {
            val cx = 270f
            val cy = wh * 0.5f
            val r = 190f
            p.style = Paint.Style.STROKE
            p.shader = null
            p.color = Color.BLACK
            p.alpha = 255
            p.strokeWidth = 1600f
            c.drawCircle(cx, cy, r + 800f, p)
            p.strokeWidth = 3f
            p.color = Color.argb(200, 20, 20, 20)
            c.drawCircle(cx, cy, r, p)
            p.strokeWidth = 1.6f
            p.color = Color.argb(230, 10, 10, 10)
            c.drawLine(cx - r, cy, cx + r, cy, p)
            c.drawLine(cx, cy - r, cx, cy + r, p)
            for (i in 1..6) {
                val d = i * 22f
                c.drawLine(cx - 6f, cy + d, cx + 6f, cy + d, p)
            }
            p.style = Paint.Style.FILL
            c.drawCircle(cx, cy, 2.2f, p)
            txt(c, "x" + world.zoom().toInt(), cx + r - 24f, cy + r - 20f, 16f, Color.WHITE)
            bar(c, cx - 60f, cy + r + 8f, 120f, 7f, world.breath, Color.rgb(120, 200, 255))
        }
        // objective compass
        val tg = targetPos()
        if (tg != null && state == S_PLAY) {
            val px = if (world.pveh != null) world.pveh!!.x else pl.x
            val pz = if (world.pveh != null) world.pveh!!.y else pl.y
            val dx = tg[0] - px
            val dz = tg[1] - pz
            val d = hypot(dx, dz)
            val fx2 = cos(world.camYaw)
            val fz2 = sin(world.camYaw)
            val sx = dx * (-fz2) + dz * fx2
            val sy = -(dx * fx2 + dz * fz2)
            val l = max(0.001f, hypot(sx, sy))
            val ux = sx / l
            val uy = sy / l
            val ax = 270f
            val ay = 128f
            pth.reset()
            pth.moveTo(ax + ux * 16f, ay + uy * 16f)
            pth.lineTo(ax - ux * 9f - uy * 11f, ay - uy * 9f + ux * 11f)
            pth.lineTo(ax - ux * 9f + uy * 11f, ay - uy * 9f - ux * 11f)
            pth.close()
            p.style = Paint.Style.FILL
            p.shader = null
            p.color = Color.rgb(255, 214, 70)
            p.alpha = 235
            c.drawPath(pth, p)
            txt(c, (d / 10f).toInt().toString() + " m", ax, ay + 34f, 13f, Color.WHITE)
        }
    }

    private fun circleBtn(c: Canvas, x: Float, y: Float, r: Float, fillC: Int, label: String, size: Float, on: Boolean = false) {
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = fillC
        p.alpha = if (on) 230 else 190
        c.drawCircle(x, y, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        p.color = Color.WHITE
        p.alpha = 160
        c.drawCircle(x, y, r, p)
        tp.textSize = size
        tp.typeface = Typeface.DEFAULT_BOLD
        val w = tp.measureText(label)
        val s2 = if (w > r * 1.7f) size * (r * 1.7f) / w else size
        txt(c, label, x, y + s2 * 0.35f, s2, Color.WHITE)
    }

    private fun drawControls(c: Canvas) {
        // joystick
        if (joyId != -1) {
            p.style = Paint.Style.FILL
            p.shader = null
            p.color = Color.WHITE
            p.alpha = 40
            c.drawCircle(joyOx, joyOy, 58f, p)
            p.alpha = 110
            c.drawCircle(joyKx, joyKy, 26f, p)
        } else {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            p.color = Color.WHITE
            p.alpha = 50
            c.drawCircle(110f - offX, wh - 130f, 54f, p)
            txt(c, L.t("حرّك", "Move"), 110f - offX, wh - 124f, 16f, Color.WHITE, alpha = 0.5f)
        }
        val inVeh = world.pveh != null
        circleBtn(c, fireX(), fireY(), 56f, if (fireHeld) Color.rgb(255, 110, 70) else Color.rgb(210, 60, 50), L.t("نار", "FIRE"), 22f, fireHeld)
        if (!inVeh) {
            circleBtn(c, reloadX(), reloadY(), 34f, Color.rgb(50, 60, 80), L.t("تعبئة", "Reload"), 14f, world.reloadT > 0f)
            if (World.canScope(world.weapon)) circleBtn(c, scopeX(), scopeY(), 34f, if (world.scoped) Color.rgb(60, 130, 200) else Color.rgb(50, 60, 80), if (world.scoped && world.scopeLvl >= 1 && World.scopeLevels(world.weapon) > 1) "x" + world.zoom().toInt() else L.t("منظار", "Scope"), 14f, world.scoped)
            circleBtn(c, crouchX(), crouchY(), 32f, if (world.crouching) Color.rgb(60, 130, 200) else Color.rgb(50, 60, 80), L.t("انحناء", "Crouch"), 13f, world.crouching)
            if (world.scoped) circleBtn(c, breathX(), breathY(), 34f, if (world.holdBreath) Color.rgb(60, 160, 220) else Color.rgb(50, 60, 80), L.t("نَفَس", "Breath"), 14f, world.holdBreath)
            circleBtn(c, wpnX(), wpnY(), 38f, Color.rgb(60, 56, 80), weaponName(world.weapon), 14f, wmenu)
        }
        val ready = world.supportCd <= 0f
        if (ready) circleBtn(c, supX(), supY(), 38f, Color.rgb(220, 160, 40), L.t("إسناد", "Fire"), 17f, true)
        else circleBtn(c, supX(), supY(), 38f, Color.rgb(60, 56, 50), world.supportCd.toInt().toString(), 22f)
        if (wmenu && !inVeh) {
            panel(c, 270f, wh * 0.5f, 410f, 330f)
            txt(c, L.t("اختر السلاح", "Choose weapon"), 270f, wh * 0.5f - 130f, 22f, Color.rgb(255, 214, 90))
            for (i in 0 until World.NW) {
                val col = i % 3
                val row = i / 3
                val bx = 270f - 130f + col * 130f
                val by = wh * 0.5f - 85f + row * 58f
                button(c, B_W0 + i, bx, by, 124f, 50f, weaponName(i), if (i == world.weapon) 0 else 1, 14f)
            }
        }
    }

    private fun drawCut(c: Canvas) {
        val lines = Story.cut(curCut)
        if (lines.isEmpty() || cutIdx >= lines.size) return
        val ln = lines[cutIdx]
        val key = ln[0]
        fullRect(c, Color.BLACK, 0.35f)
        val py = wh - 270f
        panel(c, 270f, py + 100f, 510f, 230f)
        val which = when (key) {
            "qubba" -> 0
            "savanna" -> 1
            "apollo" -> 2
            else -> -1
        }
        if (which >= 0) portrait(c, which, 36f, py - 6f, 150f, py + 150f)
        else {
            val lk: Look = when (key) {
                "elite" -> Looks.ELITE_UNI
                "defector" -> Looks.DEFECTOR
                "falan" -> Looks.FALAN
                "dinari" -> Looks.DINARI
                else -> Looks.JANJA
            }
            rf.set(36f, py - 6f, 150f, py + 150f)
            p.style = Paint.Style.FILL
            p.shader = LinearGradient(0f, py, 0f, py + 150f, Color.rgb(70, 64, 56), Color.rgb(34, 30, 28), Shader.TileMode.CLAMP)
            p.alpha = 255
            c.drawRoundRect(rf, 18f, 18f, p)
            p.shader = null
            Sprites.soldier(c, 93f, py + 140f, 1.7f, lk, 0f, false, 1f, 0f, 6, 0f, 0f, 1f)
        }
        txt(c, Story.speaker(key), 330f, py + 6f, 20f, Color.rgb(255, 196, 80))
        paragraph(c, L.t(ln[1], ln[2]), 330f, py + 38f, 17f, 300f, Color.WHITE)
        txt(c, L.t("المس للمتابعة", "Tap to continue"), 270f, py + 205f, 14f, Color.rgb(200, 190, 170))
        hits.add(Hit(B_CUT, -offX - 10f, -10f, 550f + offX, wh + 10f))
    }

    private fun drawDance(c: Canvas) {
        val m = world.mission
        val st = world.sceneT
        val tt = World.TREATY_T
        if (m == 5 && st < tt + 0.5f) {
            val a = if (st > tt - 0.5f) max(0f, (tt + 0.5f - st)) else 1f
            panel(c, 270f, 142f, 480f, 232f)
            txt(c, L.t("معاهدة السلام", "Peace Treaty"), 270f, 66f, 30f, Color.rgb(255, 214, 90), alpha = a)
            txt(c, L.t("١. وقف دائم لإطلاق النار بين الجنجا والحكم الجديد", "1. Permanent ceasefire: Janja and the new government"), 270f, 100f, 15f, Color.WHITE, alpha = a)
            txt(c, L.t("٢. نفي النخب والفلنقايات إلى «السماء ذات البروج»", "2. Elites and Falanqayat exiled to the sky of constellations"), 270f, 124f, 15f, Color.WHITE, alpha = a)
            txt(c, L.t("٣. وحدة البلاد وعودة النازحين وإعادة الإعمار", "3. One country, return of the displaced, reconstruction"), 270f, 148f, 15f, Color.WHITE, alpha = a)
            tp.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            val names = arrayOf(L.t("السافنا", "Savanna"), L.t("أبولو", "Apollo"), L.t("القبة", "Qubba"))
            val xs = floatArrayOf(110f, 270f, 430f)
            val ts = floatArrayOf(3.5f, 6.5f, 9.5f)
            for (n in 0 until 3) {
                p.shader = null
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.5f
                p.color = Color.WHITE
                p.alpha = (120 * a).toInt()
                c.drawLine(xs[n] - 60f, 206f, xs[n] + 60f, 206f, p)
                if (st > ts[n]) {
                    tp.textSize = 24f
                    tp.textAlign = Paint.Align.CENTER
                    tp.shader = null
                    tp.color = Color.rgb(150, 200, 255)
                    tp.alpha = (255 * a).toInt()
                    c.drawText(names[n], xs[n], 198f - (if (n == 1) 2f else 0f), tp)
                }
                txt(c, names[n], xs[n], 226f, 13f, Color.rgb(200, 190, 170), alpha = a)
            }
            if (st > 10f && st < tt) txt(c, L.t("تمّ التوقيع!", "Signed!"), 270f, wh * 0.44f, 40f, Color.rgb(255, 214, 90))
        } else {
            txt(c, L.t("النصر!", "Victory!"), 270f, wh * 0.14f, 54f, Color.rgb(255, 214, 90))
            txt(c, if (m == 4) L.t("احتفال دار صباح", "Dar Sabah celebrates") else L.t("احتفال السلام", "Celebration of peace"), 270f, wh * 0.14f + 36f, 22f, Color.WHITE)
            for (f in confetti) {
                p.style = Paint.Style.FILL
                p.shader = null
                p.color = confCols[f[4].toInt().coerceIn(0, 3)]
                p.alpha = 255
                c.drawRect(f[0], f[1], f[0] + 6f, f[1] + 9f, p)
            }
        }
        val need = if (m == 5) tt + 12f else 12f
        if (st > need) button(c, B_DANCE, 270f, wh - 60f, 260f, 56f, L.t("متابعة", "Continue"), 0, 24f)
    }

    private fun drawPause(c: Canvas) {
        fullRect(c, Color.BLACK, 0.55f)
        panel(c, 270f, wh / 2f, 360f, 330f)
        txt(c, L.t("إيقاف مؤقت", "Paused"), 270f, wh / 2f - 100f, 34f, Color.WHITE)
        button(c, B_RESUME, 270f, wh / 2f - 30f, 260f, 56f, L.t("استمر", "Resume"), 0, 24f)
        button(c, B_RESTART, 270f, wh / 2f + 40f, 260f, 52f, L.t("إعادة المعركة", "Restart"), 1, 20f)
        button(c, B_MENU, 270f, wh / 2f + 104f, 260f, 52f, L.t("القائمة", "Menu"), 1, 20f)
    }

    private fun drawWin(c: Canvas) {
        fullRect(c, Color.BLACK, 0.6f)
        panel(c, 270f, wh / 2f, 460f, 420f)
        txt(c, L.t("نصر!", "Victory!"), 270f, wh / 2f - 140f, 44f, Color.rgb(255, 214, 90))
        for (i in 0 until 3) star(c, 190f + i * 80f, wh / 2f - 90f, 26f, Color.rgb(255, 200, 60))
        paragraph(c, Story.winText(mission), 270f, wh / 2f - 30f, 20f, 400f, Color.WHITE)
        button(c, B_NEXT, 270f, wh / 2f + 100f, 280f, 58f, L.t("المعركة التالية", "Next battle"), 0, 24f)
        button(c, B_MENU, 270f, wh / 2f + 168f, 280f, 50f, L.t("القائمة", "Menu"), 1, 20f)
    }

    private fun drawOver(c: Canvas) {
        fullRect(c, Color.rgb(40, 0, 0), 0.6f)
        panel(c, 270f, wh / 2f, 460f, 380f)
        txt(c, L.t("فشلت المهمة", "Mission failed"), 270f, wh / 2f - 110f, 36f, Color.rgb(255, 110, 100))
        paragraph(c, Story.loseText(world.loseReason), 270f, wh / 2f - 50f, 20f, 400f, Color.WHITE)
        button(c, B_RESTART, 270f, wh / 2f + 50f, 280f, 58f, L.t("حاول مجدداً", "Try again"), 0, 24f)
        button(c, B_MENU, 270f, wh / 2f + 120f, 280f, 50f, L.t("القائمة", "Menu"), 1, 20f)
    }

    private fun drawEnd(c: Canvas) {
        drawBackdrop(c)
        val ph3 = menuT / BEAT * PI_F
        val sn = sin(ph3)
        val list = arrayOf(Looks.SAVANNA, Looks.QUBBA, Looks.JANJA, Looks.DINARI, Looks.DEFECTOR)
        for (i in list.indices) {
            val alt = if (i % 2 == 0) sn else -sn
            Sprites.soldier(c, 60f + i * 105f, wh * 0.84f, 1.2f, list[i], 0f, false, 1f, 0f, 6, 0f, 0f, 1f, 0.5f + 0.5f * alt, max(0f, alt), max(0f, -alt), abs(alt) * 5f)
        }
        drawTitle(c, 270f, wh * 0.14f, 64f)
        panel(c, 270f, wh * 0.46f, 480f, wh * 0.5f)
        paragraph(c, Story.epilogue(), 270f, wh * 0.46f - wh * 0.25f + 56f, 20f, 420f, Color.WHITE)
        button(c, B_MENU, 270f, wh * 0.46f + wh * 0.25f - 32f, 240f, 54f, L.t("القائمة", "Menu"), 0, 22f)
    }
}
