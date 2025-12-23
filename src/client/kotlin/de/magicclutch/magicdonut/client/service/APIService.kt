package de.magicclutch.magicdonut.client.service

import java.util.regex.Pattern

class APIService {
    companion object {
        val apiTokenPattern: Pattern = Pattern.compile("API (Key|Token is): ([a-z0-9]{32})")
    }
}