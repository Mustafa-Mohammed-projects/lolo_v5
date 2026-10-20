package com.abololo.game

/** One snapshot of everything the OpenGL thread must draw. Written on the UI thread, copied under a lock. */
class Frame {
    companion object {
        const val REC = 16
        const val MAXE = 900
        const val K_SOLDIER = 1
        const val K_VEHICLE = 2
        const val K_PLANE = 3
        const val K_BUILDING = 4
        const val K_COVER = 5
        const val K_BULLET = 6
        const val K_BOOM = 7
        const val K_PART = 8
        const val K_SHELL = 9
        const val K_RING = 10
        const val K_PROP = 11
    }

    @JvmField val d = FloatArray(MAXE * REC)
    @JvmField var n = 0
    @JvmField var has = false
    @JvmField var ex = 0f
    @JvmField var ey = 0f
    @JvmField var ez = 0f
    @JvmField var cx = 0f
    @JvmField var cy = 0f
    @JvmField var cz = 0f
    @JvmField var fov = 55f
    @JvmField var t = 0f
    @JvmField var worldW = 900f
    @JvmField var worldD = 2000f
    @JvmField var scene = 0

    fun clear() { n = 0 }

    operator fun get(i: Int): Float = d[i]

    /** starts a record, returns its base index (or -1 when full) */
    fun begin(kind: Int, x: Float, y: Float, z: Float, yaw: Float): Int {
        if (n >= MAXE) return -1
        val b = n * REC
        n++
        d[b] = kind.toFloat()
        d[b + 1] = x
        d[b + 2] = y
        d[b + 3] = z
        d[b + 4] = yaw
        for (i in 5 until REC) d[b + i] = 0f
        return b
    }

    fun copyFrom(o: Frame) {
        System.arraycopy(o.d, 0, d, 0, o.n * REC)
        n = o.n
        has = o.has
        ex = o.ex; ey = o.ey; ez = o.ez
        cx = o.cx; cy = o.cy; cz = o.cz
        fov = o.fov; t = o.t
        worldW = o.worldW; worldD = o.worldD
        scene = o.scene
    }
}

object FrameBus {
    private val lock = Any()
    private val front = Frame()

    fun publish(f: Frame) {
        synchronized(lock) { front.copyFrom(f) }
    }

    fun fetch(into: Frame) {
        synchronized(lock) { into.copyFrom(front) }
    }

    fun hide() {
        synchronized(lock) { front.has = false }
    }
}
