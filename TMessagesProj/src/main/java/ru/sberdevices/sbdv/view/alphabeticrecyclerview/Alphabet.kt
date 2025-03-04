package ru.sberdevices.sbdv.view.alphabeticrecyclerview

sealed class Alphabet(val letters: Array<String>) {
    object EN : Alphabet(
        arrayOf(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        )
    )

    object RU : Alphabet(
        arrayOf(
            "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й", "К", "Л", "М",
            "Н", "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ",
            "Ы", "Ь", "Э", "Ю", "Я"
        )
    )

    object Numbers : Alphabet(
        arrayOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        )
    )
}