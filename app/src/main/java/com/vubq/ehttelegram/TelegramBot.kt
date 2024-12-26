package com.vubq.ehttelegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.vubq.ehttelegram.enums.AutoType
import com.vubq.ehttelegram.enums.EquipmentType

class TelegramBot {

    private lateinit var EHTBot: EHTBot

    private val allowedUserIds = setOf(5543802102)

    val bot = bot {
        token = "7623992481:AAFROtN9qewiJpo-ypdlYlFWkhQSp0mpxbo"
        dispatch {
            text {
                handleTextCommand(text, message)
            }

            callbackQuery {
                handleCallBackQuery(callbackQuery)
            }
        }
    }

    private fun handleTextCommand(text: String, message: Message) {
        EHTBot = EHTBot()
        if (!isUserAllowed(message)) {
            bot.sendMessage(ChatId.fromId(message.chat.id), "Bạn không có quyền sử dụng bot này!")
        }
        when (text) {
            "/start" -> {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = AutoType.EQUIP.description,
                            callbackData = AutoType.EQUIP.code
                        ),
                        InlineKeyboardButton.CallbackData(
                            text = AutoType.MOUNT.description,
                            callbackData = AutoType.MOUNT.code
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(text = "Thoát", callbackData = "exit")
                    )
                )

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Chọn loại auto:",
                    replyMarkup = inlineKeyboardMarkup
                )
//                bot.sendMessage(ChatId.fromId(message.chat.id), "Bot đã được khởi động!")
            }

            "/checkBot" -> {
                bot.sendMessage(ChatId.fromId(message.chat.id), "Bot đang hoạt động!")
            }

            "/auto" -> {
                EHTBot.screenCapture()
            }
        }
    }

    private fun handleCallBackQuery(callbackQuery: CallbackQuery) {
        if (AutoType.isValidCode(callbackQuery.data)) {
            AutoType.fromCode(callbackQuery.data)?.let { EHTBot.setAutoType(it) }
        }
        if (EquipmentType.isValidCode(callbackQuery.data)) {
            EquipmentType.fromCode(callbackQuery.data)?.let { EHTBot.setEquipmentType(it) }

        }
        when (callbackQuery.data) {
            AutoType.EQUIP.code -> {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.ARMOR.description,
                            callbackData = EquipmentType.ARMOR.code
                        ),
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.GLOVES.description,
                            callbackData = EquipmentType.GLOVES.code
                        ),
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.SHOE.description,
                            callbackData = EquipmentType.SHOE.code
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.NECKLACE.description,
                            callbackData = EquipmentType.NECKLACE.code
                        ),
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.RING.description,
                            callbackData = EquipmentType.RING.code
                        ),
                        InlineKeyboardButton.CallbackData(
                            text = EquipmentType.WEAPON.description,
                            callbackData = EquipmentType.WEAPON.code
                        )
                    )
                )

                bot.sendMessage(
                    chatId = ChatId.fromId(callbackQuery.from.id),
                    text = "Chọn loại trang bị:",
                    replyMarkup = inlineKeyboardMarkup
                )
            }
        }
        bot.sendMessage(ChatId.fromId(callbackQuery.from.id), callbackQuery.data)
    }

    private fun isUserAllowed(message: Message): Boolean {
        val userId = message.from?.id
        return userId != null && allowedUserIds.contains(userId)
    }

    fun start() {
        bot.startPolling()
    }

//    fun stopNotice() {
//        bot.sendMessage(ChatId.fromId(5543802102), "Bot đã dừng!")
//    }
}