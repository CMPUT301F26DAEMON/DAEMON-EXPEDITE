package com.example.daemon_expedite.backend

class ChatNotFoundException(
    val chatId: Id,
) : Exception("Chat $chatId not found")

class InvalidChatDataException(
    val chatId: Id,
    reason: String,
) : Exception("Invalid data for chat $chatId: $reason")

class InvalidDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class NotFoundException(
    message: String,
) : Exception(message)

/** Thrown when joining or leaving a lottery outside its entry period. */
class LotteryClosedException(
    val lotteryId: Id,
) : Exception("Lottery $lotteryId is not open for entries")

/** Thrown when cancelling a lottery whose stays have started, or that is already over. */
class LotteryNotCancellableException(
    val lotteryId: Id,
) : Exception("Lottery $lotteryId can no longer be cancelled")

/**
 * Thrown when edited lottery settings break the rules against the latest saved lottery, for
 * example more entrants joined than the new waitlist limit. [problems] says what to fix.
 */
class LotteryEditRejectedException(
    val lotteryId: Id,
    val problems: LotteryProblems,
) : Exception("Edit to lottery $lotteryId rejected: $problems")

/** Thrown when joining a lottery whose waitlist limit has been reached. */
class LotteryFullException(
    val lotteryId: Id,
) : Exception("Lottery $lotteryId waiting list is full")
