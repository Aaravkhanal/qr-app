package com.modernqr.app.scanner

/** Ensures that exactly one scan is accepted before the visible result is dismissed. */
class ScanSessionGate(initiallyLocked: Boolean = false) {
    private var locked = initiallyLocked

    @Synchronized
    fun tryAccept(): Boolean {
        if (locked) return false
        locked = true
        return true
    }

    @Synchronized
    fun dismiss() {
        locked = false
    }
}
