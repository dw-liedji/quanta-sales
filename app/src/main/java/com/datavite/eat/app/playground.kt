package com.datavite.eat.app

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    println(LocalDateTime.now().toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS"))
    )
}