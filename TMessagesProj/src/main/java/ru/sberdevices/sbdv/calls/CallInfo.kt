package ru.sberdevices.sbdv.calls

import ru.sberdevices.sbdv.model.Contact
import java.util.Date

enum class CallType {
    OUT, IN, MISSED
}

data class CallInfo(
    val contact: Contact,
    val type: CallType,
    val callsCount: Int,
    val date: Date
)