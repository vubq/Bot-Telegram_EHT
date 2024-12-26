package com.vubq.ehttelegram.enums

enum class AutoType(val code: String, val description: String) {
    NULL("NULL", "Null"),
    EQUIP("EQUIP", "Trang bị"),
    MOUNT("MOUNT", "Thú cưỡi");

    companion object {
        fun isValidCode(value: String): Boolean {
            return values().any { it.code == value }
        }

        fun fromCode(value: String): AutoType? {
            return AutoType.values().find { it.code == value }
        }
    }
}