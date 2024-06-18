package com.example.hits.utility

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.hits.GAMEMODE_BATTLE_ROYALE
import com.example.hits.GAMEMODE_CS_GO
import com.example.hits.GAMEMODE_FFA
import com.example.hits.GAMEMODE_ONE_HIT_ELIMINATION
import com.example.hits.GAMEMODE_TDM
import com.example.hits.getGamemodes
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CompletableFuture
import kotlin.math.max

val databaseRef: DatabaseReference = FirebaseDatabase.getInstance("https://shooter-24512-default-rtdb.europe-west1.firebasedatabase.app").getReference()
data class User(val id: Int = 0, val name: String = "", val points: Int = 0, val damage: Int = 0, val kills: Int = 0, val deaths: Int = 0, val assists: Int = 0)
data class NewsObject(val text: String = "NO TEXT")
data class UserForLeaderboard(
    val name: String,
    val points: Int,
    val rank: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int
)

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

    for (gamemode in getGamemodes()) {
        currRoomRef.child("gamemodeVotes").child(gamemode).setValue(0)
    }

    Log.d("Firebase", "Room $roomID created")
}

fun addUserToRoom(roomID: Int, firstUser: User) {

    val newUserRef = databaseRef.child("rooms").child(roomID.toString()).child("users").child(firstUser.id.toString())

    val startTime = System.currentTimeMillis()
    newUserRef.setValue(firstUser).addOnSuccessListener {
        val endTime = System.currentTimeMillis()
        Log.d("TIME", (endTime - startTime).toString())
    }

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

fun getNews() : SnapshotStateList<String> {

    val news = SnapshotStateList<String>()

    databaseRef.child("news").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            for (newsSnapshot in dataSnapshot.children) {
                news.add(newsSnapshot.getValue(NewsObject::class.java)!!.text)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    return news
}

fun updateNicknameInDatabase(id: Int, newName: String) {

    databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            for (userSnapshot in dataSnapshot.children) {

                val currUser = userSnapshot.getValue(User::class.java)

                if (currUser!!.id == id) {
                    databaseRef.child("users").child(id.toString()).child("name").setValue(newName)
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    Log.d("Nickname change", newName)
}

fun getUsersForLeaderboard() : SnapshotStateList<UserForLeaderboard> {

    val users = SnapshotStateList<User>()
    val leaderboardUsers = SnapshotStateList<UserForLeaderboard>()

    databaseRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            users.clear()

            for (userSnapshot in dataSnapshot.children) {
                users.add(userSnapshot.getValue(User::class.java)!!)
            }

            val sortedUsers = users.sortedWith(compareBy({ -it.points }, { it.id }))

            for (i in sortedUsers.indices) {
                leaderboardUsers.add(UserForLeaderboard(
                    sortedUsers[i].name,
                    sortedUsers[i].points,
                    i,
                    sortedUsers[i].kills,
                    sortedUsers[i].deaths,
                    sortedUsers[i].assists
                ))
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    return leaderboardUsers
}

fun addValue(valueRef: DatabaseReference, value: Int) {

    valueRef.runTransaction(object : Transaction.Handler {

        override fun doTransaction(mutableData: MutableData): Transaction.Result {

            var currentValue = mutableData.getValue(Int::class.java)

            if (currentValue == null) {
                currentValue = 0
            }

            mutableData.value = currentValue + value

            return Transaction.success(mutableData)
        }

        override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
            // Transaction completed
            Log.d("Firebase", "Transaction:onComplete:$databaseError")
        }
    })
}