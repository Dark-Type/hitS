package com.example.hits.utility

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

class PlayerLogic(private val healthThreshold: Int) {
    private var isAlive = true
    private var isPlanted = false
    private var health = healthThreshold
    private var killsCount = 0
    private var deathsCount = 0
    private var assistsCount = 0

    private val damage = 50
    private val reviveHealth = 30
    fun changeHP(roomID: Int, playerID: Int, newHealth: Int) {

        health = newHealth


        if (health <= 0 && isAlive) {

            isAlive = false

            addDeaths(roomID, playerID)

            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(playerID.toString()).child("isAlive").setValue(false)
        }
    }

    fun doDamage(roomID: Int, playerID : Int) {

        val currPlayerRef = databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users").child(playerID.toString())

        addValue(currPlayerRef.child("health"), -damage)

        GlobalScope.launch(Dispatchers.IO) {

            val value = getHealthValueFromDatabase(roomID, playerID)

            if (value <= 0) {
                addKills(roomID, playerID)
                currPlayerRef.child("health").setValue(0)
            }
        }
    }

    /*
    fun doDamage(roomID: Int, playerID: Int) {
        val currPlayerRef =
            databaseRef.child("rooms").child(roomID.toString()).child("gameData").child("users")
                .child(playerID.toString())

        GlobalScope.launch(Dispatchers.IO) {
            val currentHealth = getValueFromDatabase(roomID, playerID)
            val newHealth = currentHealth - damage
            addValue(currPlayerRef.child("health"), newHealth)

            if (newHealth <= 0) {
                addKills(roomID, playerID)
            }
        }
    }
     */


    fun getHealth(): Int {
        return health
    }

    private fun addKills(roomID: Int, playerID: Int) {
        addValue(
            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(playerID.toString()).child("kills"), 1
        )
        killsCount++
    }

    private fun addDeaths(roomID: Int, playerID: Int) {
        addValue(
            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(playerID.toString()).child("deaths"), 1
        )
        deathsCount++
    }

    private fun addAssists(roomID: Int, playerID: Int) {
        addValue(
            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(playerID.toString()).child("assists"), 1
        )
        assistsCount++
    }

    fun isAlive(): Boolean {
        return isAlive
    }

    fun isDamaged(): Boolean {
        return health < healthThreshold
    }

    fun revive(roomID: Int, IDtoRevive: Int) {

        addValue(
            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(IDtoRevive.toString()).child("health"), reviveHealth
        )

        databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
            .child(IDtoRevive.toString()).child("isAlive").setValue(true)
    }

    fun getRevived() {
        isAlive = true
        health = reviveHealth
    }

    fun plant() {
        //need to add logic of planting the bomb
    }

    fun defuse() {
        //need to add logic of defusing the bomb
    }

    fun heal(roomID: Int, userID: Int) {

        GlobalScope.launch(Dispatchers.IO) {

            val value = getHealthValueFromDatabase(roomID, userID)
            val healthToAdd = if (value + 30 >= healthThreshold) max(healthThreshold - value, 0) else 30

            val healthRef = databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(userID.toString()).child("health")

            Log.d("HP", "$healthToAdd $health $healthThreshold")

            addValue(healthRef, healthToAdd)
        }
    }

    fun listenForAliveChanges(roomID: Int, userID: Int, onAliveChanged: (isAlive: Boolean) -> Unit) {
        val valueRef =
            databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                .child(userID.toString()).child("isAlive")

        valueRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue(Boolean::class.java)
                if (value != null) {
                    onAliveChanged(value)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("Firebase", "listenForValueChanges:onCancelled", databaseError.toException())
            }
        })
    }



    private suspend fun getHealthValueFromDatabase(roomID: Int, userID: Int): Int {

        return suspendCancellableCoroutine { continuation ->
            val valueRef =
                databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
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

    private suspend fun getAliveValueFromDatabase(roomID: Int, userID: Int): Boolean {

        return suspendCancellableCoroutine { continuation ->
            val valueRef =
                databaseRef.child("rooms").child(roomID.toString()).child("gameInfo").child("users")
                    .child(userID.toString()).child("isAlive")

            valueRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val value = dataSnapshot.getValue(Boolean::class.java)
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