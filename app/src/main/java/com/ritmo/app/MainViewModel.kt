package com.ritmo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = StepRepository(application)
    private val _now = MutableStateFlow(LocalDateTime.now())
    val now = _now.asStateFlow()
    fun refresh() { _now.value = LocalDateTime.now() }
}
