package ru.sberdevices.sbdv.contacts

import org.telegram.tgnet.TLRPC
import ru.sberdevices.sbdv.model.Contact
import ru.sberdevices.sbdv.util.asciiCode
import ru.sberdevices.sbdv.util.isEnglishLetter
import ru.sberdevices.sbdv.util.isRussianLetter

internal val Contact.firstNameLetter: String?
    get() {
        val firstNameLetter = firstName?.firstOrNull() ?: lastName?.firstOrNull()
        val firstLetter = (firstNameLetter ?: user.username?.firstOrNull())?.toUpperCase()
        firstLetter?.let { letter ->
            if (letter.isRussianLetter() || letter.isEnglishLetter() || letter.isDigit()){
                return letter.toString()
            }
            return null
        }
        return null
    }

internal fun List<Contact>.firstNameLetters(): Set<String> {
    val result = mutableSetOf<String>()
    this.forEach { contact ->
        contact.firstNameLetter?.let { letter ->
            result.add(letter)
        }
    }
    return result
}


internal val TLRPC.User.sortWeight: Int
    get() {
        this.username
        val firstChar = (first_name?.firstOrNull() ?: last_name?.firstOrNull())?.toUpperCase()
        firstChar?.let { char ->
            return when {
                char.isEnglishLetter() -> {
                    2000 + char.asciiCode
                }
                char.isRussianLetter() -> {
                    1000 + char.asciiCode
                }
                char.isDigit() -> {
                    3000 + char.asciiCode
                }
                else -> {
                    char.asciiCode + 4000
                }
            }
        }
        val firstUserNameChar = username?.firstOrNull()?.toUpperCase() ?: Char.MIN_VALUE
        return firstUserNameChar.asciiCode + 5000
    }