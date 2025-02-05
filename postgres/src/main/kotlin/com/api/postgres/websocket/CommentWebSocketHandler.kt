package com.api.postgres.websocket

import com.api.postgres.ClientMessage
import com.api.postgres.CommentDto
import com.api.postgres.ServerMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import com.api.postgres.services.Comments
import org.springframework.web.socket.CloseStatus

@Component
class CommentWebSocketHandler(
    private val commentService: Comments
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(CommentWebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val subscriptions = ConcurrentHashMap<String, Set<Int>>()

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ClientMessage::class.java, ClientMessageDeserializer())
        .registerTypeAdapter(ServerMessage::class.java, ServerMessageSerializer())
        .create()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("New WebSocket connection established: ${session.id}")
        sessions[session.id] = session
        subscriptions[session.id] = emptySet()
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("WebSocket connection closed: ${session.id}, status: $status")
        sessions.remove(session.id)
        subscriptions.remove(session.id)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            logger.debug("Received message from ${session.id}: ${message.payload}")

            val clientMessage = gson.fromJson(message.payload, ClientMessage::class.java)
            when (clientMessage) {
                is ClientMessage.UpdateSubscription -> {
                    logger.debug("Updating subscriptions for ${session.id}: ${clientMessage.expandedSections}")
                    subscriptions[session.id] = clientMessage.expandedSections
                    // Send confirmation back to client
                    session.sendMessage(TextMessage("""{"type":"SUBSCRIPTION_UPDATED","count":${clientMessage.expandedSections.size}}"""))
                }
            }
        } catch (e: JsonParseException) {
            logger.error("Error parsing message from ${session.id}: ${message.payload}", e)
            try {
                session.sendMessage(TextMessage("""{"type":"ERROR","message":"Invalid message format"}"""))
            } catch (e: Exception) {
                logger.error("Failed to send error message to client", e)
            }
        } catch (e: Exception) {
            logger.error("Unexpected error handling message from ${session.id}", e)
            try {
                session.sendMessage(TextMessage("""{"type":"ERROR","message":"Server error"}"""))
            } catch (e: Exception) {
                logger.error("Failed to send error message to client", e)
            }
        }
    }

    suspend fun broadcastNewReply(comment: CommentDto, parentId: Int) {
        try {
            val rootParentId = commentService.findRootParentId(parentId) ?: parentId
            val message = ServerMessage.NewReply(comment, parentId)
            val jsonMessage = gson.toJson(message)

            sessions.forEach { (sessionId, session) ->
                if (session.isOpen && subscriptions[sessionId]?.contains(rootParentId) == true) {
                    try {
                        session.sendMessage(TextMessage(jsonMessage))
                    } catch (e: Exception) {
                        logger.error("Error sending reply to session $sessionId", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting new reply", e)
        }
    }

    fun broadcastNewRootComment(comment: CommentDto) {
        try {
            val message = ServerMessage.NewRootComment(comment)
            broadcastToAll(message)
        } catch (e: Exception) {
            logger.error("Error broadcasting new root comment", e)
        }
    }

    fun broadcastReplyCount(commentId: Int, newCount: Int) {
        try {
            val message = ServerMessage.ReplyCountUpdate(commentId, newCount)
            broadcastToAll(message)
        } catch (e: Exception) {
            logger.error("Error broadcasting reply count", e)
        }
    }

    private fun broadcastToAll(message: ServerMessage) {
        val json = gson.toJson(message)
        logger.debug("Broadcasting to all: $json")

        sessions.values.forEach { session ->
            if (session.isOpen) {
                try {
                    session.sendMessage(TextMessage(json))
                } catch (e: Exception) {
                    logger.error("Error sending message to session ${session.id}", e)
                }
            }
        }
    }
}