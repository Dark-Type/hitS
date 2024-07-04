package com.example.hits

const val GAMEMODE_FFA = "Free For All"
const val GAMEMODE_TDM = "Team Deathmatch"
const val GAMEMODE_ONE_HIT_ELIMINATION = "One Hit Elimination"
const val GAMEMODE_CS_GO = "CS:GO"
const val GAMEMODE_HNS = "Hide and Seek"
const val GAMEMODE_ONE_VS_ALL = "One vs All"

val gameModesDescription = hashMapOf(
    GAMEMODE_FFA to "Fight against each other in a free-for-all mode!",
    GAMEMODE_TDM to "Choose a team and play with your friends!",
    GAMEMODE_ONE_HIT_ELIMINATION to "One hit and you are out!",
    GAMEMODE_CS_GO to "Write Plant somewhere and it will become the plant for the bomb!\n The rest is up to you!",
    GAMEMODE_HNS to "One team hides and one team seeks!\nNo shooting, just like Lydia asked",
    GAMEMODE_ONE_VS_ALL to "Wanna feel like a main character in some action movie?\n Try this mode! You are the only one against everyone else!"
)

fun getGamemodeDescription(gamemode: String): String {
    return gameModesDescription[gamemode] ?: "No description"
}


fun getGamemodes(): List<String> {

    return listOf(
        GAMEMODE_FFA,
        GAMEMODE_TDM,
        GAMEMODE_ONE_HIT_ELIMINATION,
        GAMEMODE_CS_GO,
        GAMEMODE_HNS,
        GAMEMODE_ONE_VS_ALL
    )
}