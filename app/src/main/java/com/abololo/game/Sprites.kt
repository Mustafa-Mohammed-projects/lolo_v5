package com.abololo.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** How a soldier looks. Every faction and every commander has its own Look. */
class Look(
    val skin: Int, val skinHi: Int, val cloth: Int, val clothD: Int, val vest: Int,
    val hat: Int,        // 0 none, 1 helmet, 2 beret, 3 officer cap, 4 boonie hat, 5 turban
    val hatCol: Int, val eye: Int, val bigNose: Boolean,
    val beard: Int,      // 0 none, 1 mustache/stubble, 2 full beard
    val hair: Int, val shades: Boolean, val suit: Boolean, val acc: Int, val scale: Float,
    val hairLong: Int = 0,   // 0 short, 1 long hair / dreadlocks
    val camo: Int = 0,       // colour of camouflage patches (0 = none)
    val epaulet: Int = 0     // colour of shoulder boards (0 = none)
)

object Looks {
    private fun k(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    // Janja forces (white, tough soldiers) – the player's side
    val JANJA = Look(
        skin = k(238, 206, 182), skinHi = k(252, 228, 208), cloth = k(150, 134, 96), clothD = k(112, 98, 68),
        vest = k(92, 98, 68), hat = 4, hatCol = k(160, 142, 100), eye = k(60, 40, 30), bigNose = false,
        beard = 1, hair = k(60, 44, 32), shades = false, suit = false, acc = k(150, 134, 96), scale = 1f
    )
    val JANJA_HELMET = Look(
        skin = k(238, 206, 182), skinHi = k(252, 228, 208), cloth = k(150, 134, 96), clothD = k(112, 98, 68),
        vest = k(92, 98, 68), hat = 1, hatCol = k(118, 112, 78), eye = k(60, 40, 30), bigNose = false,
        beard = 0, hair = k(60, 44, 32), shades = false, suit = false, acc = k(150, 134, 96), scale = 1f
    )
    // Falanqayat: black skin, red eyes, big nose, green military uniform
    val FALAN = Look(
        skin = k(30, 24, 24), skinHi = k(70, 54, 50), cloth = k(54, 88, 48), clothD = k(36, 62, 34),
        vest = k(36, 46, 36), hat = 1, hatCol = k(62, 94, 56), eye = k(240, 30, 30), bigNose = true,
        beard = 0, hair = k(8, 8, 10), shades = false, suit = false, acc = k(54, 88, 48), scale = 1f
    )
    val FALAN_BARE = Look(
        skin = k(30, 24, 24), skinHi = k(70, 54, 50), cloth = k(54, 88, 48), clothD = k(36, 62, 34),
        vest = k(36, 46, 36), hat = 0, hatCol = 0, eye = k(240, 30, 30), bigNose = true,
        beard = 0, hair = k(8, 8, 10), shades = false, suit = false, acc = k(54, 88, 48), scale = 1f
    )
    // Elites: look like the Janja but wear military or civilian suits and hold the posts
    val ELITE_UNI = Look(
        skin = k(232, 200, 176), skinHi = k(250, 226, 206), cloth = k(72, 80, 98), clothD = k(50, 56, 72),
        vest = k(52, 56, 68), hat = 3, hatCol = k(52, 58, 78), eye = k(60, 40, 30), bigNose = false,
        beard = 1, hair = k(40, 32, 28), shades = false, suit = false, acc = k(226, 186, 70), scale = 1f
    )
    val ELITE_SUIT = Look(
        skin = k(232, 200, 176), skinHi = k(250, 226, 206), cloth = k(38, 38, 46), clothD = k(24, 24, 30),
        vest = k(38, 38, 46), hat = 0, hatCol = 0, eye = k(60, 40, 30), bigNose = false,
        beard = 1, hair = k(40, 32, 28), shades = false, suit = true, acc = k(200, 40, 44), scale = 1f
    )
    // Elite officers who defect to the Janja side (green armband)
    val DEFECTOR = Look(
        skin = k(232, 200, 176), skinHi = k(250, 226, 206), cloth = k(72, 80, 98), clothD = k(50, 56, 72),
        vest = k(52, 56, 68), hat = 3, hatCol = k(52, 58, 78), eye = k(60, 40, 30), bigNose = false,
        beard = 1, hair = k(40, 32, 28), shades = false, suit = false, acc = k(70, 200, 100), scale = 1f
    )
    // Dinari forces: very pale skin, soft features, light uniforms
    val DINARI = Look(
        skin = k(250, 234, 222), skinHi = k(255, 246, 238), cloth = k(206, 210, 216), clothD = k(168, 176, 188),
        vest = k(150, 162, 176), hat = 1, hatCol = k(228, 230, 236), eye = k(80, 120, 178), bigNose = false,
        beard = 0, hair = k(210, 186, 120), shades = false, suit = false, acc = k(120, 150, 190), scale = 1f
    )
    val EXPERT = Look(
        skin = k(226, 190, 160), skinHi = k(244, 214, 188), cloth = k(86, 98, 110), clothD = k(62, 72, 84),
        vest = k(240, 134, 40), hat = 1, hatCol = k(242, 202, 54), eye = k(50, 34, 26), bigNose = false,
        beard = 0, hair = k(50, 36, 28), shades = false, suit = false, acc = k(240, 134, 40), scale = 1f
    )
    // Commanders (colours sampled from the reference picture)
    val SAVANNA = Look(
        skin = k(109, 68, 48), skinHi = k(140, 92, 66), cloth = k(190, 160, 116), clothD = k(150, 120, 84),
        vest = k(176, 146, 100), hat = 5, hatCol = k(194, 156, 104), eye = k(30, 20, 16), bigNose = false,
        beard = 2, hair = k(24, 18, 16), shades = false, suit = false, acc = k(200, 170, 110), scale = 1.22f,
        hairLong = 1, camo = k(150, 112, 74)
    )
    val QUBBA = Look(
        skin = k(140, 90, 65), skinHi = k(170, 116, 86), cloth = k(98, 108, 72), clothD = k(66, 76, 50),
        vest = k(80, 90, 60), hat = 0, hatCol = 0, eye = k(30, 20, 16), bigNose = false,
        beard = 1, hair = k(20, 16, 14), shades = false, suit = false, acc = k(226, 186, 70), scale = 1.22f,
        camo = k(60, 52, 36), epaulet = k(180, 30, 40)
    )
    // Apollo: like Savanna, with a moustache, a beard and long hair
    val APOLLO = Look(
        skin = k(118, 74, 52), skinHi = k(148, 98, 70), cloth = k(84, 96, 64), clothD = k(58, 68, 44),
        vest = k(60, 66, 50), hat = 0, hatCol = 0, eye = k(30, 20, 16), bigNose = false,
        beard = 2, hair = k(26, 20, 18), shades = false, suit = false, acc = k(236, 196, 70), scale = 1.22f,
        hairLong = 1, camo = k(52, 60, 38)
    )

    val ALL: Array<Look> by lazy {
        arrayOf(JANJA, JANJA_HELMET, FALAN, FALAN_BARE, ELITE_UNI, ELITE_SUIT, DEFECTOR, DINARI, EXPERT, SAVANNA, QUBBA, APOLLO)
    }

    fun id(l: Look): Int {
        val a = ALL
        for (i in a.indices) if (a[i] === l) return i
        return 0
    }
}

/** All drawing of characters, vehicles, aircraft and buildings (2.5D). */
object Sprites {
    private const val PI_F = 3.14159265f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val r = RectF()
    private var ga = 1f
    private val RED = Color.rgb(224, 32, 32)

    fun mix(a: Int, b: Int, t: Float): Int {
        val q = if (t < 0f) 0f else if (t > 1f) 1f else t
        return Color.argb(
            Color.alpha(a),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * q).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * q).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * q).toInt()
        )
    }

    private fun fill(color: Int, aMul: Float = 1f) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = (Color.alpha(color) * ga * aMul).toInt().coerceIn(0, 255)
    }

    private fun stroke(color: Int, w: Float, aMul: Float = 1f) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = w
        p.color = color
        p.alpha = (Color.alpha(color) * ga * aMul).toInt().coerceIn(0, 255)
    }

    private fun circle(c: Canvas, x: Float, y: Float, rad: Float) = c.drawCircle(x, y, rad, p)

    private fun oval(c: Canvas, l: Float, t: Float, rr: Float, b: Float) {
        r.set(l, t, rr, b)
        c.drawOval(r, p)
    }

    private fun rrect(c: Canvas, l: Float, t: Float, rr: Float, b: Float, rad: Float) {
        r.set(l, t, rr, b)
        c.drawRoundRect(r, rad, rad, p)
    }

    // ------------------------------------------------------------------ soldier
    private val GUN_LEN = floatArrayOf(9f, 18f, 20f, 12f, 22f, 20f, 0f)

    /**
     * Draws a soldier standing at (x, y) = feet. (aimX, aimY) = unit vector (screen space) of the weapon.
     * gun: 0 pistol, 1 AK-47, 2 G3, 3 Magnus, 4 RPG, 5 machine gun, 6 none.
     * armUp >= 0 means the dance pose (arms raised, legs lifted).
     */
    fun soldier(
        c: Canvas, x: Float, y: Float, sc: Float, lk: Look, phase: Float, moving: Boolean,
        aimX: Float, aimY: Float, gun: Int, muzzle: Float, flash: Float, alpha: Float,
        armUp: Float = -1f, legL: Float = 0f, legR: Float = 0f, bounce: Float = 0f
    ) {
        ga = alpha
        c.save()
        c.translate(x, y)
        val s = sc * lk.scale
        c.scale(s, s)
        val dance = armUp >= 0f
        val sw = if (moving) sin(phase) else 0f
        val bob = (if (moving) abs(sw) * 1.5f else 0f) + bounce

        fun tint(col: Int): Int = if (flash > 0f) mix(col, RED, min(1f, flash)) else col
        val skin = tint(lk.skin)
        val skinHi = tint(lk.skinHi)
        val cloth = tint(lk.cloth)
        val clothD = tint(lk.clothD)
        val vest = tint(lk.vest)
        val boot = tint(Color.rgb(34, 28, 24))

        // shadow
        fill(Color.BLACK, 0.28f)
        oval(c, -14f, -4f, 14f, 5f)

        // legs
        val lfx: Float
        val lfy: Float
        val rfx: Float
        val rfy: Float
        if (dance) {
            lfx = -6f - legL * 4f
            lfy = -3f - legL * 13f
            rfx = 6f + legR * 4f
            rfy = -3f - legR * 13f
        } else {
            lfx = -6f + sw * 3f
            lfy = -3f - max(0f, sw) * 4f
            rfx = 6f - sw * 3f
            rfy = -3f - max(0f, -sw) * 4f
        }
        stroke(clothD, 7f)
        c.drawLine(-5.5f, -19f, lfx, lfy, p)
        c.drawLine(5.5f, -19f, rfx, rfy, p)
        fill(boot)
        rrect(c, lfx - 5.2f, lfy - 2.4f, lfx + 4.2f, lfy + 2.6f, 2.2f)
        rrect(c, rfx - 4.2f, rfy - 2.4f, rfx + 5.2f, rfy + 2.6f, 2.2f)

        c.translate(0f, -bob)

        // torso
        fill(cloth)
        rrect(c, -12.5f, -45f, 12.5f, -17f, 6f)
        if (lk.suit) {
            fill(Color.rgb(238, 238, 240))
            path.reset()
            path.moveTo(-4.5f, -45f)
            path.lineTo(4.5f, -45f)
            path.lineTo(0f, -28f)
            path.close()
            c.drawPath(path, p)
            fill(tint(lk.acc))
            path.reset()
            path.moveTo(-1.6f, -44f)
            path.lineTo(1.6f, -44f)
            path.lineTo(2.4f, -31f)
            path.lineTo(0f, -28.6f)
            path.lineTo(-2.4f, -31f)
            path.close()
            c.drawPath(path, p)
            stroke(clothD, 1.4f)
            c.drawLine(-12f, -45f, -4.5f, -28f, p)
            c.drawLine(12f, -45f, 4.5f, -28f, p)
        } else {
            fill(vest)
            rrect(c, -10f, -43.5f, 10f, -23f, 3.5f)
            fill(clothD)
            rrect(c, -8.6f, -33f, -3.4f, -26f, 1.4f)
            rrect(c, -2.2f, -33f, 2.8f, -26f, 1.4f)
            rrect(c, 4f, -33f, 8.8f, -26f, 1.4f)
            fill(tint(lk.acc))
            rrect(c, -13.4f, -45f, -9f, -41.4f, 1.2f)
            rrect(c, 9f, -45f, 13.4f, -41.4f, 1.2f)
        }
        fill(tint(Color.rgb(30, 26, 22)))
        r.set(-12.5f, -24f, 12.5f, -20.5f)
        c.drawRect(r, p)

        // arms and weapon
        if (dance) {
            val up = armUp
            val wave = sin(phase) * 3f
            val lhx = -17f - up * 3f
            val lhy = -42f - up * 22f + wave
            val rhx = 17f + up * 3f
            val rhy = -42f - up * 22f - wave
            stroke(cloth, 6f)
            c.drawLine(-11f, -42f, lhx, lhy, p)
            c.drawLine(11f, -42f, rhx, rhy, p)
            fill(skin)
            circle(c, lhx, lhy, 3.4f)
            circle(c, rhx, rhy, 3.4f)
        } else {
            val gx = 2f
            val gy = -31f
            val len = GUN_LEN[gun]
            val tx = gx + aimX * len
            val ty = gy + aimY * len
            val bx = gx - aimX * 6f
            val by = gy - aimY * 6f
            val mx = gx + aimX * (len * 0.55f)
            val my = gy + aimY * (len * 0.55f)
            if (gun != 6) {
                when (gun) {
                    0 -> {
                        stroke(Color.rgb(40, 40, 46), 3.2f)
                        c.drawLine(gx, gy, tx, ty, p)
                    }
                    3 -> {
                        stroke(Color.rgb(196, 200, 208), 3.2f)
                        c.drawLine(gx, gy, tx, ty, p)
                        stroke(Color.rgb(112, 76, 44), 3.6f)
                        c.drawLine(gx, gy, gx - aimX * 3f, gy - aimY * 3f + 3f, p)
                    }
                    4 -> {
                        stroke(Color.rgb(72, 82, 60), 5f)
                        c.drawLine(gx - aimX * 12f, gy - aimY * 12f, tx, ty, p)
                        fill(Color.rgb(122, 108, 78))
                        circle(c, tx + aimX * 3f, ty + aimY * 3f, 4.2f)
                    }
                    else -> {
                        stroke(Color.rgb(100, 68, 40), 3.6f)
                        c.drawLine(bx, by, gx, gy, p)
                        stroke(Color.rgb(34, 36, 40), 3.8f)
                        c.drawLine(gx, gy, mx, my, p)
                        stroke(Color.rgb(78, 80, 86), 2.2f)
                        c.drawLine(mx, my, tx, ty, p)
                        stroke(Color.rgb(34, 36, 40), 2.8f)
                        c.drawLine(gx + aimX * 4f, gy + aimY * 4f, gx + aimX * 4f + 0.5f, gy + aimY * 4f + 6f, p)
                    }
                }
                if (muzzle > 0f) {
                    fill(Color.rgb(255, 232, 130))
                    circle(c, tx + aimX * 3f, ty + aimY * 3f, 5.2f)
                    fill(Color.rgb(255, 150, 40), 0.7f)
                    circle(c, tx + aimX * 3f, ty + aimY * 3f, 8f)
                }
            }
            stroke(cloth, 5.6f)
            c.drawLine(-11f, -40f, mx, my, p)
            c.drawLine(11f, -40f, gx + 1f, gy + 1f, p)
            fill(skin)
            circle(c, mx, my, 3.1f)
            circle(c, gx + 1f, gy + 1f, 3.1f)
        }

        // head
        fill(skin)
        circle(c, -10f, -53f, 2.4f)
        circle(c, 10f, -53f, 2.4f)
        circle(c, 0f, -53f, 10.4f)
        // hair
        fill(tint(lk.hair))
        for (i in 0..6) {
            val a = (198f + i * 24f) * PI_F / 180f
            circle(c, cos(a) * 9.4f, -53f + sin(a) * 9.4f, 3.4f)
        }
        // eyes
        fill(Color.rgb(242, 240, 232))
        oval(c, -6.8f, -56.2f, -2.2f, -52.6f)
        oval(c, 2.2f, -56.2f, 6.8f, -52.6f)
        if (lk.eye == Color.rgb(240, 30, 30)) {
            fill(lk.eye, 0.28f)
            circle(c, -4.4f, -54.4f, 4.6f)
            circle(c, 4.4f, -54.4f, 4.6f)
        }
        fill(tint(lk.eye))
        circle(c, -4.3f, -54.4f, 1.7f)
        circle(c, 4.3f, -54.4f, 1.7f)
        fill(Color.rgb(10, 8, 8))
        circle(c, -4.3f, -54.4f, 0.8f)
        circle(c, 4.3f, -54.4f, 0.8f)
        stroke(tint(lk.hair), 1.7f)
        c.drawLine(-7.4f, -58.2f, -2f, -56.6f, p)
        c.drawLine(7.4f, -58.2f, 2f, -56.6f, p)
        // nose (big for the Falanqayat)
        if (lk.bigNose) {
            fill(skinHi)
            oval(c, -3.6f, -55f, 3.6f, -48.4f)
            fill(Color.rgb(10, 8, 8), 0.7f)
            circle(c, -1.6f, -49.8f, 0.9f)
            circle(c, 1.6f, -49.8f, 0.9f)
        } else {
            stroke(skinHi, 1.3f)
            c.drawLine(0f, -54f, 0.8f, -50.6f, p)
        }
        // beard / mustache
        if (lk.beard == 2) {
            fill(tint(lk.hair))
            path.reset()
            path.moveTo(-9.6f, -55f)
            path.quadTo(-10.2f, -43f, 0f, -43.4f)
            path.quadTo(10.2f, -43f, 9.6f, -55f)
            path.quadTo(6f, -49.4f, 0f, -50.6f)
            path.quadTo(-6f, -49.4f, -9.6f, -55f)
            path.close()
            c.drawPath(path, p)
        } else if (lk.beard == 1) {
            stroke(tint(lk.hair), 2.2f)
            path.reset()
            path.moveTo(-4.6f, -49.6f)
            path.quadTo(0f, -52.4f, 4.6f, -49.6f)
            c.drawPath(path, p)
        }
        // mouth
        if (lk.beard != 2) {
            stroke(Color.rgb(24, 14, 14), 1.4f)
            if (dance) {
                r.set(-3.2f, -49f, 3.2f, -45.4f)
                c.drawArc(r, 10f, 160f, false, p)
            } else {
                c.drawLine(-2.8f, -46.8f, 2.8f, -46.8f, p)
            }
        }
        // sunglasses
        if (lk.shades) {
            fill(Color.rgb(16, 16, 20))
            rrect(c, -8f, -57f, -0.8f, -52.2f, 2f)
            rrect(c, 0.8f, -57f, 8f, -52.2f, 2f)
            stroke(Color.rgb(16, 16, 20), 1.2f)
            c.drawLine(-0.8f, -55f, 0.8f, -55f, p)
            fill(Color.WHITE, 0.35f)
            rrect(c, -7f, -56.4f, -4f, -55.4f, 0.5f)
        }
        // headgear
        val hc = tint(lk.hatCol)
        when (lk.hat) {
            1 -> {
                fill(hc)
                r.set(-11.8f, -66.6f, 11.8f, -46f)
                c.drawArc(r, 180f, 180f, true, p)
                fill(mix(hc, Color.BLACK, 0.3f))
                rrect(c, -12.6f, -58.4f, 12.6f, -56.2f, 1f)
            }
            2 -> {
                fill(hc)
                oval(c, -12f, -67f, 12.6f, -57f)
                fill(tint(lk.acc))
                circle(c, -7f, -61f, 1.6f)
            }
            3 -> {
                fill(hc)
                rrect(c, -11f, -68.4f, 11f, -60.4f, 2f)
                fill(mix(hc, Color.BLACK, 0.4f))
                rrect(c, -12.6f, -61.4f, 12.6f, -59f, 1.2f)
                fill(tint(lk.acc))
                circle(c, 0f, -64.4f, 2.2f)
                fill(Color.rgb(210, 40, 44))
                rrect(c, -11f, -62.4f, 11f, -61.2f, 0.4f)
            }
            4 -> {
                fill(hc)
                oval(c, -15f, -62.6f, 15f, -56.6f)
                r.set(-9.6f, -69.4f, 9.6f, -56f)
                c.drawArc(r, 180f, 180f, true, p)
            }
        }
        c.restore()
        ga = 1f
    }

    // ------------------------------------------------------------------ vehicles
    /**
     * type 0 = enemy technical (pickup with a machine gun), 1 = 4x4 carrying a 23 mm twin cannon,
     * 2 = allied pickup, 3 = Dinari armoured truck.
     */
    fun vehicle(
        c: Canvas, x: Float, y: Float, sc: Float, type: Int, faceR: Boolean, wheel: Float,
        aimX: Float, aimY: Float, muzzle: Float, flash: Float
    ) {
        ga = 1f
        fun tint(col: Int): Int = if (flash > 0f) mix(col, RED, min(1f, flash)) else col
        val body: Int
        val dark: Int
        when (type) {
            0 -> { body = Color.rgb(64, 92, 56); dark = Color.rgb(40, 62, 38) }
            1 -> { body = Color.rgb(198, 170, 112); dark = Color.rgb(150, 124, 78) }
            2 -> { body = Color.rgb(176, 152, 104); dark = Color.rgb(130, 108, 70) }
            else -> { body = Color.rgb(206, 210, 216); dark = Color.rgb(150, 158, 170) }
        }
        val dir = if (faceR) 1f else -1f
        c.save()
        c.translate(x, y)
        c.scale(sc * dir, sc)
        // shadow
        fill(Color.BLACK, 0.3f)
        oval(c, -42f, -6f, 42f, 7f)
        // wheels
        for (wx in floatArrayOf(-24f, 24f)) {
            fill(Color.rgb(22, 22, 24))
            circle(c, wx, -9f, 10f)
            fill(Color.rgb(120, 120, 126))
            circle(c, wx, -9f, 4.2f)
            stroke(Color.rgb(60, 60, 66), 1.6f)
            c.drawLine(wx + cos(wheel) * 8f, -9f + sin(wheel) * 8f, wx - cos(wheel) * 8f, -9f - sin(wheel) * 8f, p)
        }
        // chassis + bed + cab
        fill(tint(dark))
        rrect(c, -38f, -22f, 38f, -12f, 3f)
        fill(tint(body))
        rrect(c, -38f, -32f, 6f, -14f, 3f)
        rrect(c, 4f, -40f, 36f, -14f, 4f)
        fill(Color.rgb(46, 66, 80))
        path.reset()
        path.moveTo(10f, -37f)
        path.lineTo(24f, -37f)
        path.lineTo(32f, -26f)
        path.lineTo(10f, -26f)
        path.close()
        c.drawPath(path, p)
        fill(Color.rgb(250, 232, 150))
        circle(c, 35f, -22f, 2.6f)
        if (type == 3) {
            fill(tint(dark))
            rrect(c, -36f, -50f, 4f, -32f, 3f)
        }
        c.restore()

        // mounted weapon (drawn in screen space so it can point anywhere)
        val bx = x + (-14f * dir) * sc
        val by = y - 34f * sc
        ga = 1f
        if (type == 1) {
            fill(Color.rgb(58, 62, 58))
            circle(c, bx, by, 8f * sc)
            fill(Color.rgb(90, 96, 88))
            rrect(c, bx - 9f * sc, by - 4f * sc, bx + 9f * sc, by + 4f * sc, 2f)
            for (o in floatArrayOf(-3.2f, 3.2f)) {
                val ox = -aimY * o * sc
                val oy = aimX * o * sc
                stroke(Color.rgb(38, 40, 40), 3.4f * sc)
                c.drawLine(bx + ox, by + oy, bx + ox + aimX * 34f * sc, by + oy + aimY * 34f * sc, p)
                if (muzzle > 0f) {
                    fill(Color.rgb(255, 224, 120))
                    circle(c, bx + ox + aimX * 38f * sc, by + oy + aimY * 38f * sc, 6f * sc)
                }
            }
        } else {
            fill(Color.rgb(50, 52, 54))
            circle(c, bx, by + 4f * sc, 5f * sc)
            stroke(Color.rgb(36, 38, 40), 3.2f * sc)
            c.drawLine(bx, by, bx + aimX * 22f * sc, by + aimY * 22f * sc, p)
            if (muzzle > 0f) {
                fill(Color.rgb(255, 224, 120))
                circle(c, bx + aimX * 26f * sc, by + aimY * 26f * sc, 5f * sc)
            }
        }
    }

    // ------------------------------------------------------------------ aircraft
    fun plane(c: Canvas, x: Float, y: Float, sc: Float, faceR: Boolean, flash: Float, t: Float) {
        ga = 1f
        val dir = if (faceR) 1f else -1f
        c.save()
        c.translate(x, y)
        c.scale(sc * dir, sc)
        val body = if (flash > 0f) mix(Color.rgb(96, 104, 120), RED, min(1f, flash)) else Color.rgb(96, 104, 120)
        // engine glow
        fill(Color.rgb(255, 170, 60), 0.8f)
        oval(c, -52f - 6f * abs(sin(t * 30f)), -5f, -38f, 3f)
        // wings
        fill(Color.rgb(72, 78, 92))
        path.reset()
        path.moveTo(-6f, -2f)
        path.lineTo(-34f, 16f)
        path.lineTo(-20f, 16f)
        path.lineTo(14f, -2f)
        path.close()
        c.drawPath(path, p)
        path.reset()
        path.moveTo(-6f, -4f)
        path.lineTo(-30f, -22f)
        path.lineTo(-18f, -22f)
        path.lineTo(14f, -4f)
        path.close()
        c.drawPath(path, p)
        // tail fin
        fill(Color.rgb(84, 90, 104))
        path.reset()
        path.moveTo(-40f, -4f)
        path.lineTo(-46f, -22f)
        path.lineTo(-30f, -4f)
        path.close()
        c.drawPath(path, p)
        // fuselage
        fill(body)
        path.reset()
        path.moveTo(50f, 0f)
        path.quadTo(30f, -9f, -8f, -8f)
        path.lineTo(-42f, -6f)
        path.lineTo(-42f, 4f)
        path.lineTo(-8f, 6f)
        path.quadTo(30f, 6f, 50f, 0f)
        path.close()
        c.drawPath(path, p)
        // canopy + red stripe
        fill(Color.rgb(60, 110, 150))
        oval(c, 6f, -10f, 28f, -2f)
        fill(Color.rgb(196, 40, 44))
        rrect(c, -30f, -4f, -10f, -1.4f, 1f)
        c.restore()
    }

    // ------------------------------------------------------------------ buildings (oblique 2.5D boxes)
    /** Box whose footprint is l to r on screen with back edge backY and front edge frontY (ground level). */
    fun box(c: Canvas, l: Float, r0: Float, backY: Float, frontY: Float, h: Float, top: Int, front: Int, side: Int) {
        ga = 1f
        // ground shadow
        fill(Color.BLACK, 0.22f)
        oval(c, l - 4f, frontY - 6f, r0 + 16f, frontY + 8f)
        // front face
        fill(front)
        r.set(l, frontY - h, r0, frontY)
        c.drawRect(r, p)
        fill(side, 0.5f)
        r.set(r0 - (r0 - l) * 0.12f, frontY - h, r0, frontY)
        c.drawRect(r, p)
        // top face
        fill(top)
        path.reset()
        path.moveTo(l, backY - h)
        path.lineTo(r0, backY - h)
        path.lineTo(r0, frontY - h)
        path.lineTo(l, frontY - h)
        path.close()
        c.drawPath(path, p)
        stroke(Color.BLACK, 1.2f, 0.25f)
        c.drawLine(l, frontY - h, r0, frontY - h, p)
    }

    /**
     * type 0 house, 1 control room, 2 water tank, 3 tent, 4 earth rampart, 5 depot/warehouse.
     * (l..r) = screen footprint, backY/frontY = ground edges.
     */
    fun building(c: Canvas, type: Int, l: Float, r0: Float, backY: Float, frontY: Float, h: Float, hpFrac: Float, t: Float, flash: Float) {
        ga = 1f
        val w = r0 - l
        val cx = (l + r0) / 2f
        fun tint(col: Int): Int = if (flash > 0f) mix(col, RED, min(1f, flash)) else col
        when (type) {
            0, 5 -> {
                val top = if (type == 0) Color.rgb(226, 190, 138) else Color.rgb(170, 176, 160)
                val front = if (type == 0) Color.rgb(200, 160, 108) else Color.rgb(140, 148, 134)
                box(c, l, r0, backY, frontY, h, tint(top), tint(front), Color.rgb(90, 70, 50))
                // door + windows
                fill(Color.rgb(54, 36, 24))
                rrect(c, cx - 8f, frontY - h * 0.55f, cx + 8f, frontY, 2f)
                fill(Color.rgb(40, 30, 26))
                rrect(c, l + 8f, frontY - h * 0.78f, l + 22f, frontY - h * 0.5f, 2f)
                rrect(c, r0 - 22f, frontY - h * 0.78f, r0 - 8f, frontY - h * 0.5f, 2f)
            }
            1 -> {
                box(c, l, r0, backY, frontY, h, tint(Color.rgb(184, 186, 192)), tint(Color.rgb(140, 144, 152)), Color.rgb(80, 84, 92))
                // window strip + door
                fill(Color.rgb(40, 74, 96))
                rrect(c, l + 8f, frontY - h * 0.78f, r0 - 8f, frontY - h * 0.48f, 2f)
                fill(Color.rgb(48, 50, 56))
                rrect(c, cx - 9f, frontY - h * 0.42f, cx + 9f, frontY, 2f)
                // roof: antenna mast + radar dish + blinking light
                val roofY = (backY + frontY) / 2f - h
                stroke(Color.rgb(70, 72, 78), 2.4f)
                c.drawLine(cx - w * 0.3f, roofY, cx - w * 0.3f, roofY - 40f, p)
                c.drawLine(cx - w * 0.3f - 8f, roofY - 28f, cx - w * 0.3f + 8f, roofY - 28f, p)
                stroke(Color.rgb(90, 92, 98), 2f)
                c.drawLine(cx + w * 0.22f, roofY, cx + w * 0.22f, roofY - 16f, p)
                fill(Color.rgb(214, 216, 222))
                oval(c, cx + w * 0.22f - 17f, roofY - 34f, cx + w * 0.22f + 17f, roofY - 14f)
                fill(Color.rgb(120, 124, 132))
                circle(c, cx + w * 0.22f, roofY - 24f, 3f)
                if (sin(t * 6f) > 0f) {
                    fill(Color.rgb(255, 60, 50))
                    circle(c, cx - w * 0.3f, roofY - 42f, 3.4f)
                    fill(Color.rgb(255, 60, 50), 0.25f)
                    circle(c, cx - w * 0.3f, roofY - 42f, 9f)
                }
                // flight-control label stripe
                fill(Color.rgb(230, 190, 60))
                rrect(c, l, frontY - h, r0, frontY - h + 5f, 1f)
            }
            2 -> {
                // water tank on legs
                stroke(Color.rgb(84, 70, 56), 4f)
                c.drawLine(l + 6f, frontY, l + 14f, frontY - h * 0.6f, p)
                c.drawLine(r0 - 6f, frontY, r0 - 14f, frontY - h * 0.6f, p)
                c.drawLine(cx - 6f, backY, cx - 6f, frontY - h * 0.6f, p)
                fill(Color.BLACK, 0.22f)
                oval(c, l - 4f, frontY - 6f, r0 + 16f, frontY + 8f)
                val tankTop = frontY - h
                val tankBot = frontY - h * 0.6f
                fill(tint(Color.rgb(120, 148, 170)))
                r.set(l + 4f, tankTop, r0 - 4f, tankBot)
                c.drawRect(r, p)
                fill(tint(Color.rgb(100, 128, 150)))
                oval(c, l + 4f, tankBot - 9f, r0 - 4f, tankBot + 9f)
                fill(tint(Color.rgb(160, 188, 208)))
                oval(c, l + 4f, tankTop - 9f, r0 - 4f, tankTop + 9f)
                // pipe + valve wheel
                stroke(Color.rgb(70, 74, 80), 5f)
                c.drawLine(cx, tankBot + 6f, cx, frontY - 6f, p)
                c.drawLine(cx, frontY - 6f, cx + 30f, frontY - 6f, p)
                fill(Color.rgb(210, 50, 44))
                circle(c, cx + 16f, frontY - 14f, 9f)
                stroke(Color.rgb(250, 230, 220), 2f)
                c.drawLine(cx + 16f - 7f, frontY - 14f, cx + 16f + 7f, frontY - 14f, p)
                c.drawLine(cx + 16f, frontY - 21f, cx + 16f, frontY - 7f, p)
            }
            3 -> {
                // tent: front triangle + roof ridge
                fill(Color.BLACK, 0.22f)
                oval(c, l - 4f, frontY - 6f, r0 + 14f, frontY + 8f)
                fill(tint(Color.rgb(98, 110, 70)))
                path.reset()
                path.moveTo(l, frontY)
                path.lineTo(cx, frontY - h)
                path.lineTo(r0, frontY)
                path.close()
                c.drawPath(path, p)
                fill(tint(Color.rgb(74, 84, 52)))
                path.reset()
                path.moveTo(cx, frontY - h)
                path.lineTo(r0, frontY)
                path.lineTo(r0 - 8f, backY)
                path.lineTo(cx - 4f, backY - h)
                path.close()
                c.drawPath(path, p)
                fill(Color.rgb(36, 30, 24))
                path.reset()
                path.moveTo(cx - 10f, frontY)
                path.lineTo(cx, frontY - h * 0.5f)
                path.lineTo(cx + 10f, frontY)
                path.close()
                c.drawPath(path, p)
            }
            else -> {
                // earth rampart: tall dirt wall
                fill(Color.BLACK, 0.25f)
                oval(c, l - 4f, frontY - 6f, r0 + 14f, frontY + 10f)
                p.shader = android.graphics.LinearGradient(
                    0f, frontY - h, 0f, frontY, Color.rgb(188, 140, 88), Color.rgb(112, 78, 46), android.graphics.Shader.TileMode.CLAMP
                )
                p.style = Paint.Style.FILL
                p.alpha = 255
                r.set(l, frontY - h, r0, frontY)
                c.drawRect(r, p)
                p.shader = null
                fill(Color.rgb(214, 168, 112))
                path.reset()
                path.moveTo(l, backY - h)
                var xx = l
                while (xx <= r0) {
                    path.lineTo(xx, (frontY + backY) / 2f - h + sin(xx * 0.09f) * 3f)
                    xx += 10f
                }
                path.lineTo(r0, frontY - h)
                path.lineTo(l, frontY - h)
                path.close()
                c.drawPath(path, p)
                stroke(Color.rgb(70, 46, 26), 2f, 0.3f)
                var k = 1
                while (k < 4) {
                    c.drawLine(l, frontY - h + k * h / 4f, r0, frontY - h + k * h / 4f + 3f, p)
                    k++
                }
                // stones in the wall
                var sx = l + 10f
                var i = 0
                while (sx < r0 - 6f) {
                    fill(if (i % 2 == 0) Color.rgb(124, 106, 90) else Color.rgb(98, 80, 62))
                    circle(c, sx, frontY - h * (0.25f + 0.5f * ((i * 37) % 10) / 10f), 3.4f + (i % 3))
                    sx += 22f
                    i++
                }
            }
        }
        // health bar for destructible targets
        if (hpFrac < 0.999f && (type == 1 || type == 0 || type == 5)) {
            fill(Color.BLACK, 0.6f)
            rrect(c, cx - 26f, frontY - h - 50f, cx + 26f, frontY - h - 44f, 3f)
            fill(Color.rgb(230, 70, 60))
            rrect(c, cx - 26f, frontY - h - 50f, cx - 26f + 52f * max(0.04f, hpFrac), frontY - h - 44f, 3f)
        }
    }

    /** Sandbag / barricade cover piece. */
    fun cover(c: Canvas, l: Float, r0: Float, backY: Float, frontY: Float, h: Float, kind: Int) {
        ga = 1f
        fill(Color.BLACK, 0.2f)
        oval(c, l - 2f, frontY - 4f, r0 + 8f, frontY + 6f)
        if (kind == 0) {
            // sandbags in a row, two high
            var x = l
            var i = 0
            while (x < r0 - 4f) {
                val bw = min(15f, r0 - x)
                for (row in 0..1) {
                    val yy = frontY - row * h * 0.5f
                    fill(if ((i + row) % 2 == 0) Color.rgb(202, 174, 120) else Color.rgb(180, 150, 100))
                    rrect(c, x + row * 4f, yy - h * 0.5f, x + bw - 1f + row * 4f, yy, 3f)
                }
                x += 14f
                i++
            }
        } else if (kind == 1) {
            // concrete barrier
            box(c, l, r0, backY, frontY, h, Color.rgb(170, 170, 166), Color.rgb(132, 132, 130), Color.rgb(90, 90, 90))
        } else {
            // wrecked car
            box(c, l, r0, backY, frontY, h * 0.8f, Color.rgb(96, 70, 54), Color.rgb(70, 52, 42), Color.rgb(40, 30, 24))
            fill(Color.rgb(20, 20, 22))
            circle(c, l + 10f, frontY, 6f)
            circle(c, r0 - 10f, frontY, 6f)
        }
    }
}
