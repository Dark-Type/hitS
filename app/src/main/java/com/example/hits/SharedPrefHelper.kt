package com.example.hits

import android.content.Context
import android.content.SharedPreferences

class SharedPrefHelper(context: Context) {
    private val preferencesName = "com.example.hits"
    private val nicknameKey = "nickname"
    private val killsKey = "kills"
    private val deathsKey = "deaths"
    private val assistsKey = "assists"
    private val damageKey = "damage"
    private val idKey = "id"
    private val pointsKey = "points"
    private val prefs: SharedPreferences =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun createID(id: Int) {
        val editor = prefs.edit()
        editor.putString(idKey, id.toString())
        editor.apply()
    }

    fun getID(): String? {
        return prefs.getString(idKey, null)
    }

    fun setFirstTimeJoiningLobby(isFirstTime: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean("isFirstTimeJoiningLobby", isFirstTime)
        editor.apply()
    }

    fun isFirstTimeJoiningLobby(): Boolean {
        return prefs.getBoolean("isFirstTimeJoiningLobby", true)
    }


    fun saveNickname(nickname: String) {
        val editor = prefs.edit()
        editor.putString(nicknameKey, nickname)
        editor.apply()
    }

    fun getNickname(): String? {
        return prefs.getString(nicknameKey, null)
    }

    fun saveKills(k: String) {
        val editor = prefs.edit()
        editor.putString(killsKey, k)
        editor.apply()
    }

    fun getKills(): String? {
        return prefs.getString(killsKey, null)
    }

    fun saveDeaths(d: String) {
        val editor = prefs.edit()
        editor.putString(deathsKey, d)
        editor.apply()
    }

    fun getDeaths(): String? {
        return prefs.getString(deathsKey, null)
    }

    fun saveAssists(a: String) {
        val editor = prefs.edit()
        editor.putString(assistsKey, a)
        editor.apply()
    }

    fun getAssists(): String? {
        return prefs.getString(assistsKey, null)
    }

    fun saveDamage(damage: String) {
        val editor = prefs.edit()
        editor.putString(damageKey, damage)
        editor.apply()
    }

    fun getDamage(): String? {
        return prefs.getString(damageKey, null)
    }

    fun savePoints(points: String) {
        val editor = prefs.edit()
        editor.putString(pointsKey, points)
        editor.apply()
    }

    fun getPoints(): String? {
        return prefs.getString(pointsKey, null)
    }
}