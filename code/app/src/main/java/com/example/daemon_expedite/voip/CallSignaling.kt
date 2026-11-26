package com.example.daemon_expedite.voip

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Firestore side of a voice or video call: how the two phones swap the details WebRTC needs before audio
 * can flow ("signaling"). Nothing here touches audio; see [CallManager] for that.
 *
 * One document per call, `calls/<callId>`:
 * ```
 * callerId, callerName, calleeId, calleeName   who is calling whom (user ids, display names)
 * video       true for a video call, false for voice only
 * status      "ringing" | "accepted" | "declined" | "ended" | "missed"
 * offer       { type, sdp }   written by the caller when the call starts
 * answer      { type, sdp }   written by the callee when they accept
 * createdAt   server timestamp
 * ```
 * and two subcollections, `callerCandidates` and `calleeCandidates`, holding each side's network
 * addresses (ICE candidates) as they are found. Each phone writes its own and listens to the
 * other's.
 *
 * The Cloud Function `notifyIncomingCall` (functions/index.js) pushes the callee when a call
 * document is created, so a phone with the app closed still rings.
 */
internal object CallSignaling {
    const val STATUS_RINGING = "ringing"
    const val STATUS_ACCEPTED = "accepted"
    const val STATUS_DECLINED = "declined"
    const val STATUS_ENDED = "ended"
    const val STATUS_MISSED = "missed"

    private const val CALLER_CANDIDATES = "callerCandidates"
    private const val CALLEE_CANDIDATES = "calleeCandidates"

    private val db get() = Firebase.firestore

    private fun callRef(callId: String) = db.collection("calls").document(callId)

    /** A call as read from its document. */
    data class CallDoc(
        val id: String,
        val callerId: String,
        val callerName: String,
        val calleeId: String,
        val calleeName: String,
        val video: Boolean,
        val status: String,
        val offer: SessionDescription?,
        val answer: SessionDescription?,
        val createdAtMillis: Long?,
    )

    /** Creates the call document. The callee's phone starts ringing once this is written. */
    suspend fun createCall(
        callId: String,
        callerId: String,
        callerName: String,
        calleeId: String,
        calleeName: String,
        video: Boolean,
        offer: SessionDescription,
    ) {
        callRef(callId)
            .set(
                mapOf(
                    "callerId" to callerId,
                    "callerName" to callerName,
                    "calleeId" to calleeId,
                    "calleeName" to calleeName,
                    "video" to video,
                    "status" to STATUS_RINGING,
                    "offer" to offer.toMap(),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    suspend fun fetchCall(callId: String): CallDoc? = callRef(callId).get().await().let { snap -> snap.data?.let { parse(snap.id, it) } }

    /** The callee accepts: stores their answer and marks the call accepted in one write. */
    suspend fun answerCall(
        callId: String,
        answer: SessionDescription,
    ) {
        callRef(callId).update(mapOf("answer" to answer.toMap(), "status" to STATUS_ACCEPTED)).await()
    }

    suspend fun setStatus(
        callId: String,
        status: String,
    ) {
        callRef(callId).update("status", status).await()
    }

    suspend fun addCandidate(
        callId: String,
        fromCaller: Boolean,
        candidate: IceCandidate,
    ) {
        callRef(callId)
            .collection(if (fromCaller) CALLER_CANDIDATES else CALLEE_CANDIDATES)
            .add(
                mapOf(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "candidate" to candidate.sdp,
                ),
            ).await()
    }

    /** Calls [onChange] with the call every time its document changes. */
    fun listenToCall(
        callId: String,
        onChange: (CallDoc) -> Unit,
    ): ListenerRegistration =
        callRef(callId).addSnapshotListener { snap, _ ->
            val data = snap?.data ?: return@addSnapshotListener
            onChange(parse(snap.id, data))
        }

    /** Calls [onCandidate] for each candidate the other side adds (including ones already there). */
    fun listenToCandidates(
        callId: String,
        fromCaller: Boolean,
        onCandidate: (IceCandidate) -> Unit,
    ): ListenerRegistration =
        callRef(callId)
            .collection(if (fromCaller) CALLER_CANDIDATES else CALLEE_CANDIDATES)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) return@forEach
                    val d = change.document
                    val sdp = d.getString("candidate") ?: return@forEach
                    val index = d.getLong("sdpMLineIndex")?.toInt() ?: 0
                    onCandidate(IceCandidate(d.getString("sdpMid"), index, sdp))
                }
            }

    /**
     * Calls [onRinging] for every call to [userId] that is still ringing. Used while the app is
     * open, so incoming calls show up even if the push is slow or Cloud Functions aren't deployed.
     */
    fun listenForIncoming(
        userId: String,
        onRinging: (CallDoc) -> Unit,
    ): ListenerRegistration =
        db
            .collection("calls")
            .where(Filter.and(Filter.equalTo("calleeId", userId), Filter.equalTo("status", STATUS_RINGING)))
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        onRinging(parse(change.document.id, change.document.data))
                    }
                }
            }

    private fun SessionDescription.toMap() = mapOf("type" to type.canonicalForm(), "sdp" to description)

    private fun parseSdp(value: Any?): SessionDescription? {
        val map = value as? Map<*, *> ?: return null
        val type = map["type"] as? String ?: return null
        val sdp = map["sdp"] as? String ?: return null
        return SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
    }

    private fun parse(
        id: String,
        data: Map<String, Any?>,
    ) = CallDoc(
        id = id,
        callerId = data["callerId"] as? String ?: "",
        callerName = data["callerName"] as? String ?: "",
        calleeId = data["calleeId"] as? String ?: "",
        calleeName = data["calleeName"] as? String ?: "",
        video = data["video"] as? Boolean ?: false,
        status = data["status"] as? String ?: STATUS_ENDED,
        offer = parseSdp(data["offer"]),
        answer = parseSdp(data["answer"]),
        createdAtMillis = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
    )
}
