package com.example.hits.utility

import kotlin.math.min

val winK = 1.2f
val loseK = 0.8f

fun calculatePointsGainForTDM(kills: Int, deaths: Int, assists: Int, isWon: Boolean) : Int {

    val winKoeff = if (isWon) winK else loseK
    val KDR = calculateKDRCoeff(kills, deaths)

    return ((kills * KDR + assists) * winKoeff).toInt()
}

fun calculatePointsGainForFFA(kills: Int, deaths: Int, assists: Int) : Int {

    val KDR = calculateKDRCoeff(kills, deaths)
    return (kills * KDR + assists).toInt()
}

fun calculateKDRCoeff(kills: Int, deaths: Int) : Float {
    return if (deaths == 0) 10.0f else min(10.0f, kills.toFloat() / deaths)
}