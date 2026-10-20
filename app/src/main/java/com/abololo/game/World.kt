package com.abololo.game

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ====================================================================== entities
open class Ent(@JvmField var x: Float, @JvmField var y: Float) {
    @JvmField var hp = 1f
    @JvmField var maxHp = 1f
    @JvmField var dead = false
    @JvmField var flash = 0f
}

class Soldier(x: Float, y: Float, @JvmField var team: Int) : Ent(x, y) {
    @JvmField var look: Look = Looks.JANJA
    @JvmField var weapon = 1
    @JvmField var armed = true
    @JvmField var cd = Random.nextFloat() * 0.6f
    @JvmField var cdMul = 1f
    @JvmField var dmgMul = 1f
    @JvmField var armor = 1f
    @JvmField var aimX = 0f
    @JvmField var aimY = -1f
    @JvmField var phase = Random.nextFloat() * 6f
    @JvmField var moving = false
    @JvmField var muzzle = 0f
    @JvmField var mode = 0          // 0 guard, 1 rush, 2 hunt, 3 follow player, 4 march, 5 hold, 6 escort experts
    @JvmField var tx = 0f
    @JvmField var ty = 0f
    @JvmField var spd = 70f
    @JvmField var sight = 380f
    @JvmField var slot = 0
    @JvmField var deadT = 0f
    @JvmField var coverT = Random.nextFloat()
    @JvmField var cvx = 0f
    @JvmField var cvy = 0f
    @JvmField var hasCover = false
    @JvmField var isPlayer = false
    @JvmField var expert = false
    @JvmField var inVeh = false
    @JvmField var tagAr = ""
    @JvmField var fleeing = false
    @JvmField var tagEn = ""
}

class Vehicle(x: Float, y: Float, @JvmField var team: Int, @JvmField val type: Int) : Ent(x, y) {
    @JvmField var faceR = true
    @JvmField var wheel = 0f
    @JvmField var spd = 90f
    @JvmField var aimX = 1f
    @JvmField var aimY = 0f
    @JvmField var cd = 0f
    @JvmField var muzzle = 0f
    @JvmField var mode = 0          // 0 guard, 1 drive to (tx,ty), 2 hunt
    @JvmField var tx = 0f
    @JvmField var ty = 0f
    @JvmField var isPlayer = false
    @JvmField var moving = false
    @JvmField var deadT = 0f
    @JvmField var vx = 0f
    @JvmField var vy = 0f
    @JvmField var yaw = -1.5708f
    @JvmField var speed = 0f
    @JvmField var turret = -1.5708f
    @JvmField var fleeing = false
}

class Plane(x: Float, y: Float, @JvmField var vx: Float) : Ent(x, y) {
    @JvmField var bombCd = 0.6f
    @JvmField var warned = false
}

class Building(x: Float, y: Float, @JvmField val w: Float, @JvmField val d: Float, @JvmField val h: Float, @JvmField val type: Int) : Ent(x, y) {
    @JvmField var side = 0          // 1 = enemy target that can be destroyed
    @JvmField var solid = true
}

class Cover(@JvmField val x: Float, @JvmField val y: Float, @JvmField val w: Float, @JvmField val d: Float, @JvmField val h: Float, @JvmField val kind: Int)

class Bullet(
    @JvmField var x: Float, @JvmField var y: Float, @JvmField val vx: Float, @JvmField val vy: Float,
    @JvmField val team: Int, @JvmField val dmg: Float, @JvmField val kind: Int
) {
    @JvmField var life = 1.2f
    @JvmField var fromPlayer = false
    @JvmField var wpn = 0
    @JvmField val ox = x
    @JvmField val oy = y
}

class Pop(@JvmField val x: Float, @JvmField val y: Float, @JvmField val v: Int, @JvmField val crit: Boolean) {
    @JvmField var t = 0f
}

class Shell(@JvmField val x: Float, @JvmField val y: Float, @JvmField var t: Float, @JvmField val r: Float, @JvmField val dmg: Float, @JvmField val srcSide: Int)

class Boom(@JvmField val x: Float, @JvmField val y: Float, @JvmField val r: Float) {
    @JvmField var t = 0f
}

class Part(
    @JvmField var x: Float, @JvmField var y: Float, @JvmField var z: Float,
    @JvmField var vx: Float, @JvmField var vy: Float, @JvmField var vz: Float,
    @JvmField var life: Float, @JvmField val maxLife: Float, @JvmField val color: Int, @JvmField val size: Float, @JvmField val kind: Int
)

// ====================================================================== the battlefield
class World(private val sfx: Sfx) {
    companion object {
        const val KY = 0.72f
        const val BEAT = 60f / 118f
        const val TREATY_T = 14f
        const val NW = 12   // 0 pistol, 1 AK-47, 2 M4, 3 Magnum, 4 RPG, 5 SMG, 6 shotgun, 7 sniper, 8 LMG, 9 scoped carbine, 10 marksman rifle, 11 anti-materiel rifle
        val W_DMG = floatArrayOf(20f, 14f, 26f, 55f, 80f, 10f, 11f, 95f, 17f, 13f, 48f, 160f)
        val W_CD = floatArrayOf(0.2f, 0.1f, 0.16f, 0.6f, 1.4f, 0.075f, 0.85f, 1.1f, 0.085f, 0.085f, 0.45f, 1.5f)
        val W_MAG = intArrayOf(17, 40, 30, 6, 1, 40, 8, 10, 100, 40, 9, 5)
        val W_RES = intArrayOf(170, 400, 300, 120, 12, 480, 96, 120, 600, 400, 120, 60)
        val W_REL = floatArrayOf(1.1f, 1.7f, 2.0f, 2.2f, 2.6f, 1.6f, 2.4f, 2.6f, 3.6f, 1.6f, 1.7f, 3.0f)
        val W_RANGE = floatArrayOf(300f, 430f, 540f, 420f, 520f, 270f, 210f, 820f, 480f, 470f, 380f, 1000f)
        val W_SPEED = floatArrayOf(620f, 720f, 850f, 780f, 380f, 640f, 560f, 1100f, 780f, 800f, 760f, 1300f)
        val W_SPREAD = floatArrayOf(0.022f, 0.05f, 0.03f, 0.02f, 0f, 0.06f, 0.11f, 0.004f, 0.05f, 0.032f, 0.018f, 0.0015f)
        val W_PEL = intArrayOf(1, 1, 1, 1, 1, 1, 8, 1, 1, 1, 1, 1)
        val W_RECOIL = floatArrayOf(0.012f, 0.010f, 0.014f, 0.03f, 0f, 0.008f, 0.04f, 0.05f, 0.007f, 0.008f, 0.035f, 0.07f)
        val W_CRIT = floatArrayOf(0.08f, 0.08f, 0.1f, 0.15f, 0f, 0.06f, 0.05f, 0.35f, 0.06f, 0.1f, 0.2f, 0.5f)

        fun canScope(w: Int): Boolean = w == 1 || w == 2 || w == 7 || w == 9 || w == 10 || w == 11
        fun scopeLevels(w: Int): Int = if (w == 7 || w == 11) 2 else 1

        fun sideOf(team: Int): Int = if (team == 0 || team == 4 || team == 5 || team == 6) 0 else 1
    }

    var W = 900f
    var D = 2000f
    val soldiers = ArrayList<Soldier>()
    val vehicles = ArrayList<Vehicle>()
    val planes = ArrayList<Plane>()
    val buildings = ArrayList<Building>()
    val covers = ArrayList<Cover>()
    val bullets = ArrayList<Bullet>()
    val shells = ArrayList<Shell>()
    val booms = ArrayList<Boom>()
    val parts = ArrayList<Part>()

    var player: Soldier? = null
    var pveh: Vehicle? = null

    var mission = 1
    var phase = 0
    var t = 0f
    var result = 0                 // 0 playing, 1 win, 2 lose
    var loseReason = 0             // 1 commander fell, 2 vehicle destroyed, 3 experts died, 4 rampart breached
    var objId = 0
    var hearts = 6
    var progress = 0f
    var shake = 0f
    var hurtV = 0f
    var killCount = 0
    var supportCd = 0f

    // story hooks
    var cutId = 0
    var waitCut = false
    var dancing = false
    var sceneKind = 0
    var sceneT = 0f
    private var lastBeat = -1
    private var dinariDown = 0
    private var musicOn = false

    // input
    var moveX = 0f
    var moveY = 0f
    var fireHeld = false

    // player weapon state
    var weapon = 1
    val ammo = IntArray(NW)
    val reserve = IntArray(NW)
    var camYaw = -1.5708f
    var scoped = false
    var scopeLvl = 0
    var crouching = false
    var crouchT = 0f
    var kick = 0f
    var sprinting = false
    var holdBreath = false
    var breath = 1f
    var hitT = 0f
    var hitCrit = false
    var hitKill = false
    var swayT = 0f
    private var pvx = 0f
    private var pvy = 0f
    val pops = ArrayList<Pop>()
    var recoil = 0f
    var assistLock = false
    var crossDist = 260f
    var reloadT = 0f
    private var pCd = 0f

    // mission bookkeeping
    private var waveT = 0f
    private var planeT = 0f
    private var techT = 0f
    private var spawnedTotal = 0
    private var totalToSpawn = 0
    private var phaseT = 0f
    private var endT = 0f
    private var exitX = 0f
    private var exitY = 0f
    private var tankX = 0f
    private var tankY = 0f
    private var surveyX = 0f
    private var surveyY = 0f
    private var goalX = 0f
    private var goalY = 0f
    private var betrayed = false

    // ------------------------------------------------------------------ helpers
    private fun rf(a: Float, b: Float): Float = a + Random.nextFloat() * (b - a)

    private fun clearAll() {
        soldiers.clear(); vehicles.clear(); planes.clear(); buildings.clear(); covers.clear()
        bullets.clear(); shells.clear(); booms.clear(); parts.clear()
        player = null; pveh = null
        result = 0; loseReason = 0; progress = 0f; shake = 0f; hurtV = 0f
        waveT = 0f; planeT = 0f; techT = 0f; spawnedTotal = 0; totalToSpawn = 0; phaseT = 0f; endT = 0f
        betrayed = false; dancing = false; sceneT = 0f; lastBeat = -1
        supportCd = 5f
        reloadT = 0f; pCd = 0f
        for (k in 0 until NW) { ammo[k] = W_MAG[k]; reserve[k] = W_RES[k] }
        camYaw = -1.5708f; scoped = false; scopeLvl = 0; crouching = false; crouchT = 0f; kick = 0f; holdBreath = false; breath = 1f
        hitT = 0f; pvx = 0f; pvy = 0f; pops.clear(); recoil = 0f; assistLock = false
    }

    private fun newSoldier(team: Int, look: Look, weapon: Int, hp: Float, x: Float, y: Float): Soldier {
        val s = Soldier(x, y, team)
        s.look = look
        s.weapon = weapon
        s.hp = hp
        s.maxHp = hp
        soldiers.add(s)
        return s
    }

    private fun addFalan(x: Float, y: Float, mode: Int, tx: Float, ty: Float, tough: Boolean = false): Soldier {
        val s = newSoldier(1, if (Random.nextInt(3) == 0) Looks.FALAN_BARE else Looks.FALAN, 1, if (tough) 85f else 44f, x, y)
        s.dmgMul = 0.5f
        s.cdMul = 4f
        s.armor = if (tough) 0.85f else 1f
        s.spd = rf(58f, 76f)
        s.mode = mode
        s.tx = tx
        s.ty = ty
        s.sight = 400f
        return s
    }

    private fun addElite(x: Float, y: Float, mode: Int, suit: Boolean, team: Int = 2): Soldier {
        val look = if (team == 6) Looks.DEFECTOR else if (suit) Looks.ELITE_SUIT else Looks.ELITE_UNI
        val s = newSoldier(team, look, if (suit) 0 else 1, 60f, x, y)
        s.dmgMul = if (team == 6) 0.8f else 0.6f
        s.cdMul = if (team == 6) 2.2f else 3.4f
        s.spd = 66f
        s.mode = mode
        s.sight = 400f
        return s
    }

    private fun addDinari(x: Float, y: Float, mode: Int): Soldier {
        val s = newSoldier(3, Looks.DINARI, 2, 70f, x, y)
        s.dmgMul = 0.55f
        s.cdMul = 5f
        s.spd = 72f
        s.slot = Random.nextInt(4)
        s.mode = mode
        s.sight = 420f
        return s
    }

    private fun addAlly(x: Float, y: Float, slot: Int, helmet: Boolean = false, mode: Int = 3): Soldier {
        val s = newSoldier(0, if (helmet) Looks.JANJA_HELMET else Looks.JANJA, if (Random.nextBoolean()) 1 else 2, 95f, x, y)
        s.dmgMul = 0.9f
        s.cdMul = 2.2f
        s.spd = 80f
        s.mode = mode
        s.slot = slot
        s.sight = 420f
        return s
    }

    private fun addTechnical(x: Float, y: Float, team: Int, type: Int, mode: Int): Vehicle {
        val v = Vehicle(x, y, team, type)
        v.hp = if (type == 3) 240f else 170f
        v.maxHp = v.hp
        v.mode = mode
        v.spd = 88f
        v.yaw = if (sideOf(team) == 0) -1.5708f else 1.5708f
        v.turret = v.yaw
        vehicles.add(v)
        return v
    }

    private fun addBuilding(x: Float, y: Float, w: Float, d: Float, h: Float, type: Int, side: Int = 0, hp: Float = 100000f): Building {
        val b = Building(x, y, w, d, h, type)
        b.side = side
        b.hp = hp
        b.maxHp = hp
        b.solid = type != 3
        buildings.add(b)
        return b
    }

    private fun addCover(x: Float, y: Float, kind: Int) {
        val w = if (kind == 0) rf(58f, 90f) else if (kind == 1) rf(46f, 60f) else 62f
        val d = if (kind == 0) 16f else if (kind == 1) 20f else 30f
        val h = if (kind == 0) 20f else if (kind == 1) 28f else 30f
        covers.add(Cover(x, y, w, d, h, kind))
    }

    private fun scatterCover(n: Int, x0: Float, x1: Float, y0: Float, y1: Float, avoidX: Float = -1f, avoidHalf: Float = 0f) {
        var i = 0
        var guard = 0
        while (i < n && guard < n * 6) {
            guard++
            val x = rf(x0, x1)
            val y = rf(y0, y1)
            if (avoidX >= 0f && abs(x - avoidX) < avoidHalf) continue
            addCover(x, y, if (Random.nextInt(6) == 0) 2 else if (Random.nextInt(3) == 0) 1 else 0)
            i++
        }
    }

    private fun group(n: Int, cx: Float, cy: Float, spread: Float, mode: Int, tx: Float = 0f, ty: Float = 0f, toughEvery: Int = 0) {
        for (i in 0 until n) {
            addFalan(cx + rf(-spread, spread), cy + rf(-spread * 0.5f, spread * 0.5f), mode, tx, ty, toughEvery > 0 && i % toughEvery == toughEvery - 1)
        }
    }

    private fun makePlayer(look: Look, x: Float, y: Float, hp: Float, tagAr: String, tagEn: String, weapon: Int = 1) {
        val s = newSoldier(0, look, weapon, hp, x, y)
        s.isPlayer = true
        s.armor = 0.8f
        s.tagAr = tagAr
        s.tagEn = tagEn
        s.spd = 125f
        player = s
        this.weapon = weapon
    }

    private fun makeNamedAlly(look: Look, x: Float, y: Float, tagAr: String, tagEn: String, slot: Int): Soldier {
        val s = newSoldier(0, look, 2, 220f, x, y)
        s.dmgMul = 1f
        s.cdMul = 1.6f
        s.armor = 0.7f
        s.spd = 84f
        s.mode = 3
        s.slot = slot
        s.tagAr = tagAr
        s.tagEn = tagEn
        return s
    }

    // ------------------------------------------------------------------ mission setup
    fun loadMission(m: Int) {
        mission = m
        phase = 0
        t = 0f
        killCount = 0
        clearAll()
        waitCut = false
        cutId = 0
        when (m) {
            1 -> setupM1()
            2 -> setupM2()
            3 -> setupM3A()
            4 -> setupM4()
            else -> setupM5()
        }
    }

    private fun setupM1() {
        W = 900f; D = 2000f
        objId = 1
        makePlayer(Looks.SAVANNA, 450f, 1900f, 280f, "السافنا", "Savanna")
        for (i in 0 until 4) addAlly(420f + i * 20f, 1930f, i)
        tankX = 450f; tankY = 150f
        addBuilding(tankX, tankY, 80f, 50f, 100f, 2, 0)
        // village flanks
        for (i in 0 until 7) {
            addBuilding(if (i % 2 == 0) 70f else 830f, 1700f - i * 230f, 90f, 60f, 56f, 0, 0)
        }
        scatterCover(34, 60f, 840f, 300f, 1760f)
        // enemy groups
        group(6, 450f, 1500f, 190f, 0)
        group(8, 450f, 1150f, 220f, 0, toughEvery = 4)
        group(8, 450f, 820f, 240f, 0, toughEvery = 3)
        addTechnical(380f, 800f, 1, 0, 0)
        group(8, 450f, 480f, 240f, 0, toughEvery = 3)
        addTechnical(560f, 500f, 1, 0, 0)
        group(6, 450f, 270f, 170f, 0)
    }

    private fun setupM2() {
        W = 900f; D = 2000f
        objId = 3
        surveyX = 450f; surveyY = 360f
        val pl = newSoldier(0, Looks.QUBBA, 1, 100f, 450f, 1880f)
        pl.isPlayer = true
        pl.inVeh = true
        pl.tagAr = "القبة"; pl.tagEn = "Qubba"
        player = pl
        val v = Vehicle(450f, 1880f, 0, 1)
        v.hp = 520f; v.maxHp = 520f
        v.isPlayer = true
        v.spd = 125f
        vehicles.add(v)
        pveh = v
        for (i in 0 until 4) {
            val e = newSoldier(4, Looks.EXPERT, 6, 50f, 420f + i * 20f, 1960f + (i % 2) * 20f)
            e.armed = false
            e.expert = true
            e.mode = 4
            e.tx = surveyX + (i - 1.5f) * 26f
            e.ty = surveyY + 30f
            e.spd = 40f
        }
        for (i in 0 until 6) addAlly(380f + i * 28f, 1990f - (i % 3) * 18f, i, true, 6)
        // camp to be brought down
        addBuilding(450f, 150f, 150f, 80f, 70f, 1, 0)
        addBuilding(300f, 190f, 80f, 40f, 50f, 3, 0)
        addBuilding(600f, 190f, 80f, 40f, 50f, 3, 0)
        addBuilding(150f, 260f, 70f, 50f, 46f, 0, 0)
        addBuilding(750f, 260f, 70f, 50f, 46f, 0, 0)
        scatterCover(30, 40f, 860f, 450f, 1900f, 450f, 70f)
        planeT = 9f
        waveT = 4f
        techT = 26f
    }

    private fun setupM3A() {
        W = 900f; D = 1600f
        objId = 4
        makePlayer(Looks.SAVANNA, 450f, 1500f, 300f, "السافنا", "Savanna")
        for (i in 0 until 4) addAlly(420f + i * 20f, 1530f, i)
        val cr = addBuilding(450f, 250f, 120f, 70f, 70f, 1, 1, 620f)
        cr.hp = 620f; cr.maxHp = 620f
        addBuilding(280f, 300f, 80f, 44f, 50f, 3, 0)
        addBuilding(620f, 300f, 80f, 44f, 50f, 3, 0)
        addBuilding(130f, 380f, 80f, 50f, 50f, 5, 0)
        addBuilding(770f, 380f, 80f, 50f, 50f, 5, 0)
        scatterCover(32, 60f, 840f, 380f, 1420f)
        group(6, 450f, 1100f, 220f, 0)
        group(8, 450f, 780f, 240f, 0, toughEvery = 3)
        addTechnical(350f, 720f, 1, 0, 0)
        addTechnical(560f, 700f, 1, 0, 0)
        group(8, 450f, 440f, 220f, 0, toughEvery = 3)
        addElite(400f, 330f, 0, false)
        addElite(500f, 330f, 0, false)
        addElite(450f, 380f, 0, true)
        planeT = 20f
    }

    private fun setupM3B() {
        val keepWeapon = weapon
        clearAll()
        weapon = keepWeapon
        phase = 1
        W = 700f; D = 1400f
        objId = 5
        hearts = 6
        exitX = 350f; exitY = 1000f
        addBuilding(150f, 1000f, 300f, 50f, 70f, 4, 0)
        addBuilding(550f, 1000f, 300f, 50f, 70f, 4, 0)
        makePlayer(Looks.QUBBA, 350f, 880f, 300f, "القبة", "Qubba")
        for (i in 0 until 5) addAlly(310f + i * 20f, 910f, i, true)
        for (i in 0 until 7) addCover(110f + i * 80f, 830f + (i % 2) * 40f, 0)
        addCover(330f, 940f, 1)
        addCover(240f, 960f, 0)
        addCover(450f, 960f, 0)
        totalToSpawn = 40
        waveT = 3f
    }

    private fun setupM3C() {
        val keepWeapon = weapon
        clearAll()
        weapon = keepWeapon
        phase = 2
        W = 900f; D = 1200f
        objId = 6
        makePlayer(Looks.APOLLO, 450f, 1080f, 320f, "أبولو", "Apollo")
        for (i in 0 until 5) addAlly(410f + i * 20f, 1110f, i)
        addBuilding(450f, 160f, 120f, 70f, 70f, 1, 0)
        addBuilding(280f, 220f, 80f, 44f, 50f, 3, 0)
        addBuilding(620f, 220f, 80f, 44f, 50f, 3, 0)
        scatterCover(26, 60f, 840f, 380f, 1000f)
        for (i in 0 until 26) {
            val a = i * 6.2832f / 26f
            val s = addFalan(450f + kotlin.math.cos(a) * 150f, 520f + sin(a) * 90f, 2, 0f, 0f, i % 4 == 3)
            s.sight = 700f
        }
    }

    private fun setupM4() {
        W = 900f; D = 2000f
        objId = 7
        goalX = 450f; goalY = 300f
        val pl = newSoldier(0, Looks.QUBBA, 1, 100f, 450f, 1780f)
        pl.isPlayer = true
        pl.inVeh = true
        pl.tagAr = "القبة"; pl.tagEn = "Qubba"
        player = pl
        val v = Vehicle(450f, 1780f, 0, 1)
        v.hp = 560f; v.maxHp = 560f
        v.isPlayer = true
        v.spd = 125f
        vehicles.add(v)
        pveh = v
        makeNamedAlly(Looks.SAVANNA, 400f, 1830f, "السافنا", "Savanna", 0)
        for (i in 1..6) addAlly(350f + i * 30f, 1840f + (i % 2) * 20f, i, i % 2 == 0)
        // the Falanqayat who march with us (not yet aware of the trick)
        for (i in 0 until 10) {
            val s = addFalan(380f + (i % 5) * 28f, 1870f + (i / 5) * 26f, 3, 0f, 0f, i % 4 == 3)
            s.team = 5
            s.slot = 7 + i
            s.cdMul = 2.2f
            s.dmgMul = 0.8f
        }
        // Elite base behind us
        addBuilding(200f, 1960f, 100f, 60f, 60f, 5, 0)
        addBuilding(450f, 1980f, 120f, 70f, 66f, 0, 0)
        addBuilding(700f, 1960f, 100f, 60f, 60f, 5, 0)
        // Janja base far ahead
        addBuilding(450f, 150f, 130f, 70f, 64f, 5, 0)
        addBuilding(300f, 200f, 80f, 44f, 50f, 3, 0)
        addBuilding(600f, 200f, 80f, 44f, 50f, 3, 0)
        scatterCover(34, 40f, 860f, 420f, 1700f, 450f, 60f)
        cutId = 41
        waitCut = true
    }

    private fun setupM5() {
        W = 900f; D = 2000f
        objId = 10
        makePlayer(Looks.SAVANNA, 450f, 1900f, 300f, "السافنا", "Savanna")
        makeNamedAlly(Looks.QUBBA, 400f, 1930f, "القبة", "Qubba", 0)
        for (i in 0 until 4) addAlly(360f + i * 24f, 1960f, i + 1)
        for (i in 0 until 4) {
            val d = addElite(330f + i * 40f, 1990f, 3, false, 6)
            d.slot = i + 5
        }
        val a = addBuilding(220f, 1250f, 110f, 66f, 66f, 1, 1, 440f)
        val b = addBuilding(690f, 800f, 110f, 66f, 66f, 1, 1, 440f)
        val c = addBuilding(450f, 330f, 120f, 70f, 70f, 1, 1, 520f)
        a.hp = 440f; b.hp = 440f; c.hp = 520f
        addBuilding(450f, 1300f, 90f, 60f, 54f, 0, 0)
        addBuilding(80f, 900f, 90f, 60f, 54f, 0, 0)
        addBuilding(830f, 1350f, 90f, 60f, 54f, 0, 0)
        addBuilding(450f, 780f, 80f, 44f, 50f, 3, 0)
        scatterCover(40, 40f, 860f, 420f, 1800f)
        for (i in 0 until 5) addElite(180f + i * 30f, 1180f + (i % 2) * 40f, 0, i % 2 == 1)
        group(6, 300f, 1050f, 120f, 0, toughEvery = 3)
        for (i in 0 until 5) addElite(630f + i * 25f, 760f + (i % 2) * 40f, 0, i % 2 == 0)
        addTechnical(560f, 900f, 2, 0, 0)
        group(6, 520f, 700f, 140f, 0, toughEvery = 3)
        for (i in 0 until 5) addElite(380f + i * 30f, 300f + (i % 2) * 40f, 0, i % 2 == 1)
        group(5, 450f, 450f, 180f, 0, toughEvery = 2)
        addTechnical(350f, 420f, 1, 0, 0)
        planeT = 16f
        cutId = 51
        waitCut = true
    }

    private fun setupM5Dinari() {
        phase = 1
        objId = 11
        // clear leftovers of the Elites but keep our side
        for (s in soldiers) if (sideOf(s.team) == 1) s.dead = true
        vehicles.removeAll { sideOf(it.team) == 1 }
        bullets.clear()
        totalToSpawn = 22
        spawnedTotal = 0
        dinariDown = 0
        waveT = 1f
        planeT = 9999f
        val pl = player
        if (pl != null) {
            pl.hp = min(pl.maxHp, pl.hp + 120f)
        }
    }

    fun cutDone() {
        val id = cutId
        cutId = 0
        waitCut = false
        when (id) {
            31 -> setupM3B()
            32 -> setupM3C()
            42 -> betray()
            43 -> startDance(4)
            52 -> setupM5Dinari()
            53 -> startDance(5)
        }
    }

    private fun showCut(id: Int) {
        cutId = id
        waitCut = true
        // freeze input
        moveX = 0f; moveY = 0f; fireHeld = false
    }

    private fun betray() {
        betrayed = true
        objId = 8
        for (s in soldiers) {
            if (s.team == 5) {
                s.team = 1
                s.mode = 2
                s.sight = 600f
            }
        }
        for (i in 0 until 10) {
            val side = if (i % 2 == 0) -20f else W + 20f
            val s = addFalan(side, (player?.y ?: 1000f) + rf(-300f, 100f), 2, 0f, 0f, i % 3 == 2)
            s.sight = 700f
        }
    }

    private fun startDance(kind: Int) {
        dancing = true
        sceneKind = kind
        sceneT = 0f
        lastBeat = -1
        bullets.clear()
        musicOn = false
        if (kind != 5) {
            musicOn = true
            sfx.startMusic(if (kind == 4) Sfx.MUSIC_VICTORY else Sfx.MUSIC_END)
        }
    }

    // ------------------------------------------------------------------ queries
    fun nearestHostile(x: Float, y: Float, range: Float, side: Int, includeVeh: Boolean = true): Ent? {
        var best: Ent? = null
        var bd = range
        for (s in soldiers) {
            if (s.dead || s.inVeh || sideOf(s.team) == side) continue
            val d = hypot(s.x - x, s.y - y)
            if (d < bd) { bd = d; best = s }
        }
        if (includeVeh) {
            for (v in vehicles) {
                if (v.dead || sideOf(v.team) == side) continue
                val d = hypot(v.x - x, v.y - y) - 14f
                if (d < bd) { bd = d; best = v }
            }
        }
        return best
    }

    private fun leaderPos(s: Soldier): FloatArray {
        if (s.mode == 6) {
            var sx = 0f
            var sy = 0f
            var n = 0
            for (e in soldiers) if (e.expert && !e.dead) { sx += e.x; sy += e.y; n++ }
            if (n > 0) return floatArrayOf(sx / n, sy / n)
        }
        val v = pveh
        if (v != null && !v.dead) return floatArrayOf(v.x, v.y)
        val p = player
        if (p != null && !p.dead) return floatArrayOf(p.x, p.y)
        return floatArrayOf(s.x, s.y)
    }

    private val slotX = floatArrayOf(-50f, 50f, -90f, 90f, 0f, -130f, 130f, -40f, 40f, -80f, 80f, -120f, 120f, 0f, -160f, 160f, -20f)
    private val slotY = floatArrayOf(40f, 40f, 70f, 70f, 90f, 50f, 50f, 110f, 110f, 130f, 130f, 100f, 100f, 150f, 80f, 80f, 70f)

    private fun pickCover(s: Soldier, tx: Float, ty: Float) {
        var best: Cover? = null
        var bd = 230f
        val sd = hypot(s.x - tx, s.y - ty)
        for (cv in covers) {
            val d = hypot(cv.x - s.x, cv.y - s.y)
            if (d >= bd) continue
            val cd = hypot(cv.x - tx, cv.y - ty)
            if (cd > sd + 10f || cd < 90f) continue
            bd = d
            best = cv
        }
        if (best == null) {
            s.hasCover = false
            return
        }
        val ux = best.x - tx
        val uy = best.y - ty
        val ul = max(1f, hypot(ux, uy))
        val nx = ux / ul
        val ny = uy / ul
        s.cvx = best.x + nx * (best.w * 0.5f * abs(nx) + 16f)
        s.cvy = best.y + ny * (best.d * 0.5f * abs(ny) + 16f)
        s.hasCover = true
    }

    // ------------------------------------------------------------------ combat primitives
    private fun fireBullet(x: Float, y: Float, dx: Float, dy: Float, team: Int, w: Int, dmg: Float, spread: Float): Bullet {
        val l = max(0.001f, hypot(dx, dy))
        var ax = dx / l
        var ay = dy / l
        if (spread > 0f) {
            val a = (Random.nextFloat() - 0.5f) * 2f * spread
            val ca = kotlin.math.cos(a)
            val sa = sin(a)
            val nx = ax * ca - ay * sa
            val ny = ax * sa + ay * ca
            ax = nx; ay = ny
        }
        val sp = W_SPEED[w]
        val b = Bullet(x + ax * 14f, y + ay * 14f, ax * sp, ay * sp, team, dmg, if (w == 4) 2 else 0)
        b.life = if (w == 4) 1.6f else W_RANGE[w] / sp + 0.15f
        b.wpn = w
        bullets.add(b)
        return b
    }

    private fun blood(x: Float, y: Float, n: Int) {
        for (i in 0 until n) {
            val a = Random.nextFloat() * 6.2832f
            val sp = rf(30f, 120f)
            parts.add(Part(x, y, 24f, kotlin.math.cos(a) * sp, sin(a) * sp * 0.5f, rf(40f, 120f), 0.5f, 0.5f, android.graphics.Color.rgb(190, 20, 20), rf(1.6f, 3.2f), 0))
        }
    }

    private fun dust(x: Float, y: Float, n: Int) {
        for (i in 0 until n) {
            parts.add(Part(x + rf(-8f, 8f), y + rf(-4f, 4f), 6f, rf(-40f, 40f), rf(-20f, 20f), rf(10f, 40f), 0.7f, 0.7f, android.graphics.Color.rgb(222, 196, 150), rf(4f, 8f), 1))
        }
    }

    fun hurtSoldier(s: Soldier, dmg: Float) {
        if (s.dead) return
        s.hp -= dmg * s.armor
        s.flash = 0.18f
        blood(s.x, s.y, 3)
        if (s.isPlayer) {
            hurtV = max(hurtV, 0.35f)
            sfx.play(Sfx.HURT, 0.5f, 1.1f)
        }
        if (s.hp <= 0f) {
            s.dead = true
            s.deadT = 6f
            s.flash = 1f
            if (sideOf(s.team) == 1) killCount++
            if (s.team == 3) dinariDown++
            if (!s.isPlayer) sfx.play(Sfx.SCREAM, 0.5f, rf(0.9f, 1.3f))
            blood(s.x, s.y, 10)
        }
    }

    private fun hurtVehicle(v: Vehicle, dmg: Float) {
        if (v.dead) return
        v.hp -= dmg * (if (v.isPlayer) 0.7f else 1f)
        v.flash = 0.15f
        if (v.isPlayer) hurtV = max(hurtV, 0.3f)
        if (v.hp <= 0f) {
            v.dead = true
            v.deadT = 30f
            explode(v.x, v.y, 70f, 60f, sideOf(v.team), false)
            if (v.isPlayer) {
                val p = player
                if (p != null) { p.dead = true; p.hp = 0f }
            }
        }
    }

    private fun hurtBuilding(b: Building, dmg: Float) {
        if (b.dead || b.side != 1) return
        b.hp -= dmg
        b.flash = 0.12f
        if (b.hp <= 0f) {
            b.dead = true
            for (k in 0 until 4) {
                booms.add(Boom(b.x + rf(-b.w * 0.4f, b.w * 0.4f), b.y + rf(-b.d * 0.3f, b.d * 0.3f), rf(40f, 70f)))
            }
            sfx.play(Sfx.BOOM, 1f, 0.8f)
            shake = max(shake, 0.8f)
            for (i in 0 until 14) {
                parts.add(Part(b.x + rf(-b.w * 0.4f, b.w * 0.4f), b.y, 20f, rf(-30f, 30f), rf(-10f, 10f), rf(30f, 90f), 2f, 2f, android.graphics.Color.rgb(70, 66, 62), rf(8f, 16f), 3))
            }
        }
    }

    /** srcSide: 0 = our artillery/rockets, 1 = the enemy's. Friendly fire is reduced. */
    fun explode(x: Float, y: Float, r: Float, dmg: Float, srcSide: Int, sound: Boolean = true) {
        booms.add(Boom(x, y, r))
        if (sound) sfx.play(Sfx.BOOM, 0.8f, rf(0.85f, 1.15f))
        val pl = player
        if (pl != null) {
            val d = hypot(pl.x - x, pl.y - y)
            if (d < 500f) shake = max(shake, 0.5f * (1f - d / 500f) + 0.1f)
        }
        for (s in soldiers) {
            if (s.dead || s.inVeh) continue
            val d = hypot(s.x - x, s.y - y)
            if (d < r + 8f) {
                val k = (1f - d / (r + 8f) * 0.6f) * (if (sideOf(s.team) == srcSide) 0.35f else 1f)
                hurtSoldier(s, dmg * k)
            }
        }
        for (v in vehicles) {
            if (v.dead) continue
            val d = hypot(v.x - x, v.y - y)
            if (d < r + 24f) hurtVehicle(v, dmg * (1f - d / (r + 24f) * 0.5f) * (if (sideOf(v.team) == srcSide) 0.35f else 1f))
        }
        if (srcSide == 0) {
            for (b in buildings) {
                if (b.dead || b.side != 1) continue
                if (abs(b.x - x) < b.w / 2f + r * 0.6f && abs(b.y - y) < b.d / 2f + r * 0.6f) hurtBuilding(b, dmg * 1.4f)
            }
        }
        for (i in 0 until 8) {
            parts.add(Part(x + rf(-r * 0.3f, r * 0.3f), y + rf(-r * 0.2f, r * 0.2f), 10f, rf(-40f, 40f), rf(-20f, 20f), rf(30f, 80f), 1.1f, 1.1f, android.graphics.Color.rgb(70, 66, 62), rf(8f, 14f), 1))
        }
    }

    // ------------------------------------------------------------------ player control
    private var asScore = 1e9f
    private val asPos = FloatArray(2)
    private var asHas = false

    private fun consider(px: Float, py: Float, fx: Float, fy: Float, tx: Float, ty: Float, range: Float, cone: Float, penalty: Float) {
        val dx = tx - px
        val dy = ty - py
        val d = hypot(dx, dy)
        if (d < 4f || d > range) return
        val c = ((dx * fx + dy * fy) / d).coerceIn(-1f, 1f)
        val ang = kotlin.math.acos(c)
        if (ang > cone) return
        val sc = ang * 200f + d * 0.1f + penalty
        if (sc < asScore) {
            asScore = sc
            asPos[0] = tx
            asPos[1] = ty
            asHas = true
        }
    }

    /** looks for something to snap the crosshair to; result in asPos / asHas */
    private fun findAssist(px: Float, py: Float, fx: Float, fy: Float, range: Float, cone: Float, air: Boolean) {
        asScore = 1e9f
        asHas = false
        for (s in soldiers) {
            if (s.dead || s.inVeh || sideOf(s.team) == 0) continue
            consider(px, py, fx, fy, s.x, s.y, range, cone, 0f)
        }
        for (v in vehicles) {
            if (v.dead || sideOf(v.team) == 0) continue
            consider(px, py, fx, fy, v.x, v.y, range, cone, 0f)
        }
        for (b in buildings) {
            if (b.dead || b.side != 1) continue
            consider(px, py, fx, fy, b.x, b.y + b.d * 0.3f, range, cone, 260f)
        }
        if (air) {
            for (a in planes) {
                if (a.dead) continue
                val d = hypot(a.x - px, a.y - py)
                val lead = d / 900f
                consider(px, py, fx, fy, a.x + a.vx * lead, a.y, range, cone * 1.4f, -40f)
            }
        }
    }

    fun selectWeapon(i: Int) {
        if (pveh != null) return
        val n = i.coerceIn(0, NW - 1)
        if (n == weapon) return
        weapon = n
        reloadT = 0f
        scoped = false; scopeLvl = 0
        sfx.play(Sfx.CLICK, 0.6f, 1.3f)
        if (ammo[weapon] <= 0) startReload()
    }

    fun switchWeapon() {
        selectWeapon((weapon + 1) % NW)
    }

    fun startReload() {
        if (reloadT > 0f || pveh != null) return
        if (ammo[weapon] >= W_MAG[weapon] || reserve[weapon] <= 0) return
        reloadT = W_REL[weapon]
        sfx.play(Sfx.CLICK, 0.8f, 0.6f)
    }

    fun toggleScope() {
        if (pveh != null || !canScope(weapon)) return
        if (!scoped) { scoped = true; scopeLvl = 1 }
        else if (scopeLvl < scopeLevels(weapon)) scopeLvl++
        else { scoped = false; scopeLvl = 0 }
    }

    /** current zoom factor of the scope (1 = none) */
    fun zoom(): Float {
        if (!scoped) return 1f
        return when (weapon) {
            7 -> if (scopeLvl >= 2) 8f else 4f
            11 -> if (scopeLvl >= 2) 12f else 6f
            10 -> 3f
            else -> 2f
        }
    }

    fun toggleCrouch() { if (pveh == null) crouching = !crouching }

    fun callSupport() {
        if (supportCd > 0f || waitCut || dancing) return
        val pl = player ?: return
        val e = nearestHostile(pl.x, pl.y, 750f, 0, true)
        val cx: Float
        val cy: Float
        if (e != null) {
            cx = e.x; cy = e.y
        } else {
            cx = pl.x; cy = pl.y - 260f
        }
        supportCd = 26f
        for (i in 0 until 7) {
            shells.add(Shell(cx + rf(-85f, 85f), cy + rf(-60f, 60f), 1.3f + i * 0.2f, 62f, 95f, 0))
        }
        sfx.play(Sfx.CLICK, 0.9f, 0.6f)
    }

    private fun wrapA(a: Float): Float {
        var r = a
        while (r > 3.14159265f) r -= 6.2831853f
        while (r < -3.14159265f) r += 6.2831853f
        return r
    }

    private fun updatePlayer(dt: Float) {
        val pl = player ?: return
        if (pl.dead) return
        pl.flash = max(0f, pl.flash - dt * 3f)
        if (supportCd > 0f) supportCd -= dt
        recoil = max(0f, recoil - dt * 0.07f)
        val fx = kotlin.math.cos(camYaw)
        val fy = sin(camYaw)
        val mx = fx * (-moveY) + (-fy) * moveX
        val my = fy * (-moveY) + fx * moveX
        val mlen = min(1f, hypot(moveX, moveY))
        val v = pveh
        if (v != null && !v.dead) {
            // ---- driving the 4x4 with the 23 mm cannon
            if (mlen > 0.15f) {
                val th = kotlin.math.atan2(my, mx)
                val diff = wrapA(th - v.yaw)
                var throttle: Float
                if (abs(diff) <= 1.75f) {
                    throttle = mlen * max(0.3f, kotlin.math.cos(diff))
                    val turn = 1.9f * (0.4f + min(1f, abs(v.speed) / 55f))
                    v.yaw += diff.coerceIn(-turn * dt, turn * dt)
                } else {
                    throttle = -mlen * 0.5f
                    val dr = wrapA(th + 3.14159265f - v.yaw)
                    val turn = 1.6f * (0.4f + min(1f, abs(v.speed) / 55f))
                    v.yaw += dr.coerceIn(-turn * dt, turn * dt)
                }
                val tgtSpeed = throttle * v.spd * 1.4f
                v.speed += (tgtSpeed - v.speed) * min(1f, dt * 1.6f)
            } else {
                v.speed += (0f - v.speed) * min(1f, dt * 2.6f)
            }
            v.x += kotlin.math.cos(v.yaw) * v.speed * dt
            v.y += sin(v.yaw) * v.speed * dt
            v.moving = abs(v.speed) > 4f
            v.wheel += v.speed * dt * 0.12f
            // turret follows the camera, snapping to aircraft and ground targets
            findAssist(v.x, v.y, fx, fy, 700f, 0.4f, true)
            var ax = fx
            var ay = fy
            assistLock = asHas
            if (asHas) {
                val dx = asPos[0] - v.x
                val dy = asPos[1] - v.y
                val l = max(0.001f, hypot(dx, dy))
                ax = dx / l * 0.85f + fx * 0.15f
                ay = dy / l * 0.85f + fy * 0.15f
            }
            val ta = kotlin.math.atan2(ay, ax)
            val td = wrapA(ta - v.turret)
            v.turret += td.coerceIn(-4.2f * dt, 4.2f * dt)
            v.aimX = kotlin.math.cos(v.turret)
            v.aimY = sin(v.turret)
            v.cd -= dt
            if (v.muzzle > 0f) v.muzzle -= dt
            if (fireHeld && v.cd <= 0f && abs(td) < 0.25f) {
                v.cd = 0.11f
                v.muzzle = 0.06f
                val bx = v.x - kotlin.math.cos(v.yaw) * 9f + v.aimX * 34f
                val by = v.y - sin(v.yaw) * 9f + v.aimY * 34f
                val sp = 0.025f + (if (v.moving) 0.02f else 0f)
                val a = v.turret + (Random.nextFloat() - 0.5f) * 2f * sp
                val b = Bullet(bx, by, kotlin.math.cos(a) * 900f, sin(a) * 900f, 0, 30f, 1)
                b.life = 0.85f
                bullets.add(b)
                sfx.play(Sfx.CANNON, 0.55f, rf(0.95f, 1.1f))
                shake = max(shake, 0.05f)
            }
            pl.x = v.x
            pl.y = v.y
            pl.aimX = v.aimX
            pl.aimY = v.aimY
            return
        }
        // ---- infantry (Free Fire style: the camera direction is the aim direction)
        val sniperScoped = scoped && canScope(weapon)
        val zm = zoom()
        findAssist(pl.x, pl.y, fx, fy, W_RANGE[weapon], if (sniperScoped) 0.07f else 0.2f, weapon == 4)
        var ax = fx
        var ay = fy
        assistLock = asHas
        if (asHas) {
            val dx = asPos[0] - pl.x
            val dy = asPos[1] - pl.y
            val l = max(0.001f, hypot(dx, dy))
            val k = if (sniperScoped) 0.4f else if (weapon == 7 || weapon == 11) 0.55f else 0.8f
            ax = dx / l * k + fx * (1f - k)
            ay = dy / l * k + fy * (1f - k)
            val ll = max(0.001f, hypot(ax, ay))
            ax /= ll; ay /= ll
            crossDist = l
        } else {
            crossDist = if (sniperScoped) 220f else 110f
        }
        // scope sway and breath holding
        swayT += dt
        if (holdBreath && sniperScoped && breath > 0.02f) breath = max(0f, breath - dt * 0.28f)
        else { holdBreath = false; breath = min(1f, breath + dt * 0.2f) }
        if (sniperScoped) {
            val amp = 0.0022f * zm * (if (holdBreath) 0.08f else 1f) * (if (crouching) 0.6f else 1f) * (if (pl.moving) 2.5f else 1f)
            val sa = sin(swayT * 1.7f) * amp + sin(swayT * 3.1f) * amp * 0.4f
            val c1 = kotlin.math.cos(sa); val s1 = sin(sa)
            val nx = ax * c1 - ay * s1
            val ny = ax * s1 + ay * c1
            ax = nx; ay = ny
        }
        pl.aimX = ax
        pl.aimY = ay
        // movement with acceleration, sprint and crouch
        crouchT += ((if (crouching) 1f else 0f) - crouchT) * min(1f, dt * 9f)
        sprinting = mlen > 0.92f && !fireHeld && !sniperScoped && !crouching && reloadT <= 0f
        var spdMul = 1f
        if (weapon == 8) spdMul = 0.78f else if (weapon == 4 || weapon == 11) spdMul = 0.88f else if (weapon == 7) spdMul = 0.95f
        if (sniperScoped) spdMul *= 0.45f
        if (crouching) spdMul *= 0.55f
        if (sprinting) spdMul *= 1.3f
        val ml = max(0.001f, hypot(mx, my))
        val tvx = if (mlen > 0.12f) mx / ml * pl.spd * mlen * spdMul else 0f
        val tvy = if (mlen > 0.12f) my / ml * pl.spd * mlen * spdMul else 0f
        val acc = min(1f, dt * (if (mlen > 0.12f) 9f else 12f))
        pvx += (tvx - pvx) * acc
        pvy += (tvy - pvy) * acc
        val sp2 = hypot(pvx, pvy)
        pl.x += pvx * dt
        pl.y += pvy * dt
        if (sp2 > 10f) {
            pl.moving = true
            pl.phase += dt * (sp2 / pl.spd) * 11f
        } else {
            pl.moving = false
        }
        // keep the body facing the aim direction (strafing keeps the gun forward)
        pCd -= dt
        kick = max(0f, kick - dt * 9f)
        hitT = max(0f, hitT - dt)
        var pi2 = pops.size - 1
        while (pi2 >= 0) { val pp = pops[pi2]; pp.t += dt; if (pp.t > 0.9f) pops.removeAt(pi2); pi2-- }
        if (pl.muzzle > 0f) pl.muzzle -= dt
        if (reloadT > 0f) {
            reloadT -= dt
            if (reloadT <= 0f) {
                val take = min(W_MAG[weapon] - ammo[weapon], reserve[weapon])
                ammo[weapon] += take
                reserve[weapon] -= take
                sfx.play(Sfx.CLICK, 0.8f, 0.8f)
            }
        }
        if (fireHeld && pCd <= 0f && reloadT <= 0f && !sprinting) {
            if (ammo[weapon] <= 0) {
                if (reserve[weapon] > 0) startReload() else { pCd = 0.4f; sfx.play(Sfx.CLICK, 0.5f, 1.6f) }
            } else {
                ammo[weapon]--
                pCd = W_CD[weapon]
                pl.muzzle = 0.07f
                var spread = W_SPREAD[weapon] + recoil
                if (pl.moving) spread += 0.02f * min(1f, sp2 / pl.spd)
                if (crouching) spread *= 0.6f
                if (sniperScoped) spread *= 0.3f
                for (pi in 0 until W_PEL[weapon]) fireBullet(pl.x, pl.y, pl.aimX, pl.aimY, 0, weapon, W_DMG[weapon], spread).fromPlayer = true
                recoil = min(0.12f, recoil + W_RECOIL[weapon] * (if (crouching) 0.7f else 1f))
                kick = if (W_CD[weapon] < 0.2f) 0.45f else 1f
                val w = weapon
                sfx.play(if (w == 4) Sfx.BOOM else if (w == 6 || w == 7 || w == 3 || w == 11 || w == 10) Sfx.CANNON else Sfx.SHOT, if (w == 4) 0.5f else 0.8f,
                    if (w == 1) 1.15f else if (w == 2) 0.85f else if (w == 3) 0.6f else if (w == 5) 1.35f else if (w == 8) 0.7f else if (w == 7 || w == 11) 0.55f else if (w == 9) 1.05f else if (w == 10) 0.8f else 1f)
                if (w == 3 || w == 4 || w == 6 || w == 7 || w == 10 || w == 11) shake = max(shake, if (w == 11) 0.2f else 0.12f)
                if (ammo[weapon] <= 0 && reserve[weapon] > 0) startReload()
            }
        }
    }

    private fun coverPush(v: Vehicle) {
        for (cv in covers) {
            val hx = cv.w / 2f + 20f
            val hy = cv.d / 2f + 20f
            val dx = v.x - cv.x
            val dy = v.y - cv.y
            if (abs(dx) < hx && abs(dy) < hy) {
                val px = hx - abs(dx)
                val py = hy - abs(dy)
                if (px < py) {
                    if (dx >= 0f) v.x += px else v.x -= px
                } else {
                    if (dy >= 0f) v.y += py else v.y -= py
                }
                v.speed *= 0.6f
            }
        }
    }

    // ------------------------------------------------------------------ AI
    private fun moveToward(s: Soldier, gx: Float, gy: Float, sp: Float, dt: Float): Boolean {
        val dx = gx - s.x
        val dy = gy - s.y
        val d = hypot(dx, dy)
        if (d < 6f) return false
        val step = min(d, sp * dt)
        s.x += dx / d * step
        s.y += dy / d * step
        s.phase += dt * 10f
        return true
    }

    private fun updateSoldier(s: Soldier, dt: Float) {
        if (s.dead) { s.deadT -= dt; return }
        if (s.isPlayer) return
        s.cd -= dt
        if (s.muzzle > 0f) s.muzzle -= dt
        if (s.flash > 0f) s.flash -= dt * 3f
        val side = sideOf(s.team)
        if (s.team == 3) {
            // the Dinari are cowards: they run away at the first sign of danger
            if (!s.fleeing) {
                val close = nearestHostile(s.x, s.y, 150f, side, false)
                if (s.hp < s.maxHp * 0.85f || dinariDown >= 2 + s.slot || close != null) {
                    s.fleeing = true
                    s.armed = false
                    s.mode = 5
                    sfx.play(Sfx.SCREAM, 0.3f, 1.6f)
                }
            }
            if (s.fleeing) {
                val gx0 = s.x + (if (s.x < W / 2f) -30f else 30f)
                s.moving = moveToward(s, gx0, -40f, s.spd * 1.6f, dt)
                s.aimX = 0f
                s.aimY = -1f
                if (s.y < 28f) { s.dead = true; s.deadT = 0f }
                return
            }
        }
        val tgt: Ent? = if (s.mode == 4) null else nearestHostile(s.x, s.y, s.sight, side)
        var gx = s.x
        var gy = s.y
        var go = false
        var sp = s.spd
        when (s.mode) {
            0 -> {
                if (tgt != null) {
                    val d = hypot(tgt.x - s.x, tgt.y - s.y)
                    s.coverT -= dt
                    if (s.coverT <= 0f) { s.coverT = 1.3f + Random.nextFloat(); pickCover(s, tgt.x, tgt.y) }
                    if (s.hasCover) { gx = s.cvx; gy = s.cvy; go = true }
                    else if (d > 250f) { gx = tgt.x; gy = tgt.y; go = true }
                } else {
                    s.hasCover = false
                }
            }
            1 -> { gx = s.tx; gy = s.ty; go = true }
            2 -> {
                val h = nearestHostile(s.x, s.y, 6000f, side)
                val stopD = if (s.team == 3) 290f else 170f
                if (h != null && hypot(h.x - s.x, h.y - s.y) > stopD) { gx = h.x; gy = h.y; go = true }
            }
            3, 6 -> {
                val lp = leaderPos(s)
                val sl = s.slot.coerceIn(0, slotX.size - 1)
                val fx = lp[0] + slotX[sl]
                val fy = lp[1] + slotY[sl]
                if (tgt != null) {
                    s.coverT -= dt
                    if (s.coverT <= 0f) { s.coverT = 1.3f + Random.nextFloat(); pickCover(s, tgt.x, tgt.y) }
                    if (s.hasCover && hypot(s.cvx - lp[0], s.cvy - lp[1]) < 280f) { gx = s.cvx; gy = s.cvy; go = true }
                    else if (hypot(fx - s.x, fy - s.y) > 120f) { gx = fx; gy = fy; go = true }
                } else {
                    s.hasCover = false
                    if (hypot(fx - s.x, fy - s.y) > 30f) { gx = fx; gy = fy; go = true }
                    if (hypot(lp[0] - s.x, lp[1] - s.y) > 260f) sp = s.spd * 1.5f
                }
            }
            4 -> {
                gx = s.tx; gy = s.ty; go = true
            }
            else -> {
            }
        }
        s.moving = false
        if (go) s.moving = moveToward(s, gx, gy, sp, dt)
        if (tgt != null) {
            val dx = tgt.x - s.x
            val dy = tgt.y - s.y
            val d = max(0.001f, hypot(dx, dy))
            s.aimX = dx / d
            s.aimY = dy / d
            val w = s.weapon.coerceIn(0, NW - 1)
            if (s.armed && d <= W_RANGE[w] && s.cd <= 0f) {
                s.cd = W_CD[w] * s.cdMul + Random.nextFloat() * 0.2f
                s.muzzle = 0.07f
                fireBullet(s.x, s.y, dx, dy, s.team, w, W_DMG[w] * s.dmgMul, W_SPREAD[w] + 0.05f + d / 4000f)
                if (Random.nextInt(3) == 0) sfx.play(Sfx.SHOT, 0.12f, rf(0.8f, 1.1f))
            }
        } else if (s.moving) {
            val dx = gx - s.x
            val dy = gy - s.y
            val d = max(0.001f, hypot(dx, dy))
            s.aimX = dx / d
            s.aimY = dy / d
        }
    }

    private fun updateVehicle(v: Vehicle, dt: Float) {
        if (v.dead) { v.deadT -= dt; return }
        if (v.isPlayer) return
        v.cd -= dt
        if (v.muzzle > 0f) v.muzzle -= dt
        if (v.flash > 0f) v.flash -= dt * 3f
        val side = sideOf(v.team)
        if (v.team == 3) {
            if (!v.fleeing && v.hp < v.maxHp * 0.75f) { v.fleeing = true; sfx.play(Sfx.JET, 0.3f, 1.4f) }
            if (v.fleeing) {
                val diffF = wrapA(-1.5708f - v.yaw)
                val turnF = 1.7f * (0.4f + min(1f, abs(v.speed) / 55f))
                v.yaw += diffF.coerceIn(-turnF * dt, turnF * dt)
                v.speed += (v.spd * 1.15f - v.speed) * min(1f, dt * 1.5f)
                v.x += kotlin.math.cos(v.yaw) * v.speed * dt
                v.y += sin(v.yaw) * v.speed * dt
                v.moving = true
                v.wheel += v.speed * dt * 0.12f
                v.turret += wrapA(v.yaw - v.turret).coerceIn(-2f * dt, 2f * dt)
                if (v.y < 34f) { v.dead = true; v.deadT = 0f }
                return
            }
        }
        val tgt = nearestHostile(v.x, v.y, 650f, side)
        var goal = false
        var gx = v.x
        var gy = v.y
        if (tgt != null) {
            val dx = tgt.x - v.x
            val dy = tgt.y - v.y
            val d = max(0.001f, hypot(dx, dy))
            val ta = kotlin.math.atan2(dy, dx)
            val td = wrapA(ta - v.turret)
            v.turret += td.coerceIn(-3f * dt, 3f * dt)
            v.aimX = kotlin.math.cos(v.turret)
            v.aimY = sin(v.turret)
            if (d > 240f) { gx = tgt.x; gy = tgt.y; goal = true }
            if (d < 480f && v.cd <= 0f && abs(td) < 0.15f) {
                v.cd = 0.16f
                v.muzzle = 0.06f
                fireBullet(v.x + v.aimX * 20f, v.y + v.aimY * 20f, dx, dy, v.team, 1, 9f, 0.1f)
                if (Random.nextInt(4) == 0) sfx.play(Sfx.SHOT, 0.18f, 0.7f)
            }
        } else if (v.mode == 1) {
            gx = v.tx; gy = v.ty; goal = true
            v.turret += wrapA(v.yaw - v.turret).coerceIn(-2f * dt, 2f * dt)
        }
        var throttle = 0f
        if (goal) {
            val th = kotlin.math.atan2(gy - v.y, gx - v.x)
            val diff = wrapA(th - v.yaw)
            val turn = 1.7f * (0.4f + min(1f, abs(v.speed) / 55f))
            v.yaw += diff.coerceIn(-turn * dt, turn * dt)
            throttle = 0.75f * max(0.15f, kotlin.math.cos(diff))
        }
        v.speed += (throttle * v.spd - v.speed) * min(1f, dt * 1.5f)
        v.x += kotlin.math.cos(v.yaw) * v.speed * dt
        v.y += sin(v.yaw) * v.speed * dt
        v.moving = abs(v.speed) > 4f
        v.wheel += v.speed * dt * 0.12f
    }

    private fun spawnPlane() {
        val v = pveh
        val pl = player
        val ty = (v?.y ?: pl?.y ?: 1000f) + rf(-160f, 40f)
        val fromLeft = Random.nextBoolean()
        val p = Plane(if (fromLeft) -80f else W + 80f, ty, if (fromLeft) 200f else -200f)
        p.hp = 70f
        p.maxHp = 70f
        planes.add(p)
    }

    private fun updatePlane(a: Plane, dt: Float) {
        if (a.dead) return
        if (a.flash > 0f) a.flash -= dt * 3f
        a.x += a.vx * dt
        if (!a.warned && a.x > 0f && a.x < W) {
            a.warned = true
            sfx.play(Sfx.JET, 0.7f, 1f)
        }
        a.bombCd -= dt
        if (a.bombCd <= 0f && a.x > 0f && a.x < W) {
            // bomb something of ours near the flight path
            var best: Ent? = null
            var bd = 260f
            for (s in soldiers) {
                if (s.dead || s.inVeh || sideOf(s.team) != 0) continue
                val d = abs(s.x - a.x) + abs(s.y - a.y) * 0.4f
                if (d < bd) { bd = d; best = s }
            }
            for (v in vehicles) {
                if (v.dead || sideOf(v.team) != 0) continue
                val d = abs(v.x - a.x) + abs(v.y - a.y) * 0.4f
                if (d < bd) { bd = d; best = v }
            }
            if (best != null) {
                shells.add(Shell(a.x + a.vx * 0.5f, a.y + 14f, 0.9f, 52f, 70f, 1))
                a.bombCd = rf(0.7f, 1.2f)
            } else {
                a.bombCd = 0.3f
            }
        }
        if (a.x < -140f || a.x > W + 140f) a.dead = true
        if (a.hp <= 0f) {
            a.dead = true
            explode(a.x, a.y, 70f, 30f, 0, true)
            for (i in 0 until 12) {
                parts.add(Part(a.x, a.y, 110f, rf(-80f, 80f), rf(-30f, 30f), rf(-20f, 60f), 1.4f, 1.4f, android.graphics.Color.rgb(60, 56, 54), rf(6f, 12f), 1))
            }
        }
    }

    private fun solidPush(e: Ent, pad: Float) {
        for (b in buildings) {
            if (!b.solid) continue
            if (b.dead && b.side == 1) continue
            val hx = b.w / 2f + pad
            val hy = b.d / 2f + pad * 0.6f
            val dx = e.x - b.x
            val dy = e.y - b.y
            if (abs(dx) < hx && abs(dy) < hy) {
                val px = hx - abs(dx)
                val py = hy - abs(dy)
                if (px < py) {
                    if (dx >= 0f) e.x += px else e.x -= px
                } else {
                    if (dy >= 0f) e.y += py else e.y -= py
                }
            }
        }
    }

    private fun updateBullets(dt: Float) {
        var i = bullets.size - 1
        while (i >= 0) {
            val b = bullets[i]
            var remove = false
            for (st in 0 until 2) {
                b.x += b.vx * dt * 0.5f
                b.y += b.vy * dt * 0.5f
                b.life -= dt * 0.5f
                if (b.life <= 0f || b.x < -80f || b.x > W + 80f || b.y < -80f || b.y > D + 80f) {
                    remove = true
                    if (b.kind == 2) explode(b.x, b.y, 60f, 90f, sideOf(b.team))
                    break
                }
                val bs = sideOf(b.team)
                // cover and solid buildings block bullets
                var blocked = false
                for (cv in covers) {
                    // low sandbags and wrecks do not stop the player's shots (shots go over the top)
                    if (b.fromPlayer && cv.kind != 1) continue
                    if (abs(b.x - cv.x) < cv.w / 2f && abs(b.y - cv.y) < cv.d / 2f) {
                        // a shooter standing right behind the cover can fire over it
                        if (!(abs(b.ox - cv.x) < cv.w / 2f + 26f && abs(b.oy - cv.y) < cv.d / 2f + 26f)) { blocked = true; break }
                    }
                }
                if (!blocked) {
                    for (bd in buildings) {
                        if (!bd.solid || (bd.dead && bd.side == 1)) continue
                        if (abs(b.x - bd.x) < bd.w / 2f && abs(b.y - bd.y) < bd.d / 2f) {
                            if (bd.side == 1 && bs == 0) hurtBuilding(bd, b.dmg * (if (b.kind == 2) 0f else 0.6f))
                            blocked = true
                            break
                        }
                    }
                }
                if (blocked) {
                    remove = true
                    if (b.kind == 2) explode(b.x, b.y, 60f, 90f, bs)
                    else if (b.kind == 1) explode(b.x, b.y, 26f, 14f, bs, false)
                    else dust(b.x, b.y, 2)
                    break
                }
                // aircraft (cannon and rockets only)
                if (b.kind != 0 && bs == 0) {
                    for (a in planes) {
                        if (a.dead) continue
                        if (hypot(a.x - b.x, a.y - b.y) < 42f) {
                            a.hp -= b.dmg
                            a.flash = 0.15f
                            if (b.kind == 2) explode(b.x, b.y, 50f, 40f, 0)
                            remove = true
                            break
                        }
                    }
                    if (remove) break
                }
                // vehicles
                for (v in vehicles) {
                    if (v.dead || sideOf(v.team) == bs) continue
                    if (hypot(b.x - v.x, b.y - v.y) < 26f) {
                        hurtVehicle(v, b.dmg)
                        if (b.kind == 2) explode(b.x, b.y, 60f, 90f, bs)
                        else if (b.kind == 1) explode(b.x, b.y, 24f, 10f, bs, false)
                        else dust(b.x, b.y, 2)
                        remove = true
                        break
                    }
                }
                if (remove) break
                // soldiers
                for (s in soldiers) {
                    if (s.dead || s.inVeh || sideOf(s.team) == bs) continue
                    val hr = if (b.fromPlayer) 15f else 11f
                    if (abs(b.x - s.x) < hr && abs(b.y - s.y) < hr) {
                        var dm = b.dmg
                        if (b.fromPlayer && b.kind == 0) {
                            val tr = hypot(b.x - b.ox, b.y - b.oy) / W_RANGE[b.wpn]
                            if (b.wpn != 7 && b.wpn != 11 && b.wpn != 10) dm *= (1f - 0.4f * min(1f, tr))
                            val crit = Random.nextFloat() < W_CRIT[b.wpn]
                            if (crit) dm *= 1.9f
                            val wasAlive = !s.dead
                            hurtSoldier(s, dm)
                            hitT = 0.28f; hitCrit = crit; hitKill = wasAlive && s.dead
                            pops.add(Pop(s.x, s.y, dm.toInt(), crit))
                        } else {
                            hurtSoldier(s, dm)
                        }
                        if (b.kind == 2) explode(b.x, b.y, 60f, 90f, bs)
                        else if (b.kind == 1) explode(b.x, b.y, 22f, 8f, bs, false)
                        remove = true
                        break
                    }
                }
                if (remove) break
            }
            if (remove) bullets.removeAt(i)
            i--
        }
    }

    private fun updateEffects(dt: Float) {
        var i = shells.size - 1
        while (i >= 0) {
            val s = shells[i]
            s.t -= dt
            if (s.t <= 0f) {
                explode(s.x, s.y, s.r, s.dmg, s.srcSide)
                shells.removeAt(i)
            }
            i--
        }
        i = booms.size - 1
        while (i >= 0) {
            val b = booms[i]
            b.t += dt
            if (b.t > 0.6f) booms.removeAt(i)
            i--
        }
        i = parts.size - 1
        while (i >= 0) {
            val q = parts[i]
            q.life -= dt
            if (q.life <= 0f) { parts.removeAt(i); i--; continue }
            q.x += q.vx * dt
            q.y += q.vy * dt
            q.z += q.vz * dt
            if (q.kind == 0) q.vz -= 380f * dt
            else q.vz += 10f * dt
            if (q.z < 0f) { q.z = 0f; q.vz = 0f }
            i--
        }
        if (shake > 0f) shake = max(0f, shake - dt * 1.6f)
        if (hurtV > 0f) hurtV = max(0f, hurtV - dt * 1.5f)
    }

    // ------------------------------------------------------------------ main update
    fun update(dt0: Float) {
        if (waitCut) return
        val dt = min(dt0, 0.05f)
        if (dancing) {
            sceneT += dt
            val beat = sceneT / BEAT
            val b = beat.toInt()
            if (sceneKind == 5 && !musicOn && sceneT >= TREATY_T) {
                musicOn = true
                sfx.startMusic(Sfx.MUSIC_END)
            }
            if (b != lastBeat) {
                lastBeat = b
                if (sceneKind != 5 || sceneT >= TREATY_T) sfx.play(Sfx.STOMP, 0.55f)
            }
            return
        }
        t += dt
        updatePlayer(dt)
        for (s in soldiers) updateSoldier(s, dt)
        for (v in vehicles) updateVehicle(v, dt)
        for (a in planes) updatePlane(a, dt)
        // separation + bounds + solids
        for (i in 0 until soldiers.size) {
            val a = soldiers[i]
            if (a.dead || a.inVeh) continue
            for (j in i + 1 until soldiers.size) {
                val b = soldiers[j]
                if (b.dead || b.inVeh) continue
                val dx = a.x - b.x
                val dy = a.y - b.y
                if (abs(dx) < 13f && abs(dy) < 13f) {
                    val d = hypot(dx, dy)
                    if (d in 0.01f..13f) {
                        val push = (13f - d) * 0.5f
                        if (!a.isPlayer) { a.x += dx / d * push; a.y += dy / d * push }
                        if (!b.isPlayer) { b.x -= dx / d * push; b.y -= dy / d * push }
                    }
                }
            }
            a.x = a.x.coerceIn(20f, W - 20f)
            a.y = a.y.coerceIn(20f, D - 20f)
            solidPush(a, 10f)
        }
        for (v in vehicles) {
            if (v.dead) continue
            v.x = v.x.coerceIn(30f, W - 30f)
            v.y = v.y.coerceIn(30f, D - 30f)
            solidPush(v, 22f)
            coverPush(v)
        }
        updateBullets(dt)
        updateEffects(dt)
        soldiers.removeAll { it.dead && it.deadT <= 0f && !it.isPlayer }
        vehicles.removeAll { it.dead && it.deadT <= 0f && !it.isPlayer }
        planes.removeAll { it.dead }
        updateMission(dt)
        checkLose()
    }

    private fun checkLose() {
        if (result != 0 || waitCut || dancing) return
        val pl = player
        if (pl != null && pl.dead) {
            result = 2
            loseReason = if (pveh != null) 2 else 1
        }
    }

    private fun countHostile(): Int {
        var n = 0
        for (s in soldiers) if (!s.dead && sideOf(s.team) == 1) n++
        for (v in vehicles) if (!v.dead && sideOf(v.team) == 1) n++
        return n
    }

    private fun countFalan(): Int {
        var n = 0
        for (s in soldiers) if (!s.dead && s.team == 1) n++
        return n
    }

    // number of enemy bits that are still to come or alive (for the HUD)
    fun remaining(): Int = when (mission) {
        3 -> if (phase == 1) (totalToSpawn - spawnedTotal) + countHostile() else countHostile()
        5 -> if (phase == 1) (totalToSpawn - spawnedTotal) + countHostile() else countHostile()
        else -> countHostile()
    }

    fun controlRoomsLeft(): Int {
        var n = 0
        for (b in buildings) if (b.type == 1 && b.side == 1 && !b.dead) n++
        return n
    }

    fun distToGoal(): Float {
        val pl = player ?: return 0f
        return hypot(goalX - pl.x, goalY - pl.y)
    }

    fun expertsAlive(): Int {
        var n = 0
        for (s in soldiers) if (s.expert && !s.dead) n++
        return n
    }

    fun tankX() = tankX
    fun tankY() = tankY
    fun surveyX() = surveyX
    fun surveyY() = surveyY
    fun exitX() = exitX
    fun exitY() = exitY
    fun goalX() = goalX
    fun goalY() = goalY

    // ------------------------------------------------------------------ mission rules
    private fun updateMission(dt: Float) {
        if (result != 0) return
        val pl = player ?: return
        when (mission) {
            1 -> {
                if (phase == 0 && pl.y < 360f) {
                    phase = 1
                    objId = 2
                    for (i in 0 until 10) {
                        val x = if (i % 2 == 0) 40f else W - 40f
                        val s = addFalan(x, 330f + rf(-60f, 60f), 2, 0f, 0f, i % 3 == 2)
                        s.sight = 700f
                    }
                    showCutHint()
                }
                if (phase == 1) {
                    if (hypot(pl.x - tankX, pl.y - (tankY + 60f)) < 95f) {
                        progress += dt / 5f
                        if (progress >= 1f) { progress = 1f; endMission() }
                    }
                }
            }
            2 -> {
                // waves, technicals and aircraft keep coming
                waveT -= dt
                if (waveT <= 0f) {
                    waveT = 8f
                    val ref = leaderPosForSpawn()
                    val n = 4 + min(4, (t / 40f).toInt())
                    for (i in 0 until n) {
                        val x = if (i % 2 == 0) -20f else W + 20f
                        val s = addFalan(x, ref + rf(-260f, 80f), 2, 0f, 0f, i % 4 == 3)
                        s.sight = 800f
                    }
                }
                techT -= dt
                if (techT <= 0f) {
                    techT = 26f
                    val tv = addTechnical(if (Random.nextBoolean()) 120f else W - 120f, leaderPosForSpawn() - 520f, 1, 0, 2)
                    tv.spd = 80f
                }
                planeT -= dt
                if (planeT <= 0f) {
                    planeT = 15f
                    spawnPlane()
                    if (t > 60f && Random.nextBoolean()) spawnPlane()
                }
                // the experts survey the camp
                var near = 0
                for (s in soldiers) if (s.expert && !s.dead && hypot(s.x - surveyX, s.y - (surveyY + 30f)) < 90f) near++
                if (near > 0 && phase == 0) {
                    progress += dt / 32f
                    if (progress >= 1f) {
                        progress = 1f
                        phase = 1
                        endT = 3.5f
                        for (k in 0 until 7) shells.add(Shell(rf(220f, 680f), rf(120f, 260f), 0.4f + k * 0.45f, 70f, 5f, 0))
                    }
                }
                if (phase == 1) {
                    endT -= dt
                    if (endT <= 0f) endMission()
                }
                if (expertsAlive() == 0 && phase == 0) { result = 2; loseReason = 3 }
            }
            3 -> updateM3(dt, pl)
            4 -> updateM4(dt, pl)
            else -> updateM5(dt, pl)
        }
    }

    private fun showCutHint() {
        // short pause at the tank to warn the player (counter attack)
        sfx.play(Sfx.JET, 0.5f, 0.6f)
    }

    private fun leaderPosForSpawn(): Float {
        for (s in soldiers) if (s.expert && !s.dead) return s.y
        return pveh?.y ?: player?.y ?: 1000f
    }

    private fun endMission() {
        result = 1
    }

    private fun updateM3(dt: Float, pl: Soldier) {
        if (phase == 0) {
            planeT -= dt
            if (planeT <= 0f && controlRoomsLeft() > 0) { planeT = 24f; spawnPlane() }
            var cr: Building? = null
            for (b in buildings) if (b.type == 1) cr = b
            if (cr != null && cr.dead) {
                phaseT += dt
                if (phaseT > 2.2f) showCut(31)
            }
        } else if (phase == 1) {
            // trench battle: enemies rush to the exit of the rampart
            waveT -= dt
            if (waveT <= 0f && spawnedTotal < totalToSpawn) {
                waveT = 5.5f
                val n = min(5, totalToSpawn - spawnedTotal)
                for (i in 0 until n) {
                    val s = addFalan(rf(60f, W - 60f), rf(40f, 120f), 1, exitX + rf(-25f, 25f), exitY + 60f, spawnedTotal % 5 == 4)
                    s.sight = 500f
                    s.cdMul = 3f
                    spawnedTotal++
                }
            }
            for (s in soldiers) {
                if (s.dead || s.team != 1) continue
                if (s.y >= exitY - 6f && abs(s.x - exitX) < 58f) {
                    s.dead = true
                    s.deadT = 0f
                    hearts--
                    hurtV = 0.8f
                    shake = 0.5f
                    sfx.play(Sfx.HURT, 1f, 0.5f)
                    explode(s.x, s.y + 20f, 40f, 0f, 1, false)
                }
            }
            if (hearts <= 0) { result = 2; loseReason = 4 }
            if (result == 0 && spawnedTotal >= totalToSpawn && countFalan() == 0) {
                phaseT += dt
                if (phaseT > 1.5f) showCut(32)
            }
        } else {
            if (countFalan() == 0) {
                phaseT += dt
                if (phaseT > 1.5f) endMission()
            }
        }
    }

    private fun updateM4(dt: Float, pl: Soldier) {
        if (phase == 0) {
            if (pl.y < 1050f && !betrayed) {
                showCut(42)
            }
            if (betrayed) {
                phase = 1
            }
        } else if (phase == 1) {
            if (countFalan() == 0 && countHostile() == 0) {
                phase = 2
                objId = 9
                goalX = 450f
                goalY = 1850f
                // celebration committee waits at the Elite base
                for (i in 0 until 3) addElite(380f + i * 70f, 1900f, 5, i % 2 == 0)
            }
        } else if (phase == 2) {
            if (hypot(pl.x - goalX, pl.y - goalY) < 130f) {
                phase = 3
                showCut(43)
            }
        }
    }

    private fun updateM5(dt: Float, pl: Soldier) {
        if (phase == 0) {
            planeT -= dt
            if (planeT <= 0f && controlRoomsLeft() > 0) { planeT = 22f; spawnPlane() }
            if (controlRoomsLeft() == 0) {
                phaseT += dt
                if (phaseT > 2.4f) showCut(52)
            }
        } else if (phase == 1) {
            waveT -= dt
            if (waveT <= 0f && spawnedTotal < totalToSpawn) {
                waveT = 4f
                val n = min(5, totalToSpawn - spawnedTotal)
                for (i in 0 until n) {
                    val s = addDinari(rf(60f, W - 60f), rf(40f, 140f), 2)
                    s.sight = 700f
                    spawnedTotal++
                }
                if (spawnedTotal == 10 || spawnedTotal == 20) {
                    val tv = addTechnical(rf(150f, W - 150f), 60f, 3, 3, 2)
                    tv.spd = 70f
                }
            }
            if (spawnedTotal >= totalToSpawn && countHostile() == 0) {
                phaseT += dt
                if (phaseT > 1.5f) showCut(53)
            }
        }
    }

    /** Called by the view when the player taps "continue" after the dance. */
    fun finishDance() {
        dancing = false
        sfx.stopMusic()
        result = 1
    }
}
