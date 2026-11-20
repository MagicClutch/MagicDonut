package de.magicclutch.magicdonut.client.payconfirm

/** Whether the quick-pay key (set in the OneConfig settings) is currently held down. */
object QuickPayState {
    @Volatile
    var held: Boolean = false
}
