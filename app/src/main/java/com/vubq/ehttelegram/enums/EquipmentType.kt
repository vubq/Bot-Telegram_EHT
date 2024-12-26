package com.vubq.ehttelegram.enums

enum class EquipmentType(val code: String, val description: String) {
    NULL("NULL", "NULL"),
    ARMOR("ARMOR", "Giáp"),
    GLOVES("GLOVES", "Găng"),
    SHOE("SHOE", "Giày"),
    NECKLACE("NECKLACE", "Dây chuyền"),
    RING("RING", "Nhẫn"),
    WEAPON("WEAPON", "Vũ khí");

    companion object {
        fun isValidCode(value: String): Boolean {
            return AutoType.values().any { it.code == value }
        }

        fun fromCode(value: String): EquipmentType? {
            return values().find { it.code == value }
        }
    }
}