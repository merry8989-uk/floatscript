package com.floatscript.app.ui.editor

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatscript.app.R
import com.floatscript.app.overlay.OverlayService
import com.floatscript.app.viewmodel.AppViewModelProvider
import com.floatscript.app.viewmodel.ScriptEditorViewModel

/**
 * The editor form on its own, without any scaffolding. Used both by
 * [ScriptEditorScreen] (phone) and by the two-pane tablet layout on the home
 * screen.
 */
@Composable
fun ScriptEditorContent(
    scriptId: Long,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScriptEditorViewModel = viewModel(
        key = "editor-$scriptId",
        factory = AppViewModelProvider.Factory
    )
) {
    val context = LocalContext.current
    viewModel.load(scriptId)

    val title by viewModel.title.collectAsStateWithLifecycle()
    val body by viewModel.body.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.editor_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = viewModel::onBodyChange,
            label = { Text(stringResource(R.string.editor_body_hint)) },
            minLines = 10,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        OutlinedButton(
            onClick = { readClipboard(context)?.let(viewModel::appendText) },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.home_paste_clipboard))
        }
        Button(
            onClick = {
                viewModel.save { id ->
                    // If this script is the one on screen, refresh the overlay.
                    OverlayService.updateIfCurrent(context, id)
                    onSaved(id)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

/** Full-screen editor used on phones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptId: Long,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        ScriptEditorContent(
            scriptId = scriptId,
            onSaved = { onBack() },
            modifier = Modifier.padding(padding)
        )
    }
}

private fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
}
