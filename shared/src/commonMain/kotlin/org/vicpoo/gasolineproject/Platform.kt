package org.vicpoo.gasolineproject

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform