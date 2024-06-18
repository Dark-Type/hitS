package com.example.hits

const val GAMEMODE_ONE_HIT_ELIMINATION = "One Hit Elimination"
const val GAMEMODE_CS_GO = "CS:GO"
const val GAMEMODE_BATTLE_ROYALE = "Battle Royale"
const val GAMEMODE_FFA = "Free For All"
const val GAMEMODE_TDM = "Team Deathmatch"

enum class GameMods {
    ONE_HIT_ELIMINATION_1,
    ONE_HIT_ELIMINATION_2,
    ONE_HIT_ELIMINATION_3,
    ONE_HIT_ELIMINATION_4,
    CS_GO,
    BATTLE_ROYALE_1,
    BATTLE_ROYALE_2,
    BATTLE_ROYALE_3,
    BATTLE_ROYALE_4,
    ONE_VS_ALL,
    TEAM_DEATHMATCH,
}

fun getGamemodes() : List<String> {

    return listOf(
        GAMEMODE_ONE_HIT_ELIMINATION,
        GAMEMODE_CS_GO,
        GAMEMODE_BATTLE_ROYALE,
        GAMEMODE_FFA,
        GAMEMODE_TDM
    )
}