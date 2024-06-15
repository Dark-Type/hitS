package com.example.hits.utility

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.util.concurrent.CompletableFuture
import kotlin.math.max

val databaseRef: DatabaseReference = FirebaseDatabase.getInstance("https://shooter-24512-default-rtdb.europe-west1.firebasedatabase.app").getReference()
data class User(val id: Int = 0, val name: String = "")

fun getRandomID(): Int {

    var currRooms = mutableListOf<Int>()

    databaseRef.child("rooms").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            val newCurrRooms = mutableListOf<Int>()

            for (roomSnapshot in dataSnapshot.children) {

                // roomSnapshot.key is the name of the room
                // roomSnapshot.value is the data of the room

                roomSnapshot.key?.toInt()?.let { newCurrRooms.add(it) }
            }

            currRooms = newCurrRooms
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    val range = (0..999).toMutableList()

    range.removeAll(currRooms)

    return range.random()
}

fun createRoom(roomID: Int) {

    val currRoomRef = databaseRef.child("rooms").child(roomID.toString())

    currRoomRef.child("users").setValue("")
    currRoomRef.child("gameInfo").setValue("")

    Log.d("Firebase", "Room $roomID created")
}

fun addUserToRoom(roomID: Int, firstUser: User) {

    val newUserRef = databaseRef.child("rooms").child(roomID.toString()).child("users").child(firstUser.id.toString())
    newUserRef.setValue(firstUser)

    newUserRef.onDisconnect().removeValue().addOnSuccessListener {
        Log.d("CALLED", "a")
        removeRoomIfEmpty(roomID)
    }

    Log.d("Firebase", "User ${firstUser.name} with id ${firstUser.id} added to room $roomID")
}

fun getLobbyUsers(lobbyID: Int) : SnapshotStateList<User> {

    val users = SnapshotStateList<User>()

    databaseRef.child("rooms").child(lobbyID.toString()).child("users").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            users.clear()

            for (userSnapshot in dataSnapshot.children) {
                users.add(userSnapshot.getValue(User::class.java)!!)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    return users
}

fun getNewID() : CompletableFuture<Int> {

    val future = CompletableFuture<Int>()
    var newID = 0

    databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            for (userSnapshot in dataSnapshot.children) {

                val currUser = userSnapshot.getValue(User::class.java)
                newID = max(newID, currUser!!.id)
            }

            newID++
            future.complete(newID)
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
            future.completeExceptionally(databaseError.toException())
        }
    })

    Log.d("NEW", newID.toString())

    return future
}

fun createUser(id: Int, nickname: String) {

    databaseRef.child("users").child(id.toString()).setValue(User(id, nickname))

    Log.d("Firebase", "User $nickname with id $id added to main user database")
}

fun removeUserFromRoom(lobbyId: Int, user: User) {

    databaseRef.child("rooms").child(lobbyId.toString()).child("users").child(user.id.toString()).removeValue()
        .addOnSuccessListener {
            removeRoomIfEmpty(lobbyId)
        }
}

fun removeRoomIfEmpty(lobbyId: Int) {

    val users = mutableListOf<User>()

    databaseRef.child("rooms").child(lobbyId.toString()).child("users").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            users.clear()

            for (userSnapshot in dataSnapshot.children) {
                users.add(userSnapshot.getValue(User::class.java)!!)
            }

            if (users.isEmpty()) {
                databaseRef.child("rooms").child(lobbyId.toString()).removeValue()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })
}