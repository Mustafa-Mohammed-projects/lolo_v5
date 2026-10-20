package com.abololo.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/** Sound effects (SoundPool) + the ending song (res/raw/endgamelolo) + the victory song (res/raw/victory_song). */
class Sfx(private val ctx: Context) {
    companion object {
        const val SWORD = 0
        const val SHOT = 1
        const val SCREAM = 2
        const val HURT = 3
        const val CLICK = 4
        const val STOMP = 5
        const val WIN = 6
        const val LOSE = 7
        const val BOOM = 8
        const val CANNON = 9
        const val JET = 10

        const val MUSIC_END = 0
        const val MUSIC_VICTORY = 1
    }

    var enabled = true
        set(value) {
            field = value
            if (!value) {
                try {
                    music?.pause()
                } catch (e: Exception) {
                }
            } else if (musicWanted) {
                startMusic(musicKind)
            }
        }

    private val pool: SoundPool
    private val ids = IntArray(11)
    private var music: MediaPlayer? = null
    private var musicKind = MUSIC_END
    private var musicWanted = false
    private var paused = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
        ids[SWORD] = pool.load(ctx, R.raw.sfx_sword, 1)
        ids[SHOT] = pool.load(ctx, R.raw.sfx_shot, 1)
        ids[SCREAM] = pool.load(ctx, R.raw.sfx_scream, 1)
        ids[HURT] = pool.load(ctx, R.raw.sfx_hurt, 1)
        ids[CLICK] = pool.load(ctx, R.raw.sfx_click, 1)
        ids[STOMP] = pool.load(ctx, R.raw.sfx_stomp, 1)
        ids[WIN] = pool.load(ctx, R.raw.sfx_win, 1)
        ids[LOSE] = pool.load(ctx, R.raw.sfx_lose, 1)
        ids[BOOM] = pool.load(ctx, R.raw.sfx_boom, 1)
        ids[CANNON] = pool.load(ctx, R.raw.sfx_cannon, 1)
        ids[JET] = pool.load(ctx, R.raw.sfx_jet, 1)
    }

    fun play(id: Int, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled || paused) return
        try {
            pool.play(ids[id], volume, volume, 1, 0, rate)
        } catch (e: Exception) {
        }
    }

    private fun releaseMusic() {
        try {
            music?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
        }
        music = null
    }

    fun startMusic(kind: Int = MUSIC_END) {
        if (kind != musicKind) releaseMusic()
        musicKind = kind
        musicWanted = true
        if (!enabled || paused) return
        try {
            var m = music
            if (m == null) {
                val res = if (kind == MUSIC_VICTORY) R.raw.victory_song else R.raw.endgamelolo
                m = MediaPlayer.create(ctx, res)
                if (m == null) return
                m.isLooping = true
                m.setVolume(0.9f, 0.9f)
                music = m
            }
            if (!m.isPlaying) m.start()
        } catch (e: Exception) {
        }
    }

    fun stopMusic() {
        musicWanted = false
        releaseMusic()
    }

    fun onPause() {
        paused = true
        try {
            music?.let { if (it.isPlaying) it.pause() }
        } catch (e: Exception) {
        }
    }

    fun onResume() {
        paused = false
        if (musicWanted) startMusic(musicKind)
    }

    fun release() {
        stopMusic()
        try {
            pool.release()
        } catch (e: Exception) {
        }
    }
}
