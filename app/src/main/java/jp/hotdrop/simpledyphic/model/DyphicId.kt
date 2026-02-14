package jp.hotdrop.simpledyphic.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DyphicId {
    private val idFormatter: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    fun makeRecordId(date: LocalDate): Int = dateToId(date)

    fun dateToId(date: LocalDate): Int = date.format(idFormatter).toInt()

    fun idToDate(id: Int): LocalDate = LocalDate.parse(id.toString(), idFormatter)
}
