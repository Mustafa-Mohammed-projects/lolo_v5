package com.abololo.game

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/** Camera shared by the OpenGL renderer and the 2D HUD (so the HUD can project world points). */
class Cam {
    @JvmField val view = FloatArray(16)
    @JvmField val proj = FloatArray(16)
    @JvmField val vp = FloatArray(16)
    @JvmField var ex = 0f
    @JvmField var ey = 100f
    @JvmField var ez = 100f
    private val tmp = FloatArray(4)
    private val out = FloatArray(4)

    fun set(
        ex: Float, ey: Float, ez: Float, cx: Float, cy: Float, cz: Float,
        fovDeg: Float, aspect: Float
    ) {
        this.ex = ex; this.ey = ey; this.ez = ez
        Matrix.setLookAtM(view, 0, ex, ey, ez, cx, cy, cz, 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, fovDeg, aspect, 6f, 4500f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    /** Returns false when the point is behind the camera. res = (x, y) in 0..1 screen fractions (y down). */
    fun project(x: Float, y: Float, z: Float, res: FloatArray): Boolean {
        tmp[0] = x; tmp[1] = y; tmp[2] = z; tmp[3] = 1f
        Matrix.multiplyMV(out, 0, vp, 0, tmp, 0)
        if (out[3] <= 0.01f) return false
        res[0] = (out[0] / out[3] * 0.5f + 0.5f)
        res[1] = (1f - (out[1] / out[3] * 0.5f + 0.5f))
        return true
    }
}

/** A unit mesh: position(3) + normal(3) + uv(2) per vertex, drawn with a per-draw model matrix. */
class Mesh(verts: FloatArray) {
    val buf: FloatBuffer
    val count: Int = verts.size / 8

    init {
        val bb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
        buf = bb.asFloatBuffer()
        buf.put(verts)
        buf.position(0)
    }
}

object MeshBuilder {
    private fun v(o: ArrayList<Float>, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, w: Float) {
        o.add(x); o.add(y); o.add(z); o.add(nx); o.add(ny); o.add(nz); o.add(u); o.add(w)
    }

    private fun quad(o: ArrayList<Float>, a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray, n: FloatArray) {
        v(o, a[0], a[1], a[2], n[0], n[1], n[2], 0f, 0f)
        v(o, b[0], b[1], b[2], n[0], n[1], n[2], 1f, 0f)
        v(o, c[0], c[1], c[2], n[0], n[1], n[2], 1f, 1f)
        v(o, a[0], a[1], a[2], n[0], n[1], n[2], 0f, 0f)
        v(o, c[0], c[1], c[2], n[0], n[1], n[2], 1f, 1f)
        v(o, d[0], d[1], d[2], n[0], n[1], n[2], 0f, 1f)
    }

    private fun toArray(o: ArrayList<Float>): FloatArray {
        val r = FloatArray(o.size)
        for (i in 0 until o.size) r[i] = o[i]
        return r
    }

    /** cube from -0.5 to 0.5 */
    fun cube(): Mesh {
        val o = ArrayList<Float>()
        val h = 0.5f
        quad(o, floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), floatArrayOf(0f, 0f, 1f))
        quad(o, floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), floatArrayOf(0f, 0f, -1f))
        quad(o, floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(h, h, -h), floatArrayOf(h, h, h), floatArrayOf(1f, 0f, 0f))
        quad(o, floatArrayOf(-h, -h, -h), floatArrayOf(-h, -h, h), floatArrayOf(-h, h, h), floatArrayOf(-h, h, -h), floatArrayOf(-1f, 0f, 0f))
        quad(o, floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(-h, h, -h), floatArrayOf(0f, 1f, 0f))
        quad(o, floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(-h, -h, h), floatArrayOf(0f, -1f, 0f))
        return Mesh(toArray(o))
    }

    /** vertical cylinder (axis Y), radius 0.5, height 1, centred. topScale shrinks the top ring (0 = cone). Smooth normals. */
    fun cylinder(seg: Int, topScale: Float): Mesh {
        val o = ArrayList<Float>()
        val r = 0.5f
        val tr = r * topScale
        val slope = (r - tr) / 1f
        for (i in 0 until seg) {
            val a0 = i * 6.2831853f / seg
            val a1 = (i + 1) * 6.2831853f / seg
            val c0 = cos(a0); val s0 = sin(a0); val c1 = cos(a1); val s1 = sin(a1)
            val u0 = i.toFloat() / seg
            val u1 = (i + 1).toFloat() / seg
            v(o, c0 * r, -0.5f, s0 * r, c0, slope, s0, u0, 1f)
            v(o, c1 * r, -0.5f, s1 * r, c1, slope, s1, u1, 1f)
            v(o, c1 * tr, 0.5f, s1 * tr, c1, slope, s1, u1, 0f)
            v(o, c0 * r, -0.5f, s0 * r, c0, slope, s0, u0, 1f)
            v(o, c1 * tr, 0.5f, s1 * tr, c1, slope, s1, u1, 0f)
            v(o, c0 * tr, 0.5f, s0 * tr, c0, slope, s0, u0, 0f)
            if (topScale > 0.001f) {
                v(o, 0f, 0.5f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
                v(o, c1 * tr, 0.5f, s1 * tr, 0f, 1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
                v(o, c0 * tr, 0.5f, s0 * tr, 0f, 1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)
            }
            v(o, 0f, -0.5f, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            v(o, c0 * r, -0.5f, s0 * r, 0f, -1f, 0f, 0.5f + c0 * 0.5f, 0.5f + s0 * 0.5f)
            v(o, c1 * r, -0.5f, s1 * r, 0f, -1f, 0f, 0.5f + c1 * 0.5f, 0.5f + s1 * 0.5f)
        }
        return Mesh(toArray(o))
    }

    /** UV sphere, radius 0.5, smooth normals */
    fun sphere(seg: Int, rings: Int): Mesh {
        val o = ArrayList<Float>()
        for (j in 0 until rings) {
            val p0 = j * 3.14159265f / rings
            val p1 = (j + 1) * 3.14159265f / rings
            for (i in 0 until seg) {
                val a0 = i * 6.2831853f / seg
                val a1 = (i + 1) * 6.2831853f / seg
                val u0 = i.toFloat() / seg
                val u1 = (i + 1).toFloat() / seg
                val v0 = j.toFloat() / rings
                val v1 = (j + 1).toFloat() / rings
                sv(o, p0, a0, u0, v0); sv(o, p1, a0, u0, v1); sv(o, p1, a1, u1, v1)
                sv(o, p0, a0, u0, v0); sv(o, p1, a1, u1, v1); sv(o, p0, a1, u1, v0)
            }
        }
        return Mesh(toArray(o))
    }

    private fun sv(o: ArrayList<Float>, phi: Float, th: Float, u: Float, w: Float) {
        val x = sin(phi) * cos(th)
        val y = cos(phi)
        val z = sin(phi) * sin(th)
        v(o, x * 0.5f, y * 0.5f, z * 0.5f, x, y, z, u, w)
    }

    /**
     * A patch of the front of a sphere (radius 0.5) used as a face mask. +Z is the front.
     * Longitude -62..62 degrees, latitude -52..48 degrees. uv maps the patch to 0..1.
     */
    fun facePatch(seg: Int, rings: Int): Mesh {
        val o = ArrayList<Float>()
        val lon0 = -62f * 3.14159265f / 180f
        val lon1 = 62f * 3.14159265f / 180f
        val lat0 = -52f * 3.14159265f / 180f
        val lat1 = 48f * 3.14159265f / 180f
        for (j in 0 until rings) {
            for (i in 0 until seg) {
                val fa = i.toFloat() / seg
                val fb = (i + 1).toFloat() / seg
                val ga = j.toFloat() / rings
                val gb = (j + 1).toFloat() / rings
                fv(o, lon0 + (lon1 - lon0) * fa, lat1 - (lat1 - lat0) * ga, fa, ga)
                fv(o, lon0 + (lon1 - lon0) * fa, lat1 - (lat1 - lat0) * gb, fa, gb)
                fv(o, lon0 + (lon1 - lon0) * fb, lat1 - (lat1 - lat0) * gb, fb, gb)
                fv(o, lon0 + (lon1 - lon0) * fa, lat1 - (lat1 - lat0) * ga, fa, ga)
                fv(o, lon0 + (lon1 - lon0) * fb, lat1 - (lat1 - lat0) * gb, fb, gb)
                fv(o, lon0 + (lon1 - lon0) * fb, lat1 - (lat1 - lat0) * ga, fb, ga)
            }
        }
        return Mesh(toArray(o))
    }

    private fun fv(o: ArrayList<Float>, lon: Float, lat: Float, u: Float, w: Float) {
        val x = sin(lon) * cos(lat)
        val y = sin(lat)
        val z = cos(lon) * cos(lat)
        v(o, x * 0.5f, y * 0.5f, z * 0.5f, x, y, z, u, w)
    }
}

/** Draws coloured, lit, optionally textured primitives with a matrix stack. World units, Y up. */
class Gfx {
    private var prog = 0
    private var aPos = 0
    private var aNor = 0
    private var aUv = 0
    private var uMvp = 0
    private var uModel = 0
    private var uColor = 0
    private var uLit = 0
    private var uMode = 0
    private var uEye = 0
    private var uFog = 0
    private var uUv = 0
    private var uTex = 0
    private var uDetail = 0

    lateinit var cube: Mesh
    lateinit var cyl: Mesh
    lateinit var tap: Mesh
    lateinit var cone: Mesh
    lateinit var pyr: Mesh
    lateinit var sph: Mesh
    lateinit var face: Mesh
    lateinit var skySph: Mesh

    // texture ids filled by the renderer
    @JvmField var detailTex = 0
    @JvmField var sandTex = 0
    @JvmField val faceTex = IntArray(32)
    @JvmField val camoTex = IntArray(32)

    private val stack = Array(24) { FloatArray(16) }
    private var sp = 0
    private val tmp = FloatArray(16)
    private val mvp = FloatArray(16)
    @JvmField val cam = Cam()
    var drawCalls = 0

    fun init() {
        val vs = "uniform mat4 uMVP;uniform mat4 uModel;attribute vec3 aPos;attribute vec3 aNor;attribute vec2 aUV;" +
            "varying vec3 vN;varying float vD;varying vec3 vW;varying vec2 vUV;" +
            "void main(){vec4 p=uMVP*vec4(aPos,1.0);gl_Position=p;vN=(uModel*vec4(aNor,0.0)).xyz;vD=p.w;vW=(uModel*vec4(aPos,1.0)).xyz;vUV=aUV;}"
        val fs = "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "uniform vec4 uColor;uniform float uLit;uniform float uMode;uniform vec3 uFog;uniform vec3 uEye;uniform vec2 uUV;" +
            "uniform sampler2D uTex;uniform sampler2D uDetail;" +
            "varying vec3 vN;varying float vD;varying vec3 vW;varying vec2 vUV;" +
            "void main(){" +
            "vec3 n=normalize(vN);" +
            "if(uMode>1.5){float h=clamp(n.y,0.0,1.0);vec3 hor=vec3(0.82,0.79,0.72);vec3 top=vec3(0.30,0.52,0.84);" +
            "vec3 c=mix(hor,top,pow(h,0.5));float s=max(dot(n,normalize(vec3(-0.45,0.5,0.35))),0.0);" +
            "c+=vec3(1.0,0.86,0.6)*(pow(s,40.0)*0.9+pow(s,6.0)*0.12);gl_FragColor=vec4(c,1.0);return;}" +
            "vec4 base=uColor;" +
            "if(uMode>0.5){vec4 t=texture2D(uTex,vUV*uUV);base=vec4(t.rgb*uColor.rgb,t.a*uColor.a);}" +
            "vec3 c=base.rgb;" +
            "if(uLit>0.5){" +
            "vec3 l=normalize(vec3(-0.45,0.85,0.35));float d=max(dot(n,l),0.0);" +
            "vec3 amb=mix(vec3(0.40,0.34,0.28),vec3(0.58,0.64,0.74),n.y*0.5+0.5);" +
            "float g=texture2D(uDetail,vW.xz*0.07+vW.y*0.05).r;" +
            "vec3 v=normalize(uEye-vW);vec3 hh=normalize(l+v);" +
            "float spec=pow(max(dot(n,hh),0.0),30.0)*0.16;" +
            "float rim=pow(1.0-max(dot(n,v),0.0),3.0)*0.10;" +
            "c=base.rgb*(amb+vec3(1.0,0.95,0.86)*d*0.80)*(0.84+0.30*g)+vec3(spec+rim);}" +
            "float f=clamp((vD-1200.0)/2600.0,0.0,0.85);c=mix(c,uFog,f);" +
            "gl_FragColor=vec4(c,base.a);}"
        val vsh = compile(GLES20.GL_VERTEX_SHADER, vs)
        val fsh = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vsh)
        GLES20.glAttachShader(prog, fsh)
        GLES20.glLinkProgram(prog)
        aPos = GLES20.glGetAttribLocation(prog, "aPos")
        aNor = GLES20.glGetAttribLocation(prog, "aNor")
        aUv = GLES20.glGetAttribLocation(prog, "aUV")
        uMvp = GLES20.glGetUniformLocation(prog, "uMVP")
        uModel = GLES20.glGetUniformLocation(prog, "uModel")
        uColor = GLES20.glGetUniformLocation(prog, "uColor")
        uLit = GLES20.glGetUniformLocation(prog, "uLit")
        uMode = GLES20.glGetUniformLocation(prog, "uMode")
        uFog = GLES20.glGetUniformLocation(prog, "uFog")
        uEye = GLES20.glGetUniformLocation(prog, "uEye")
        uUv = GLES20.glGetUniformLocation(prog, "uUV")
        uTex = GLES20.glGetUniformLocation(prog, "uTex")
        uDetail = GLES20.glGetUniformLocation(prog, "uDetail")
        cube = MeshBuilder.cube()
        cyl = MeshBuilder.cylinder(16, 1f)
        tap = MeshBuilder.cylinder(14, 0.72f)
        cone = MeshBuilder.cylinder(16, 0f)
        pyr = MeshBuilder.cylinder(4, 0f)
        sph = MeshBuilder.sphere(16, 12)
        face = MeshBuilder.facePatch(14, 12)
        skySph = MeshBuilder.sphere(20, 12)
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    fun begin() {
        GLES20.glUseProgram(prog)
        GLES20.glUniform3f(uFog, FOG_R, FOG_G, FOG_B)
        GLES20.glUniform3f(uEye, cam.ex, cam.ey, cam.ez)
        GLES20.glUniform1i(uTex, 0)
        GLES20.glUniform1i(uDetail, 1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, detailTex)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        sp = 0
        Matrix.setIdentityM(stack[0], 0)
        drawCalls = 0
        mode = 0f
        lit = 1f
    }

    companion object {
        const val FOG_R = 0.80f
        const val FOG_G = 0.76f
        const val FOG_B = 0.69f
    }

    // ---- matrix stack
    fun push() {
        System.arraycopy(stack[sp], 0, stack[sp + 1], 0, 16)
        sp++
    }

    fun pop() { sp-- }

    fun translate(x: Float, y: Float, z: Float) { Matrix.translateM(stack[sp], 0, x, y, z) }
    fun rotY(deg: Float) { Matrix.rotateM(stack[sp], 0, deg, 0f, 1f, 0f) }
    fun rotX(deg: Float) { Matrix.rotateM(stack[sp], 0, deg, 1f, 0f, 0f) }
    fun rotZ(deg: Float) { Matrix.rotateM(stack[sp], 0, deg, 0f, 0f, 1f) }
    fun scale(s: Float) { Matrix.scaleM(stack[sp], 0, s, s, s) }
    fun scale3(x: Float, y: Float, z: Float) { Matrix.scaleM(stack[sp], 0, x, y, z) }

    // ---- material state
    private var cr = 1f
    private var cg = 1f
    private var cb = 1f
    private var ca = 1f
    private var lit = 1f
    private var mode = 0f
    private var texId = 0
    private var tu = 1f
    private var tv = 1f

    fun color(argb: Int, a: Float = 1f) {
        cr = ((argb shr 16) and 255) / 255f
        cg = ((argb shr 8) and 255) / 255f
        cb = (argb and 255) / 255f
        ca = a
    }

    fun colorF(r: Float, g: Float, b: Float, a: Float = 1f) { cr = r; cg = g; cb = b; ca = a }

    fun unlit(on: Boolean) { lit = if (on) 0f else 1f }

    /** use a texture (replaces the colour, tinted by the current colour); id 0 = none */
    fun tex(id: Int, su: Float = 1f, sv: Float = 1f) {
        if (id == 0) { mode = 0f; return }
        mode = 1f
        texId = id
        tu = su
        tv = sv
    }

    fun noTex() { mode = 0f }

    private fun draw(m: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float) {
        System.arraycopy(stack[sp], 0, tmp, 0, 16)
        Matrix.translateM(tmp, 0, x, y, z)
        Matrix.scaleM(tmp, 0, sx, sy, sz)
        Matrix.multiplyMM(mvp, 0, cam.vp, 0, tmp, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, tmp, 0)
        GLES20.glUniform4f(uColor, cr, cg, cb, ca)
        GLES20.glUniform1f(uLit, lit)
        GLES20.glUniform1f(uMode, mode)
        if (mode > 0.5f && mode < 1.5f) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glUniform2f(uUv, tu, tv)
        }
        m.buf.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, m.buf)
        m.buf.position(3)
        GLES20.glEnableVertexAttribArray(aNor)
        GLES20.glVertexAttribPointer(aNor, 3, GLES20.GL_FLOAT, false, 32, m.buf)
        m.buf.position(6)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 32, m.buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count)
        drawCalls++
    }

    /** box centred at (x,y,z) with size (w,h,d) */
    fun box(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float) = draw(cube, x, y, z, w, h, d)

    /** vertical cylinder centred at (x,y,z), radius r, height h */
    fun cylinder(x: Float, y: Float, z: Float, r: Float, h: Float) = draw(cyl, x, y, z, r * 2f, h, r * 2f)

    /** cone / pyramid base on the bottom */
    fun coneM(x: Float, y: Float, z: Float, r: Float, h: Float) = draw(cone, x, y, z, r * 2f, h, r * 2f)

    /** square pyramid (rotated 45 degrees so the sides face the axes) */
    fun pyramid(x: Float, y: Float, z: Float, w: Float, h: Float, d: Float) {
        push()
        translate(x, y, z)
        rotY(45f)
        draw(pyr, 0f, 0f, 0f, w * 1.4142f, h, d * 1.4142f)
        pop()
    }

    fun sphere(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float) = draw(sph, x, y, z, rx * 2f, ry * 2f, rz * 2f)

    /** a face mask patch for a head ellipsoid of radii (rx, ry, rz) centred at (x,y,z) */
    fun facePatch(x: Float, y: Float, z: Float, rx: Float, ry: Float, rz: Float) = draw(face, x, y, z, rx * 2f, ry * 2f, rz * 2f)

    /** a limb: tapered cylinder hanging down from the current origin (top radius r0, bottom radius 0.72*r0), then a joint sphere */
    fun limb(len: Float, r0: Float, joint: Boolean = true) {
        push()
        translate(0f, -len * 0.5f, 0f)
        rotX(180f)
        draw(tap, 0f, 0f, 0f, r0 * 2f, len, r0 * 2f)
        pop()
        if (joint) draw(sph, 0f, -len, 0f, r0 * 1.44f, r0 * 1.44f, r0 * 1.44f)
    }

    /** big sky dome centred on the camera; call with depth writes disabled */
    fun sky(ex: Float, ey: Float, ez: Float) {
        mode = 2f
        lit = 0f
        draw(skySph, ex, ey, ez, 8000f, 8000f, 8000f)
        mode = 0f
        lit = 1f
    }

    /** Cylinder lying along the local Z axis (a gun barrel), from z0 to z1 */
    fun barrel(x: Float, y: Float, z0: Float, z1: Float, r: Float) {
        push()
        translate(x, y, (z0 + z1) * 0.5f)
        rotX(90f)
        draw(cyl, 0f, 0f, 0f, r * 2f, z1 - z0, r * 2f)
        pop()
    }

    /** Cylinder lying along the local X axis (a wheel), centred at (x,y,z), thickness t */
    fun wheel(x: Float, y: Float, z: Float, r: Float, t: Float) {
        push()
        translate(x, y, z)
        rotZ(90f)
        draw(cyl, 0f, 0f, 0f, r * 2f, t, r * 2f)
        pop()
    }
}
