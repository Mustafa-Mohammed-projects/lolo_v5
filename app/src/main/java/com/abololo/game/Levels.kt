package com.abololo.game

/** Language switch (Arabic / English) used by every label in the game. */
object L {
    @JvmField var ar: Boolean = true
    fun t(arabic: String, english: String): String = if (ar) arabic else english
}
