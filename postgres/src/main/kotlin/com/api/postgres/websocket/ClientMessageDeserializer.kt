package com.api.postgres.websocket

import com.api.postgres.ClientMessage
import com.api.postgres.ServerMessage
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.slf4j.LoggerFactory
import java.lang.reflect.Type

class ClientMessageDeserializer : JsonDeserializer<ClientMessage> {
    private val logger = LoggerFactory.getLogger(ClientMessageDeserializer::class.java)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ClientMessage {
        try {
            if (!json.isJsonObject) {
                throw JsonParseException("Expected JSON object, got ${json.javaClass.simpleName}")
            }

            val jsonObject = json.asJsonObject
            logger.debug("Deserializing message: $jsonObject")

            // Check if expandedSections exists first
            val sectionsElement = jsonObject.get("expandedSections")
                ?: throw JsonParseException("expandedSections field is missing")

            if (!sectionsElement.isJsonArray) {
                throw JsonParseException("expandedSections must be an array")
            }

            try {
                val expandedSections = sectionsElement.asJsonArray
                    .mapNotNull { element ->
                        try {
                            element.asInt
                        } catch (e: Exception) {
                            logger.warn("Invalid section ID in array: $element")
                            null
                        }
                    }
                    .toSet()

                return ClientMessage.UpdateSubscription(expandedSections)
            } catch (e: Exception) {
                throw JsonParseException("Error parsing expandedSections: ${e.message}")
            }
        } catch (e: Exception) {
            // Log the full error and the JSON that caused it
            logger.error("Error deserializing message: $json", e)
            throw e
        }
    }
}

class ServerMessageSerializer : JsonSerializer<ServerMessage> {
    override fun serialize(
        src: ServerMessage,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()

        when (src) {
            is ServerMessage.NewRootComment -> {
                jsonObject.addProperty("type", "NEW_ROOT_COMMENT")
                jsonObject.add("comment", context.serialize(src.comment))
            }
            is ServerMessage.NewReply -> {
                jsonObject.addProperty("type", "NEW_REPLY")
                jsonObject.add("comment", context.serialize(src.comment))
                jsonObject.addProperty("parentId", src.parentId)
            }
            is ServerMessage.ReplyCountUpdate -> {
                jsonObject.addProperty("type", "REPLY_COUNT_UPDATE")
                jsonObject.addProperty("commentId", src.commentId)
                jsonObject.addProperty("newCount", src.newCount)
            }
        }

        return jsonObject
    }
}