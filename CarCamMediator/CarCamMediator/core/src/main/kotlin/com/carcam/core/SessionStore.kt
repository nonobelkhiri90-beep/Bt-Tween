package com.carcam.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CarSession(
    val carConnected: Boolean = false,
    val musicActive: Boolean = false,
    val monitoring: Boolean = false,
) {
    /** الشرط الذي يجعل التطبيق جاهزاً لعرض زر التسجيل */
    val ready: Boolean get() = carConnected && musicActive
}

object SessionStore {
    private val _state = MutableStateFlow(CarSession())
    val state: StateFlow<CarSession> = _state.asStateFlow()
    fun update(f: (CarSession) -> CarSession) = _state.update(f)
}
