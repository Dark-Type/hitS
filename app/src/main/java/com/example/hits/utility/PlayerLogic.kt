package com.example.hits.utility

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlayerLogic {
    private var health = 100
    private var isAlive = true
    private var isPlanted = false
    private var killsCount = 0
    private var deathsCount = 0
    private var assistsCount = 0

    private val damage = 50
    private val reviveHealth = 30
    fun takeDamage(roomID: Int, playerID: Int, newValue : Int) {

        health -= damage

        if (health <= 0) {
            isAlive = false
            addDeaths(roomID, playerID)
        }
    }

    fun doDamage(roomID: Int, playerID : Int) {

        val currPlayerRef = databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(playerID.toString())

        addValue(currPlayerRef.child("health"), -damage)

        GlobalScope.launch(Dispatchers.IO) {
            val value = getValueFromDatabase(roomID, playerID)
            if (value <= 0) {
                addKills(roomID, playerID)
            }
        }
    }

    private fun addKills(roomID: Int, playerID : Int) {
        addValue(databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(playerID.toString()).child("kills"), 1)
        killsCount++
    }

    private fun addDeaths(roomID: Int, playerID : Int) {
        addValue(databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(playerID.toString()).child("deaths"), 1)
        deathsCount++
    }

    private fun addAssists(roomID: Int, playerID : Int) {
        addValue(databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(playerID.toString()).child("assists"), 1)
        assistsCount++
    }

    fun isAlive(): Boolean {
        return isAlive
    }

    fun isDamaged(): Boolean {
        return health < 100
    }

    fun revive(roomID: Int, IDtoRevive: Int) {
        addValue(databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(IDtoRevive.toString()).child("health"), reviveHealth)
        databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users").child(IDtoRevive.toString()).child("isAlive").setValue(true)
    }

    fun getRevived() {
        health = reviveHealth
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

    fun listenForChanges(roomID: Int, userID: Int) {

        fun listenForHealthChanges(roomID: Int, userID: Int) {
            val valueRef = databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users")
                .child(userID.toString()).child("health")

            valueRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val value = dataSnapshot.getValue(Int::class.java)

                    if (value != null) takeDamage(roomID, userID, value)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("Firebase", "listenForValueChanges:onCancelled", databaseError.toException())
                }
            })
        }

        fun listenForAliveChanges(roomID: Int, userID: Int) {
            val valueRef = databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users")
                .child(userID.toString()).child("isAlive")

            valueRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val value = dataSnapshot.getValue(Boolean::class.java)

                    if (value == true) getRevived()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("Firebase", "listenForValueChanges:onCancelled", databaseError.toException())
                }
            })
        }
    }

    private suspend fun getValueFromDatabase(roomID: Int, userID: Int) : Int {

        return suspendCancellableCoroutine { continuation ->
            val valueRef = databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users")
                .child(userID.toString()).child("health")

            valueRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val value = dataSnapshot.getValue(Int::class.java)
                    if (value != null) {
                        continuation.resume(value)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    continuation.resumeWithException(databaseError.toException())
                }
            })
        }
    }
}