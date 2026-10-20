package com.abololo.game

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Low-poly models built from boxes, cylinders and spheres. Local +Z is "forward". */
object Models3D {
    private const val RAD2DEG = 57.29578f

    private fun k(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    fun mix(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt()
        return Color.rgb(r, g, bl)
    }

    private val RED = k(230, 40, 30)
    private val METAL = k(52, 54, 58)
    private val METAL2 = k(86, 88, 92)
    private val WOOD = k(112, 72, 42)
    private val BLACK = k(24, 24, 26)

    /** converts a world heading (x = cos, z = sin) into the rotation about Y that makes local +Z point there */
    fun yawDeg(a: Float): Float = 90f - a * RAD2DEG

    // ------------------------------------------------------------------ guns
    private val TIP = floatArrayOf(8f, 19f, 20f, 8f, 18f, 14f, 22f, 26f, 26f, 22f, 27f, 38f)

    fun gunTip(id: Int): Float = TIP[id.coerceIn(0, 11)]

    fun drawGun(g: Gfx, id: Int, muzzle: Float) {
        when (id) {
            0 -> {
                g.color(BLACK); g.box(0f, 0f, 3f, 1.6f, 3.2f, 6.5f)
                g.color(METAL2); g.box(0f, 1.4f, 4f, 1.7f, 0.8f, 7f)
                g.color(BLACK); g.box(0f, -2.6f, 1f, 1.4f, 3.5f, 2.2f)
            }
            1 -> {
                g.color(METAL); g.box(0f, 0f, 6f, 2.2f, 3f, 14f)
                g.color(WOOD); g.box(0f, -0.6f, -4.5f, 2.2f, 3.6f, 8f)
                g.color(WOOD); g.box(0f, -0.3f, 13.5f, 2.6f, 2.4f, 6f)
                g.color(METAL); g.push(); g.translate(0f, -4.3f, 8f); g.rotX(-18f); g.box(0f, 0f, 0f, 1.8f, 7f, 2.8f); g.pop()
                g.color(METAL2); g.barrel(0f, 0.9f, 12f, 19.5f, 0.55f)
            }
            2 -> {
                g.color(k(40, 44, 40)); g.box(0f, 0f, 6.5f, 2.2f, 3.2f, 16f)
                g.color(k(50, 56, 48)); g.box(0f, -0.4f, -4.5f, 2.2f, 3.6f, 8f)
                g.color(k(40, 44, 40)); g.box(0f, -0.2f, 15f, 2.6f, 2.6f, 7f)
                g.color(METAL); g.box(0f, -4.5f, 7f, 1.8f, 6f, 2.6f)
                g.color(METAL2); g.barrel(0f, 0.9f, 15f, 21f, 0.6f)
            }
            3 -> {
                g.color(METAL2); g.box(0f, 0f, 3.5f, 1.8f, 3.4f, 7f)
                g.color(METAL); g.barrel(0f, 1.2f, 6f, 10f, 0.9f)
                g.color(METAL); g.barrel(0f, 0.4f, 1f, 4.5f, 1.6f)
                g.color(WOOD); g.box(0f, -3f, -0.5f, 1.8f, 4.5f, 2.6f)
            }
            4 -> {
                g.color(k(74, 86, 60)); g.barrel(0f, 3.5f, -9f, 14f, 2.3f)
                g.color(k(50, 56, 44)); g.barrel(0f, 3.5f, -11f, -8f, 3.2f)
                g.color(k(96, 100, 70)); g.coneM(0f, 3.5f, 0f, 0.1f, 0.1f)
                g.color(k(94, 98, 70)); g.push(); g.translate(0f, 3.5f, 17.5f); g.rotX(90f); g.coneM(0f, 0f, 0f, 2.8f, 8f); g.pop()
                g.color(BLACK); g.box(0f, 0f, 2f, 1.6f, 4f, 2f)
            }
            5 -> {
                g.color(METAL); g.box(0f, 0f, 4.5f, 2.2f, 3.2f, 10f)
                g.color(METAL2); g.barrel(0f, 0.8f, 9f, 14f, 0.55f)
                g.color(METAL); g.box(0f, -4.8f, 6f, 1.6f, 7f, 2.4f)
                g.color(BLACK); g.box(0f, -0.4f, -4f, 1.4f, 1.4f, 6f)
            }
            6 -> {
                g.color(METAL); g.barrel(0f, 0.9f, 4f, 22f, 0.9f)
                g.color(METAL2); g.barrel(0f, -0.6f, 6f, 17f, 0.8f)
                g.color(WOOD); g.box(0f, -0.5f, 12f, 2.6f, 2.4f, 6f)
                g.color(WOOD); g.box(0f, -0.8f, -4f, 2.2f, 3.4f, 9f)
                g.color(METAL); g.box(0f, 0f, 3f, 2.2f, 3f, 5f)
            }
            7 -> {
                g.color(k(44, 48, 40)); g.box(0f, 0f, 5f, 2.2f, 3f, 15f)
                g.color(WOOD); g.box(0f, -0.8f, -6f, 2.4f, 4f, 10f)
                g.color(METAL2); g.barrel(0f, 0.7f, 12f, 26f, 0.6f)
                g.color(BLACK); g.barrel(0f, 3.4f, 1f, 9f, 1.4f)
                g.color(k(90, 160, 220)); g.barrel(0f, 3.4f, 9f, 9.6f, 1.2f)
                g.color(METAL); g.box(0f, -4f, 6f, 1.6f, 5f, 2.4f)
            }
            8 -> {
                g.color(k(46, 50, 44)); g.box(0f, 0f, 6.5f, 2.8f, 4.4f, 17f)
                g.color(WOOD); g.box(0f, -0.4f, -5.5f, 2.4f, 4f, 8f)
                g.color(METAL2); g.barrel(0f, 1f, 15f, 26f, 0.7f)
                g.color(k(34, 34, 30)); g.box(0f, -4.2f, 3f, 2.6f, 4f, 4f)
                g.color(k(190, 150, 50)); g.box(0f, -4.2f, 3f, 2.7f, 1f, 3.4f)
                g.color(METAL); g.push(); g.translate(0f, -1f, 16f); g.rotX(25f); g.box(1.6f, -3.5f, 0f, 0.5f, 7f, 0.5f); g.box(-1.6f, -3.5f, 0f, 0.5f, 7f, 0.5f); g.pop()
            }
            9 -> {
                g.color(k(150, 130, 96)); g.box(0f, 0f, 6f, 2.2f, 3.2f, 15f)
                g.color(k(120, 100, 72)); g.box(0f, -0.4f, -5f, 2.2f, 3.6f, 8f)
                g.color(METAL2); g.barrel(0f, 0.9f, 13f, 22f, 0.55f)
                g.color(METAL); g.box(0f, -4.6f, 7f, 1.8f, 6f, 2.6f)
                g.color(BLACK); g.barrel(0f, 3.3f, 2f, 10f, 1.2f)
            }
            10 -> {
                g.color(k(60, 66, 54)); g.box(0f, 0f, 6f, 2.2f, 3.2f, 17f)
                g.color(k(70, 76, 60)); g.box(0f, -0.6f, -6f, 2.4f, 4f, 10f)
                g.color(METAL2); g.barrel(0f, 0.8f, 14f, 27f, 0.6f)
                g.color(BLACK); g.barrel(0f, 3.5f, 0f, 11f, 1.5f)
                g.color(k(90, 160, 220)); g.barrel(0f, 3.5f, 11f, 11.6f, 1.3f)
                g.color(METAL); g.box(0f, -4.4f, 6f, 1.8f, 5.4f, 2.6f)
            }
            else -> {
                g.color(k(70, 74, 60)); g.box(0f, 0f, 7f, 3.2f, 4.4f, 24f)
                g.color(k(86, 82, 64)); g.box(0f, -0.6f, -8f, 3f, 5f, 10f)
                g.color(METAL2); g.barrel(0f, 0.9f, 18f, 36f, 0.85f)
                g.color(METAL); g.box(0f, 0.9f, 36.5f, 2.6f, 2.6f, 3.2f)
                g.color(BLACK); g.barrel(0f, 4f, 0f, 14f, 1.7f)
                g.color(k(90, 160, 220)); g.barrel(0f, 4f, 14f, 14.6f, 1.5f)
                g.color(METAL); g.push(); g.translate(0f, -1f, 30f); g.rotX(25f); g.box(1.8f, -4f, 0f, 0.5f, 8f, 0.5f); g.box(-1.8f, -4f, 0f, 0.5f, 8f, 0.5f); g.pop()
            }
        }
        if (muzzle > 0f) {
            g.unlit(true)
            g.color(k(255, 220, 110))
            val z = gunTip(id)
            g.sphere(0f, 0.9f, z + 2.5f, 3.2f, 3.2f, 4.5f)
            g.color(k(255, 250, 220))
            g.sphere(0f, 0.9f, z + 1.5f, 1.6f, 1.6f, 2.4f)
            g.unlit(false)
        }
    }

    // ------------------------------------------------------------------ soldiers
    /**
     * Draws a soldier standing at (x, y, z) facing world heading yaw (radians; forward = (cos, sin) in the x-z plane).
     * armUp >= 0 -> dance pose. fall 0..1 -> lying down. crouch 0..1. kick = weapon recoil 0..1.
     */
    fun soldier(
        g: Gfx, lk: Look, x: Float, y: Float, z: Float, yaw: Float, phase: Float, moving: Boolean,
        gun: Int, muzzle: Float, flash: Float, fall: Float, armUp: Float, bounce: Float,
        crouch: Float = 0f, kick: Float = 0f
    ) {
        fun tint(c: Int): Int = if (flash > 0f) mix(c, RED, min(1f, flash * 1.6f)) else c
        val li = Looks.id(lk)
        g.push()
        g.translate(x, y + bounce, z)
        g.rotY(yawDeg(yaw))
        if (fall > 0f) g.rotX(-90f * min(1f, fall))
        g.scale(lk.scale)
        val dance = armUp >= 0f
        val cr = if (dance) 0f else crouch
        val sw = if (moving) sin(phase) else 0f
        val swing = sw * 38f
        val drop = 6.2f * cr
        val camoT = g.camoTex[li]
        // legs
        for (side in 0..1) {
            val sx = if (side == 0) 3.1f else -3.1f
            val a = if (dance) (if (side == 0) bounce * 6f else -bounce * 4f) else (if (side == 0) swing else -swing)
            val sa = if (dance) 0f else (if (side == 0) max(0f, -sw) else max(0f, sw)) * 46f
            g.push()
            g.translate(sx, 17f - drop, 0f)
            g.rotX(a - 62f * cr)
            if (camoT != 0) g.tex(camoT, 1f, 1f)
            g.color(tint(if (camoT != 0) k(255, 255, 255) else lk.clothD))
            g.limb(8.4f, 2.9f)
            g.translate(0f, -8.4f, 0f)
            g.rotX(sa + 104f * cr)
            g.limb(8.2f, 2.5f, false)
            g.noTex()
            g.translate(0f, -8.2f, 0f)
            g.color(tint(k(34, 30, 26)))
            g.box(0f, -0.6f, 1.2f, 4.2f, 2.6f, 7.4f)
            g.pop()
        }
        // torso (leans forward when crouching)
        g.push()
        g.translate(0f, -drop, 0f)
        g.rotX(14f * cr)
        if (camoT != 0) g.tex(camoT, 1f, 1f)
        g.color(tint(if (camoT != 0) k(255, 255, 255) else lk.cloth))
        g.sphere(0f, 17.6f, 0f, 5.6f, 3.6f, 3.9f)
        g.sphere(0f, 23.2f, 0f, 6.5f, 7.2f, 4.3f)
        g.noTex()
        if (lk.suit) {
            g.color(tint(k(240, 240, 240))); g.box(0f, 24.5f, 3.9f, 2.6f, 9f, 0.5f)
            g.color(tint(lk.acc)); g.box(0f, 23.5f, 4.1f, 1.5f, 8f, 0.4f)
            g.color(tint(lk.cloth)); g.box(0f, 20f, 0f, 11f, 1.2f, 7.4f)
        } else {
            g.color(tint(lk.vest))
            g.sphere(0f, 23f, 0.3f, 6.9f, 6.0f, 4.65f)
            g.color(tint(mix(lk.vest, BLACK, 0.45f)))
            g.box(-3.2f, 21f, 4.2f, 3.4f, 3.6f, 1.8f)
            g.box(3.2f, 21f, 4.2f, 3.4f, 3.6f, 1.8f)
            g.box(0f, 18.4f, 0f, 11.6f, 1.2f, 7.6f)
        }
        if (lk.epaulet != 0) {
            g.color(tint(lk.epaulet)); g.box(5.8f, 29.4f, 0f, 3.6f, 1.1f, 5f); g.box(-5.8f, 29.4f, 0f, 3.6f, 1.1f, 5f)
        }
        // arms
        val armed = gun in 0..11
        for (side in 0..1) {
            val sx = if (side == 0) 7.6f else -7.6f
            g.push()
            g.translate(sx, 27.4f, 0f)
            var elbow: Float
            if (dance) {
                g.rotX(-165f * armUp + (if (side == 0) 10f else -10f))
                g.rotZ(if (side == 0) -18f else 18f)
                elbow = -20f
            } else if (armed) {
                if (side == 0) { g.rotX(-48f - 10f * kick); g.rotZ(-6f); elbow = -56f }
                else { g.rotX(-62f - 8f * kick); g.rotZ(22f); elbow = -50f }
            } else {
                g.rotX(-sw * 30f * (if (side == 0) 1f else -1f))
                elbow = -12f - max(0f, sw * (if (side == 0) 1f else -1f)) * 30f
            }
            if (camoT != 0) g.tex(camoT, 1f, 1f)
            g.color(tint(if (camoT != 0) k(255, 255, 255) else lk.cloth))
            g.limb(8.4f, 2.5f)
            g.translate(0f, -8.4f, 0f)
            g.rotX(elbow)
            g.limb(7.4f, 2.1f, false)
            g.noTex()
            g.color(tint(lk.skin))
            g.sphere(0f, -8.1f, 0f, 1.9f, 2.2f, 1.9f)
            g.pop()
        }
        // weapon
        if (armed && !dance) {
            g.push()
            g.translate(0f, 22.2f, 5.4f - kick * 1.8f)
            g.rotX(-kick * 6f)
            drawGun(g, gun, muzzle)
            g.pop()
        }
        // neck and head
        g.color(tint(lk.skin))
        g.cylinder(0f, 30.4f, 0.3f, 2.2f, 3.4f)
        val faceT = g.faceTex[li]
        g.color(tint(lk.skin))
        g.sphere(0f, 34f, 0f, 4.5f, 5.3f, 4.7f)
        if (faceT != 0) {
            g.tex(faceT, 1f, 1f)
            g.color(tint(k(255, 255, 255)))
            g.facePatch(0f, 34f, 0.04f, 4.55f, 5.35f, 4.75f)
            g.noTex()
        }
        // ears
        g.color(tint(lk.skin))
        g.sphere(4.5f, 33.6f, -0.2f, 0.9f, 1.8f, 1.2f)
        g.sphere(-4.5f, 33.6f, -0.2f, 0.9f, 1.8f, 1.2f)
        // nose
        if (lk.bigNose) {
            g.color(tint(lk.skinHi)); g.sphere(0f, 33.2f, 4.9f, 1.5f, 2.2f, 1.8f)
            g.sphere(1.1f, 32.4f, 4.5f, 0.9f, 0.8f, 0.9f); g.sphere(-1.1f, 32.4f, 4.5f, 0.9f, 0.8f, 0.9f)
        } else {
            g.color(tint(lk.skinHi)); g.sphere(0f, 33.1f, 4.75f, 0.9f, 1.5f, 1.1f)
        }
        if (lk.shades) { g.color(BLACK); g.box(0f, 34.6f, 4.5f, 8.2f, 1.8f, 0.8f) }
        // beard volume
        if (lk.beard == 2) {
            g.color(lk.hair)
            g.sphere(0f, 30.6f, 2.6f, 3.9f, 3.0f, 2.7f)
            g.sphere(3.3f, 32.2f, 2.0f, 1.2f, 2.6f, 2.2f); g.sphere(-3.3f, 32.2f, 2.0f, 1.2f, 2.6f, 2.2f)
        }
        // hair
        if (lk.hairLong == 1) {
            g.color(lk.hair)
            g.sphere(0f, 35.6f, -0.8f, 4.9f, 4.0f, 5.0f)
            g.sphere(0f, 31.5f, -3.6f, 4.4f, 5.6f, 2.0f)
            for (i in 0 until 5) {
                val hx = (i - 2) * 1.9f
                g.push()
                g.translate(hx, 30f, -4.2f - (i % 2) * 0.5f)
                g.rotX(6f + (i % 3) * 3f)
                g.limb(9f + (i % 2) * 2f, 0.85f, false)
                g.pop()
            }
        } else if (lk.hat == 0 || lk.hat == 2) {
            g.color(lk.hair)
            g.sphere(0f, 35.8f, -0.6f, 4.7f, 3.6f, 5f)
        }
        // headgear
        when (lk.hat) {
            1 -> { g.color(tint(lk.hatCol)); g.sphere(0f, 35.8f, 0f, 5.5f, 4.2f, 5.8f); g.cylinder(0f, 34.3f, 0f, 5.9f, 0.8f) }
            2 -> { g.color(tint(lk.hatCol)); g.cylinder(0f, 37f, -0.5f, 5.8f, 2f); g.sphere(1f, 37f, -1f, 6.2f, 2f, 6.2f) }
            3 -> { g.color(tint(lk.hatCol)); g.cylinder(0f, 37.6f, 0f, 5.6f, 3.4f); g.color(BLACK); g.box(0f, 36f, 4.8f, 9f, 0.9f, 3.4f); g.color(tint(lk.acc)); g.box(0f, 37.6f, 5.6f, 2f, 1.6f, 0.6f) }
            4 -> { g.color(tint(lk.hatCol)); g.cylinder(0f, 36.6f, 0f, 8.2f, 0.8f); g.sphere(0f, 38f, 0f, 4.8f, 3f, 4.8f) }
            5 -> {
                g.color(tint(lk.hatCol)); g.sphere(0f, 36.8f, 0f, 6.5f, 4.7f, 6.7f)
                g.color(tint(mix(lk.hatCol, BLACK, 0.18f))); g.cylinder(0f, 34.9f, 0f, 6.0f, 2.2f)
                g.color(tint(mix(lk.hatCol, BLACK, 0.3f))); g.sphere(4.6f, 31.5f, 3.2f, 1.8f, 4.2f, 1.8f)
            }
            else -> {}
        }
        g.pop()
        g.pop()
    }


    // ------------------------------------------------------------------ vehicles
    fun vehicle(
        g: Gfx, type: Int, x: Float, y: Float, z: Float, heading: Float, turret: Float,
        wheel: Float, muzzle: Float, flash: Float, dead: Boolean, hpFrac: Float
    ) {
        fun tint(c0: Int): Int {
            var c = c0
            if (dead) c = mix(c, k(20, 20, 20), 0.75f)
            else if (hpFrac < 0.4f) c = mix(c, k(40, 36, 32), (0.4f - hpFrac) * 1.2f)
            return if (flash > 0f) mix(c, RED, min(1f, flash * 1.6f)) else c
        }
        g.push()
        g.translate(x, y, z)
        g.rotY(yawDeg(heading))
        val body: Int
        val cab: Int
        when (type) {
            0 -> { body = k(86, 92, 62); cab = k(70, 78, 52) }
            1 -> { body = k(176, 150, 104); cab = k(150, 126, 86) }
            2 -> { body = k(150, 130, 96); cab = k(124, 106, 76) }
            else -> { body = k(200, 204, 210); cab = k(168, 174, 184) }
        }
        val big = type == 3
        val len = if (big) 62f else 50f
        val wid = if (big) 28f else 22f
        val dk = tint(mix(body, BLACK, 0.55f))
        val wr = if (big) 6.6f else 5.6f
        // underframe, axles and leaf springs
        g.color(dk)
        g.box(0f, 6.2f, 0f, wid - 6f, 2.6f, len * 0.96f)
        g.color(tint(k(46, 46, 48)))
        g.push(); g.translate(0f, wr, len * 0.32f); g.rotZ(90f); g.cylinder(0f, 0f, 0f, 1.2f, wid + 1f); g.pop()
        g.push(); g.translate(0f, wr, -len * 0.32f); g.rotZ(90f); g.cylinder(0f, 0f, 0f, 1.2f, wid + 1f); g.pop()
        // lower body with fender flares
        g.color(tint(body))
        g.box(0f, 10.4f, -len * 0.1f, wid, 4.6f, len * 0.78f)
        for (sx in 0..1) {
            val fx = if (sx == 0) wid * 0.5f - 0.6f else -wid * 0.5f + 0.6f
            g.color(tint(mix(body, BLACK, 0.2f)))
            g.box(fx, 13.2f, len * 0.32f, 2.8f, 1.2f, wr * 2.6f)
            g.box(fx, 13.2f, -len * 0.32f, 2.8f, 1.2f, wr * 2.6f)
        }
        // bed: side walls, tailgate, floor
        g.color(tint(mix(body, BLACK, 0.15f)))
        g.box(0f, 12.6f, -len * 0.3f, wid - 1f, 0.8f, len * 0.42f)
        g.color(tint(body))
        g.box(wid * 0.5f - 0.5f, 15f, -len * 0.3f, 1.1f, 4.4f, len * 0.42f)
        g.box(-wid * 0.5f + 0.5f, 15f, -len * 0.3f, 1.1f, 4.4f, len * 0.42f)
        g.box(0f, 15f, -len * 0.5f + 0.5f, wid - 1f, 4.4f, 1.1f)
        // hood (slightly sloped), grille and bumper
        g.color(tint(cab))
        g.push(); g.translate(0f, 13.6f, len * 0.37f); g.rotX(5f); g.box(0f, 0f, 0f, wid - 2.5f, 3.2f, len * 0.26f); g.pop()
        g.color(tint(mix(cab, BLACK, 0.2f)))
        g.box(0f, 11f, len * 0.5f - 0.4f, wid - 4f, 5f, 1.4f)
        g.color(tint(k(40, 42, 44)))
        for (gi in 0..3) g.box(0f, 9.6f + gi * 1.2f, len * 0.5f + 0.5f, wid - 8f, 0.5f, 0.5f)
        g.color(tint(BLACK))
        g.box(0f, 7.8f, len * 0.5f + 0.6f, wid + 0.5f, 2.2f, 2.2f)
        // cab: pillars, windshield, roof, side glass
        g.color(tint(cab))
        g.box(0f, 16.4f, len * 0.13f, wid - 2.5f, 5.6f, len * 0.24f)
        g.color(tint(k(36, 50, 62)))
        g.push(); g.translate(0f, 18.6f, len * 0.255f); g.rotX(-22f); g.box(0f, 0f, 0f, wid - 5.5f, 5.2f, 0.7f); g.pop()
        g.box(wid * 0.5f - 1.3f, 18.6f, len * 0.13f, 0.6f, 4.2f, len * 0.17f)
        g.box(-wid * 0.5f + 1.3f, 18.6f, len * 0.13f, 0.6f, 4.2f, len * 0.17f)
        g.color(tint(mix(cab, BLACK, 0.12f)))
        g.box(0f, 21.4f, len * 0.1f, wid - 3.5f, 0.9f, len * 0.26f)
        g.color(tint(mix(cab, BLACK, 0.4f)))
        g.box(wid * 0.5f - 1.1f, 15.8f, len * 0.13f, 0.4f, 0.4f, len * 0.22f)
        g.box(-wid * 0.5f + 1.1f, 15.8f, len * 0.13f, 0.4f, 0.4f, len * 0.22f)
        // mirrors
        g.color(tint(BLACK))
        g.box(wid * 0.5f + 1.3f, 18f, len * 0.26f, 1.6f, 2.2f, 0.9f)
        g.box(-wid * 0.5f - 1.3f, 18f, len * 0.26f, 1.6f, 2.2f, 0.9f)
        // headlights (round) and tail lights
        g.unlit(true)
        g.color(k(255, 244, 190))
        g.barrel(wid * 0.33f, 11.4f, len * 0.5f + 0.5f, len * 0.5f + 1.3f, 1.5f)
        g.barrel(-wid * 0.33f, 11.4f, len * 0.5f + 0.5f, len * 0.5f + 1.3f, 1.5f)
        g.color(k(190, 30, 24))
        g.box(wid * 0.42f, 13.2f, -len * 0.5f - 0.3f, 2.2f, 1.6f, 0.5f)
        g.box(-wid * 0.42f, 13.2f, -len * 0.5f - 0.3f, 2.2f, 1.6f, 0.5f)
        g.unlit(false)
        // exhaust and fuel can
        g.color(tint(k(70, 66, 62)))
        g.barrel(-wid * 0.5f + 2.5f, 6f, -len * 0.5f - 3f, -len * 0.1f, 0.9f)
        g.color(tint(k(84, 96, 62)))
        g.box(wid * 0.5f + 0.6f, 9.6f, -len * 0.02f, 1.4f, 3.4f, 2.8f)
        // roll bar on the military pick-ups
        if (type != 3) {
            g.color(tint(k(40, 42, 44)))
            g.cylinder(wid * 0.5f - 1.5f, 19f, -len * 0.06f, 0.55f, 12f)
            g.cylinder(-wid * 0.5f + 1.5f, 19f, -len * 0.06f, 0.55f, 12f)
            g.barrel(0f, 25f, -len * 0.06f, -len * 0.06f + 0.1f, 0.5f)
            g.push(); g.translate(0f, 25f, -len * 0.06f); g.rotZ(90f); g.cylinder(0f, 0f, 0f, 0.55f, wid - 3f); g.pop()
        }
        // spare tyre on the bed (technical and ally pick-up)
        if (type == 0 || type == 2) {
            g.color(tint(k(24, 24, 26)))
            g.push(); g.translate(0f, 17f, -len * 0.47f); g.rotX(90f); g.cylinder(0f, 0f, 0f, wr * 0.9f, 2.6f); g.pop()
        }
        // wheels: tyre, sidewall, rim, hub and tread blocks that turn
        for (i in 0..3) {
            val side = if (i % 2 == 0) 1f else -1f
            val wx = side * (wid * 0.5f + 0.9f)
            val wz = if (i < 2) len * 0.32f else -len * 0.32f
            g.color(tint(k(22, 22, 24)))
            g.wheel(wx, wr, wz, wr, 4.6f)
            g.color(tint(k(40, 40, 42)))
            g.wheel(wx + side * 1.8f, wr, wz, wr * 0.86f, 1.2f)
            g.color(tint(k(170, 172, 176)))
            g.wheel(wx + side * 2.5f, wr, wz, wr * 0.55f, 0.6f)
            g.color(tint(k(80, 80, 84)))
            g.wheel(wx + side * 2.9f, wr, wz, wr * 0.2f, 0.7f)
            g.color(tint(k(16, 16, 18)))
            for (j in 0 until 10) {
                g.push()
                g.translate(wx, wr, wz)
                g.rotX(wheel * 57f + j * 36f)
                g.box(0f, wr, 0f, 4.8f, 1.1f, 2.1f)
                g.pop()
            }
            g.color(tint(k(110, 110, 114)))
            g.push(); g.translate(wx + side * 2.9f, wr, wz); g.rotX(wheel * 57f)
            g.box(0f, 0f, 0f, 0.5f, wr * 1.0f, 0.7f); g.box(0f, 0f, 0f, 0.5f, 0.7f, wr * 1.0f); g.pop()
        }
        val delta = turret - heading
        when (type) {
            0 -> {
                // technical: machine gun on a pedestal behind the cab
                g.color(tint(METAL)); g.box(0f, 15f, -len * 0.2f, 4f, 8f, 4f)
                g.push(); g.translate(0f, 22f, -len * 0.2f); g.rotY(-delta * RAD2DEG)
                g.color(tint(METAL)); g.box(0f, 0f, 3f, 4f, 3f, 9f)
                g.color(tint(METAL2)); g.barrel(0f, 0.4f, 7f, 17f, 0.8f)
                g.color(tint(k(60, 80, 50))); g.box(0f, 0.5f, 5f, 9f, 5f, 0.8f)
                if (muzzle > 0f) { g.unlit(true); g.color(k(255, 220, 110)); g.sphere(0f, 0.4f, 19f, 3f, 3f, 5f); g.unlit(false) }
                g.pop()
                // sandbag crate in the bed
                g.color(tint(k(110, 100, 70))); g.box(0f, 15f, -len * 0.38f, wid - 6f, 5f, 8f)
            }
            1 -> {
                // 23 mm twin cannon on the bed
                g.color(tint(METAL)); g.cylinder(0f, 15f, -len * 0.18f, 4.6f, 6f)
                g.push(); g.translate(0f, 20f, -len * 0.18f); g.rotY(-delta * RAD2DEG)
                g.color(tint(k(90, 98, 70))); g.box(0f, 1f, 1.5f, 12f, 9f, 5f)
                g.color(tint(METAL)); g.box(0f, -2f, 0f, 9f, 3f, 8f)
                g.color(tint(METAL2)); g.barrel(3.2f, 1.2f, 3f, 30f, 1.5f); g.barrel(-3.2f, 1.2f, 3f, 30f, 1.5f)
                g.color(tint(METAL)); g.box(3.2f, 1.2f, 30f, 2.6f, 2.6f, 3.5f); g.box(-3.2f, 1.2f, 30f, 2.6f, 2.6f, 3.5f)
                g.color(tint(k(60, 64, 50))); g.box(0f, -3f, -6f, 4f, 4f, 7f)
                if (muzzle > 0f) { g.unlit(true); g.color(k(255, 220, 110)); g.sphere(3.2f, 1.2f, 34f, 3.6f, 3.6f, 6f); g.sphere(-3.2f, 1.2f, 34f, 3.6f, 3.6f, 6f); g.unlit(false) }
                g.pop()
            }
            2 -> {
                g.color(tint(k(120, 106, 76))); g.box(0f, 15f, -len * 0.3f, wid - 4f, 5f, 12f)
            }
            else -> {
                g.color(tint(k(160, 166, 174))); g.box(0f, 16f, -len * 0.18f, wid - 2f, 9f, len * 0.5f)
                g.color(tint(k(110, 116, 126))); g.box(0f, 22f, -len * 0.18f, wid - 8f, 3f, len * 0.4f)
                g.color(tint(METAL)); g.push(); g.translate(0f, 24f, -len * 0.18f); g.rotY(-delta * RAD2DEG)
                g.box(0f, 0f, 3f, 4f, 3f, 8f); g.barrel(0f, 0.3f, 6f, 15f, 0.7f); g.pop()
            }
        }
        g.pop()
    }

    // ------------------------------------------------------------------ aircraft
    fun plane(g: Gfx, x: Float, y: Float, z: Float, heading: Float, flash: Float, t: Float) {
        g.push()
        g.translate(x, y, z)
        g.rotY(yawDeg(heading))
        g.scale(1.5f)
        val body = if (flash > 0f) mix(k(110, 116, 122), RED, min(1f, flash * 1.6f)) else k(110, 116, 122)
        g.color(body)
        g.barrel(0f, 0f, -16f, 16f, 2.8f)
        g.push(); g.translate(0f, 0f, 20f); g.rotX(90f); g.coneM(0f, 0f, 0f, 2.8f, 8f); g.pop()
        g.color(mix(body, BLACK, 0.2f))
        g.box(0f, -0.4f, 1f, 36f, 0.9f, 9f)
        g.box(0f, 0.8f, -15f, 13f, 0.8f, 5f)
        g.box(0f, 4.2f, -15f, 0.9f, 7f, 5.5f)
        g.color(k(40, 70, 100)); g.sphere(0f, 2.2f, 7f, 1.9f, 1.6f, 4f)
        g.unlit(true)
        g.color(k(255, 160, 60)); g.sphere(0f, 0f, -18.5f, 1.8f, 1.8f, 3f + 1.4f * sin(t * 30f))
        if (sin(t * 6f) > 0f) { g.color(k(255, 60, 60)); g.sphere(17f, 1f, 1f, 0.9f, 0.9f, 0.9f) } else { g.color(k(80, 255, 90)); g.sphere(-17f, 1f, 1f, 0.9f, 0.9f, 0.9f) }
        g.unlit(false)
        g.pop()
    }

    // ------------------------------------------------------------------ buildings
    /**
     * type 0 house, 1 control room, 2 water tank, 3 tent, 4 earth rampart, 5 depot, 6 table.
     * (x, z) = centre of the footprint, w = size along X, d = size along Z, h = height.
     */
    fun building(g: Gfx, type: Int, x: Float, z: Float, w: Float, d: Float, h: Float, hpFrac: Float, flash: Float, dead: Boolean, t: Float) {
        fun tint(c0: Int): Int {
            var c = c0
            if (hpFrac < 0.5f) c = mix(c, k(50, 46, 42), (0.5f - hpFrac) * 0.9f)
            return if (flash > 0f) mix(c, RED, min(1f, flash * 2f)) else c
        }
        g.push()
        g.translate(x, 0f, z)
        if (dead) {
            // rubble
            g.color(k(74, 68, 62)); g.box(0f, h * 0.07f, 0f, w, h * 0.14f, d)
            g.color(k(56, 52, 48)); g.box(-w * 0.2f, h * 0.2f, d * 0.1f, w * 0.3f, h * 0.2f, d * 0.3f)
            g.box(w * 0.25f, h * 0.16f, -d * 0.1f, w * 0.25f, h * 0.16f, d * 0.3f)
            g.pop()
            return
        }
        when (type) {
            0 -> {
                g.tex(g.sandTex, w / 50f, h / 50f)
                g.color(tint(k(226, 202, 160))); g.box(0f, h / 2f, 0f, w, h, d)
                g.color(tint(k(190, 160, 118))); g.box(0f, h * 0.07f, 0f, w + 1.4f, h * 0.14f, d + 1.4f)
                g.color(tint(k(200, 172, 128))); g.box(0f, h + 1.5f, 0f, w + 4f, 3f, d + 4f)
                g.noTex()
                // parapet corners and roof details
                g.color(tint(k(176, 146, 104)))
                g.box(w * 0.5f, h + 4f, d * 0.5f, 3.4f, 5f, 3.4f); g.box(-w * 0.5f, h + 4f, d * 0.5f, 3.4f, 5f, 3.4f)
                g.box(w * 0.5f, h + 4f, -d * 0.5f, 3.4f, 5f, 3.4f); g.box(-w * 0.5f, h + 4f, -d * 0.5f, 3.4f, 5f, 3.4f)
                g.color(tint(k(120, 118, 114))); g.cylinder(w * 0.28f, h + 6f, -d * 0.2f, 3.4f, 6f)
                // arched wooden door with frame
                g.color(tint(k(110, 88, 64))); g.box(0f, h * 0.38f, d / 2f + 0.2f, w * 0.24f, h * 0.76f, 0.6f)
                g.color(tint(k(62, 44, 32))); g.box(0f, h * 0.35f, d / 2f + 0.7f, w * 0.19f, h * 0.7f, 0.6f)
                g.color(tint(k(200, 170, 70))); g.sphere(w * 0.06f, h * 0.36f, d / 2f + 1.2f, 0.8f, 0.8f, 0.6f)
                // windows with frames and sills
                for (sx in intArrayOf(-1, 1)) {
                    val wx = sx * w * 0.3f
                    g.color(tint(k(236, 222, 190))); g.box(wx, h * 0.62f, d / 2f + 0.2f, w * 0.2f, h * 0.27f, 0.6f)
                    g.color(tint(k(30, 40, 50))); g.box(wx, h * 0.62f, d / 2f + 0.7f, w * 0.15f, h * 0.2f, 0.5f)
                    g.color(tint(k(236, 222, 190))); g.box(wx, h * 0.62f, d / 2f + 1.0f, w * 0.15f, 0.5f, 0.4f); g.box(wx, h * 0.62f, d / 2f + 1.0f, 0.5f, h * 0.2f, 0.4f)
                    g.color(tint(k(180, 150, 110))); g.box(wx, h * 0.47f, d / 2f + 1.3f, w * 0.23f, 0.9f, 1.8f)
                }
            }
            1 -> {
                g.tex(g.sandTex, w / 50f, h / 50f)
                g.color(tint(k(166, 170, 172))); g.box(0f, h / 2f, 0f, w, h, d)
                g.color(tint(k(110, 116, 120))); g.box(0f, h + 1.5f, 0f, w + 4f, 3f, d + 4f)
                g.noTex()
                g.color(tint(k(78, 84, 90)))
                for (wi in 0..2) { g.box(-w * 0.34f + wi * w * 0.34f, h * 0.8f, d / 2f + 0.3f, w * 0.2f, h * 0.12f, 0.6f); g.box(-w * 0.34f + wi * w * 0.34f, h * 0.8f, -d / 2f - 0.3f, w * 0.2f, h * 0.12f, 0.6f) }
                g.color(tint(k(60, 64, 68))); g.box(0f, h * 0.05f, 0f, w + 2f, h * 0.1f, d + 2f)
                g.color(tint(k(40, 56, 72))); g.box(0f, h * 0.55f, d / 2f + 0.3f, w * 0.7f, h * 0.22f, 0.8f)
                g.color(tint(k(70, 76, 82))); g.box(0f, h * 0.28f, d / 2f + 0.3f, w * 0.16f, h * 0.5f, 0.8f)
                g.color(tint(k(210, 214, 218)))
                g.push(); g.translate(-w * 0.22f, h + 8f, 0f); g.rotZ(-35f); g.sphere(0f, 0f, 0f, 11f, 3f, 11f); g.pop()
                g.color(METAL2); g.cylinder(-w * 0.22f, h + 4f, 0f, 1.2f, 6f)
                g.cylinder(w * 0.3f, h + 16f, -d * 0.2f, 0.9f, 28f)
                g.unlit(true)
                if (sin(t * 5f) > 0f) g.color(k(255, 50, 50)) else g.color(k(110, 20, 20))
                g.sphere(w * 0.3f, h + 31f, -d * 0.2f, 2f, 2f, 2f)
                g.unlit(false)
            }
            2 -> {
                g.color(tint(k(96, 100, 104)))
                g.box(-w * 0.35f, h * 0.3f, -d * 0.3f, 3f, h * 0.6f, 3f); g.box(w * 0.35f, h * 0.3f, -d * 0.3f, 3f, h * 0.6f, 3f)
                g.box(-w * 0.35f, h * 0.3f, d * 0.3f, 3f, h * 0.6f, 3f); g.box(w * 0.35f, h * 0.3f, d * 0.3f, 3f, h * 0.6f, 3f)
                g.color(tint(k(150, 160, 168))); g.cylinder(0f, h * 0.78f, 0f, w * 0.5f, h * 0.44f)
                g.color(tint(k(120, 130, 138))); g.coneM(0f, h * 1.05f, 0f, w * 0.5f, h * 0.16f)
                g.color(tint(k(70, 76, 82))); g.cylinder(0f, h * 0.3f, d * 0.15f, 2.2f, h * 0.6f)
                // the valve wheel
                g.color(k(210, 40, 36)); g.push(); g.translate(0f, 8f, d * 0.5f + 2f); g.rotX(90f); g.cylinder(0f, 0f, 0f, 6f, 1.6f); g.pop()
                g.color(k(150, 24, 22)); g.box(0f, 8f, d * 0.5f + 2f, 12f, 1.4f, 1.4f); g.box(0f, 8f, d * 0.5f + 2f, 1.4f, 12f, 1.4f)
                g.color(tint(k(70, 76, 82))); g.box(0f, 8f, d * 0.2f, 3f, 3f, d * 0.6f)
            }
            3 -> {
                g.tex(g.sandTex, w / 40f, h / 40f)
                g.color(tint(k(186, 170, 120))); g.box(0f, h * 0.3f, 0f, w, h * 0.6f, d)
                g.noTex()
                g.color(tint(k(120, 106, 74)))
                g.box(w * 0.5f, h * 0.3f, d * 0.5f, 1.2f, h * 0.7f, 1.2f); g.box(-w * 0.5f, h * 0.3f, d * 0.5f, 1.2f, h * 0.7f, 1.2f)
                g.color(tint(k(150, 134, 92))); g.pyramid(0f, h * 0.6f + h * 0.25f, 0f, w, h * 0.5f, d)
                g.color(tint(k(50, 42, 30))); g.box(0f, h * 0.26f, d / 2f + 0.3f, w * 0.26f, h * 0.5f, 0.8f)
            }
            4 -> {
                g.tex(g.sandTex, w / 60f, h / 40f)
                g.color(tint(k(184, 150, 106))); g.box(0f, h * 0.35f, 0f, w, h * 0.7f, d)
                g.color(tint(k(160, 128, 88))); g.box(0f, h * 0.8f, 0f, w * 0.92f, h * 0.3f, d * 0.7f)
                g.noTex()
                g.color(tint(k(120, 100, 76)))
                var i = 0
                while (i < 8) {
                    val rx = (i - 3.5f) * (w / 8f)
                    g.sphere(rx, h * 0.98f, d * 0.1f * ((i % 3) - 1), 7f, 4f, 6f)
                    i++
                }
            }
            5 -> {
                g.tex(g.sandTex, w / 50f, h / 50f)
                g.color(tint(k(176, 172, 154))); g.box(0f, h / 2f, 0f, w, h, d)
                g.noTex()
                g.color(tint(k(90, 94, 80))); g.box(w * 0.3f, h * 0.3f, d / 2f + 2f, 6f, 6f, 4f); g.box(w * 0.38f, h * 0.2f, d / 2f + 4f, 6f, 5f, 4f)
                g.color(tint(k(110, 106, 96))); g.box(0f, h + 2f, 0f, w + 3f, 4f, d + 3f)
                g.color(tint(k(80, 80, 76))); g.box(0f, h * 0.38f, d / 2f + 0.3f, w * 0.5f, h * 0.76f, 0.8f)
            }
            else -> {
                // treaty table
                g.color(k(120, 80, 48)); g.box(0f, 14f, 0f, w, 3f, d)
                g.color(k(90, 58, 34)); g.box(-w * 0.42f, 7f, 0f, 3f, 14f, d * 0.8f); g.box(w * 0.42f, 7f, 0f, 3f, 14f, d * 0.8f)
                g.color(k(240, 236, 224)); g.box(0f, 15.8f, 0f, w * 0.45f, 0.5f, d * 0.6f)
            }
        }
        g.pop()
    }

    fun cover(g: Gfx, kind: Int, x: Float, z: Float, w: Float, d: Float, h: Float) {
        g.push()
        g.translate(x, 0f, z)
        if (kind == 0) {
            var i = 0
            val n = max(2, (w / 12f).toInt())
            while (i < n) {
                val bx = -w / 2f + (i + 0.5f) * (w / n)
                g.color(if (i % 2 == 0) k(204, 176, 122) else k(184, 154, 104))
                g.box(bx, h * 0.25f, 0f, w / n - 0.8f, h * 0.5f, d)
                g.color(if (i % 2 == 0) k(184, 154, 104) else k(204, 176, 122))
                g.box(bx + w / (2f * n), h * 0.75f, 0f, w / n - 0.8f, h * 0.5f, d * 0.9f)
                i++
            }
        } else if (kind == 1) {
            g.color(k(176, 176, 172)); g.box(0f, h * 0.35f, 0f, w, h * 0.7f, d)
            g.color(k(150, 150, 148)); g.box(0f, h * 0.85f, 0f, w * 0.7f, h * 0.3f, d * 0.7f)
        } else {
            g.color(k(86, 62, 48)); g.box(0f, h * 0.35f, 0f, w, h * 0.5f, d)
            g.color(k(70, 52, 42)); g.box(0f, h * 0.7f, -d * 0.05f, w * 0.55f, h * 0.35f, d * 0.8f)
            g.color(k(22, 22, 24)); g.wheel(w * 0.5f, 4f, d * 0.3f, 4.4f, 3f); g.wheel(-w * 0.5f, 4f, d * 0.3f, 4.4f, 3f)
        }
        g.pop()
    }

    // ------------------------------------------------------------------ scenery
    fun rock(g: Gfx, x: Float, z: Float, size: Float, shade: Int) {
        g.color(mix(k(138, 118, 94), k(96, 82, 66), shade / 255f))
        g.sphere(x, size * 0.3f, z, size, size * 0.6f, size * 0.85f)
        g.color(mix(k(160, 140, 112), k(120, 102, 84), shade / 255f))
        g.sphere(x + size * 0.4f, size * 0.22f, z + size * 0.2f, size * 0.55f, size * 0.4f, size * 0.5f)
    }

    fun tuft(g: Gfx, x: Float, z: Float, s: Float) {
        g.color(k(150, 138, 84))
        g.box(x, s * 0.5f, z, 0.7f, s, 0.7f)
        g.box(x + 1.2f, s * 0.4f, z + 0.4f, 0.6f, s * 0.8f, 0.6f)
        g.box(x - 1.1f, s * 0.45f, z - 0.3f, 0.6f, s * 0.9f, 0.6f)
    }

    fun tree(g: Gfx, x: Float, z: Float, s: Float) {
        g.color(k(86, 62, 44)); g.cylinder(x, s * 0.5f, z, s * 0.07f, s)
        g.color(k(110, 118, 60)); g.sphere(x, s * 1.12f, z, s * 0.62f, s * 0.2f, s * 0.62f)
        g.color(k(96, 108, 52)); g.sphere(x + s * 0.2f, s * 1.0f, z - s * 0.1f, s * 0.4f, s * 0.14f, s * 0.4f)
    }

    fun absf(v: Float) = abs(v)
}
