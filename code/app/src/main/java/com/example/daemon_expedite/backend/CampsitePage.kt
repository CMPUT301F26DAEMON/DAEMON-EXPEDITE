// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement our lottery model and rules", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.backend

import com.google.firebase.firestore.DocumentSnapshot

/**
 * One page of results from [DatabaseInterface.searchCampsites].
 *
 * @property campsites the campsites on this page.
 * @property cursor pass this to the next searchCampsites call to get the following page;
 * null when there are no more results.
 */
data class CampsitePage(
    val campsites: List<Campsite>,
    val cursor: SearchCursor?,
)

/**
 * Where a campsite search left off. Treat it as opaque: only pass it back to searchCampsites.
 *
 * @property boundIndex which geohash query (or the single plain query) the search was on.
 * @property lastDoc the last document returned, so the next page starts after it.
 */
data class SearchCursor(
    val lastDoc: DocumentSnapshot,
)
