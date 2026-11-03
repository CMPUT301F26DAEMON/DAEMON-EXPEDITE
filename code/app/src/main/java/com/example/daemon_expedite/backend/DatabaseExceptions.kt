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
