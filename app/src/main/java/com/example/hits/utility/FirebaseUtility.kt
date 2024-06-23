package com.example.hits.utility

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.hits.GAMEMODE_FFA
import com.example.hits.GAMEMODE_TDM
import com.example.hits.getGamemodes
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.util.concurrent.CompletableFuture
import kotlin.math.max

val databaseRef: DatabaseReference =
    FirebaseDatabase.getInstance("https://shooter-24512-default-rtdb.europe-west1.firebasedatabase.app")
        .getReference()

data class User(
    val id: Int = 0,
    val name: String = "",
    val points: Int = 0,
    val damage: Int = 0,
    val kills: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0
)

data class NewsObject(val text: String = "NO TEXT")
data class ScoreData(val id: Int, val name: String, val score: Int)
data class UserForLeaderboard(
    val name: String,
    val points: Int,
    val rank: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int
)

data class UserForTDM(
    val id: Int = 0,
    val name: String = "",
    val points: Int = 0,
    val damage: Int = 0,
    val kills: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0,
    val team: Int
)

const val TEAM_UNKNOWN = -1
const val TEAM_RED = 0
const val TEAM_BLUE = 1

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

    currRoomRef.child("isPlaying").setValue(false)
    currRoomRef.child("playersReady").setValue(0)

    Log.d("Firebase", "Room $roomID created")
}

fun addUserToRoom(roomID: Int, firstUser: User) {

    val newUserRef = databaseRef.child("rooms").child(roomID.toString()).child("users")
        .child(firstUser.id.toString())

    newUserRef.setValue(firstUser)
    newUserRef.child("team").setValue(TEAM_UNKNOWN)

    newUserRef.onDisconnect().removeValue().addOnSuccessListener {
        removeRoomIfEmpty(roomID)
    }

    Log.d("Firebase", "User ${firstUser.name} with id ${firstUser.id} added to room $roomID")
}

fun getLobbyUsers(lobbyID: Int): SnapshotStateList<User> {

    val users = SnapshotStateList<User>()

    databaseRef.child("rooms").child(lobbyID.toString()).child("users")
        .addListenerForSingleValueEvent(object : ValueEventListener {

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

fun getNewID(): CompletableFuture<Int> {

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

fun removeUserFromRoom(lobbyId: Int, userID: Int) {
    databaseRef.child("rooms").child(lobbyId.toString()).child("users").child(userID.toString())
        .removeValue()
        .addOnSuccessListener {
            removeRoomIfEmpty(lobbyId)
        }
}

fun removeRoomIfEmpty(lobbyId: Int) {

    val users = mutableListOf<User>()

    databaseRef.child("rooms").child(lobbyId.toString()).child("users")
        .addListenerForSingleValueEvent(object : ValueEventListener {

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

fun getNews(): SnapshotStateList<String> {

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

fun getUsersForLeaderboard(): SnapshotStateList<UserForLeaderboard> {

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
                leaderboardUsers.add(
                    UserForLeaderboard(
                        sortedUsers[i].name,
                        sortedUsers[i].points,
                        i,
                        sortedUsers[i].kills,
                        sortedUsers[i].deaths,
                        sortedUsers[i].assists
                    )
                )
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

            Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
        }
    })

    return leaderboardUsers
}

fun getUsersForResultsScreen(
    roomID: Int,
    gamemode: String
): SnapshotStateList<ScoreData> {

    val users = SnapshotStateList<User>()
    val scores = SnapshotStateList<ScoreData>()

    databaseRef.child("rooms").child(roomID.toString()).child("gameInfo")
        .child("users").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                users.clear()

                for (userSnapshot in dataSnapshot.children) {
                    users.add(userSnapshot.getValue(User::class.java)!!)
                }

                val sortedUsers = users.sortedWith(compareBy({ -it.kills }, { it.deaths }))

                for (i in sortedUsers.indices) {

                    when (gamemode) {

                        GAMEMODE_FFA -> scores.add(

                            ScoreData(

                                sortedUsers[i].id,
                                sortedUsers[i].name,
                                calculatePointsGainForFFA(
                                    sortedUsers[i].kills,
                                    sortedUsers[i].deaths,
                                    sortedUsers[i].assists
                                )
                            )
                        )

                        GAMEMODE_TDM -> scores.add(

                            ScoreData(

                                sortedUsers[i].id,
                                sortedUsers[i].name,
                                calculatePointsGainForTDM(
                                    sortedUsers[i].kills,
                                    sortedUsers[i].deaths,
                                    sortedUsers[i].assists,
                                    true //to changge
                                )
                            )
                        )
                    }
                }

                println(scores.size)
                println(gamemode)
            }

            override fun onCancelled(databaseError: DatabaseError) {

                Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
            }
        })

    return scores
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

        override fun onComplete(
            databaseError: DatabaseError?,
            committed: Boolean,
            dataSnapshot: DataSnapshot?
        ) {
            // Transaction completed
            Log.d("Firebase", "Transaction:onComplete:$databaseError")
        }
    })
}

fun runGame(roomID: Int, users: List<User>, teamRed: List<String>, teamBlue: List<String>) {

    val currRoomRef = databaseRef.child("rooms").child(roomID.toString())

    val usersWithTeam = mutableListOf<UserForTDM>()

    for (user in users) {

        if (teamRed.contains(user.name))
            usersWithTeam.add(
                UserForTDM(
                    user.id,
                    user.name,
                    user.points,
                    user.damage,
                    user.kills,
                    user.deaths,
                    user.assists,
                    TEAM_RED
                )
            )

        if (teamBlue.contains(user.name))
            usersWithTeam.add(
                UserForTDM(
                    user.id,
                    user.name,
                    user.points,
                    user.damage,
                    user.kills,
                    user.deaths,
                    user.assists,
                    TEAM_BLUE
                )
            )
    }

    var maxModeVotes = 0
    var currMode = GAMEMODE_FFA

    databaseRef.child("rooms").child(roomID.toString()).child("gamemodeVotes")
        .addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (valueSnapshot in dataSnapshot.children) {

                    val value = valueSnapshot.getValue(Int::class.java)!!

                    if (value > maxModeVotes) {
                        currMode = valueSnapshot.key!!
                        maxModeVotes = value
                    }
                }

                setCurrGameUserStats()
            }

            override fun onCancelled(databaseError: DatabaseError) {

                Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
            }

            fun setCurrGameUserStats() {

                currRoomRef.child("gameInfo").child("currGamemode").setValue(currMode)

                for (userWithTeam in usersWithTeam) {

                    currRoomRef.child("gameInfo").child("users").child(userWithTeam.id.toString())
                        .setValue(
                            UserForTDM(
                                userWithTeam.id,
                                userWithTeam.name,
                                0,
                                0,
                                0,
                                0,
                                0,
                                userWithTeam.team
                            )
                        )

                    currRoomRef.child("gameInfo").child("users").child(userWithTeam.id.toString())
                        .child("isAlive").setValue(true)
                    currRoomRef.child("gameInfo").child("users").child(userWithTeam.id.toString())
                        .child("health").setValue(100)
                }

                currRoomRef.child("isPlaying").setValue(true)
                currRoomRef.child("playersReady").setValue(0)

                for (gamemode in getGamemodes()) {
                    currRoomRef.child("gamemodeVotes").child(gamemode).setValue(0)
                }
            }
        })
}

fun setUserTeam(roomID: Int, userID: Int, team: Int) {

    databaseRef.child("rooms").child(roomID.toString()).child("users").child(userID.toString())
        .child("team").setValue(team)
}

fun endGame(roomID: Int) {

    val currRoomRef = databaseRef.child("rooms").child(roomID.toString())

    currRoomRef.child("isPlaying").setValue(false)
    currRoomRef.child("playersReady").setValue(0)

    currRoomRef.child("gameInfo").child("users")
        .addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (userSnapshot in dataSnapshot.children) {

                    val user = userSnapshot.getValue(User::class.java)!!

                    currRoomRef.child("users").child(user.id.toString()).child("team")
                        .setValue(TEAM_UNKNOWN)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

                Log.w("Firebase", "loadPost:onCancelled", databaseError.toException())
            }
        })
}

fun getUsersForCurrGameLeaderboard(roomID: Int) : SnapshotStateList<UserForLeaderboard> {

    val usersForCurrGameLeaderboard = SnapshotStateList<UserForLeaderboard>()
    val users = mutableListOf<User>()

    databaseRef.child("rooms").child(roomID.toString()).child("users")
        .addListenerForSingleValueEvent(object : ValueEventListener {

        override fun onDataChange(dataSnapshot: DataSnapshot) {

            for (userSnapshot in dataSnapshot.children) {

                users.add(userSnapshot.getValue(User::class.java)!!)
            }

            var i = 0

            for (user in users) {

                usersForCurrGameLeaderboard.add(
                    UserForLeaderboard(
                        user.name,
                        0,
                        i + 1,
                        0,
                        0,
                        0
                    )
                )

                i++
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    })

    return usersForCurrGameLeaderboard
}