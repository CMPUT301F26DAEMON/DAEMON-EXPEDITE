package com.example.daemon_expedite.backend

import com.google.firebase.firestore.DocumentSnapshot

data class SearchCursor(
    val boundIndex: Int,
    val lastDoc: DocumentSnapshot?,
)

data class CampsitePage(
    val campsites: List<Campsite>,
    val nextCursor: SearchCursor?, // null = no more results
)
