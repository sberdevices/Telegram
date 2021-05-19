package ru.sberdevices.sbdv.model

import org.telegram.tgnet.TLRPC
import java.util.Objects

data class Contact(
    val id: Int,
    val firstName: String?,
    val lastName: String?,
    val user: TLRPC.User,
    val displayedNumber: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Contact?
        return id == that?.id && firstName == that.firstName && lastName == that.lastName && user.username == that.user.username && displayedNumber == that.displayedNumber
    }

    override fun hashCode(): Int {
        return Objects.hash(id, firstName, lastName, user.username, displayedNumber)
    }
}
