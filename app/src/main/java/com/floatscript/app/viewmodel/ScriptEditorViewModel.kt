package com.floatscript.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatscript.app.data.ScriptRepository
import com.floatscript.app.model.Script
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backing state for the script editor screen (create or edit an existing script). */
class ScriptEditorViewModel(private val repository: ScriptRepository) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private var scriptId: Long = 0L
    private var loaded = false

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        scriptId = id
        if (id == 0L) return
        viewModelScope.launch {
            repository.get(id)?.let {
                _title.value = it.title
                _body.value = it.body
            }
        }
    }

    fun onTitleChange(value: String) { _title.value = value }
    fun onBodyChange(value: String) { _body.value = value }
    fun appendText(value: String) { _body.value = _body.value + value }

    fun save(onSaved: (Long) -> Unit) {
        val t = _title.value.ifBlank { "Untitled script" }
        viewModelScope.launch {
            val id = repository.upsert(Script(id = scriptId, title = t, body = _body.value))
            scriptId = id
            onSaved(id)
        }
    }
}
