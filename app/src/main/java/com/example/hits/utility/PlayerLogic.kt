package com.example.hits.utility

class PlayerLogic {
    private var health = 100
    private var isAlive = true
    private var isPlanted = false
    private var killsCount = 0
    private var deathsCount = 0
    private var assistsCount = 0
    fun takeDamage() {
        //need to add logic of retrieving this damage from the db
        val damage = 50
        health -= damage
        if (health <= 0) {
            isAlive = false
            addDeaths()
        }
    }

    fun doDamage(playerId:Int) {
        //need to add logic of sending this damage to the db
        val gotKill = false
        if (gotKill) addKills()
        return
    }

    private fun addKills() {
        //add db logic
        killsCount++
    }

    private fun addDeaths() {
        //add db logic
        deathsCount++
    }

    private fun addAssists() {
        //add db logic

        assistsCount++
    }

    fun isAlive(): Boolean {
        return isAlive
    }

    fun isDamaged(): Boolean {
        return health < 100
    }

    fun revive(id: Int) {
        //need to add logic of reviving the player
    }

    fun getRevived() {
        //need to add logic of retrieving this revive from the db
        health = 30
    }

    fun plant() {
        //need to add logic of planting the bomb
    }

    fun defuse() {
        //need to add logic of defusing the bomb
    }

    fun heal() {
        health = if (health + 30 >= 100) 100 else health + 30
    }
}