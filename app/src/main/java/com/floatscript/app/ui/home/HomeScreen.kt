package com.floatscript.app.ui.home

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatscript.app.R
import com.floatscript.app.model.Script
import com.floatscript.app.overlay.OverlayService
import com.floatscript.app.diagnostics.Diagnostics
import com.floatscript.app.ui.editor.ScriptEditorContent
import com.floatscript.app.ui.icons.AppIcons
import com.floatscript.app.viewmodel.AppViewModelProvider
import com.floatscript.app.viewmodel.ScriptListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEditScript: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecording: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    viewModel: ScriptListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scripts by viewModel.scriptList.collectAsStateWithLifecycle()

    // Tablet / foldable: list on the left, editor on the right.
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    var selectedId by remember { mutableStateOf<Long?>(null) }
    fun openEditor(id: Long) {
        if (wide) selectedId = id else onEditScript(id)
    }

    // The service's running flag is process-global, so refresh it whenever we
    // come back to the foreground.
    var overlayRunning by remember { mutableStateOf(OverlayService.isRunning) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) overlayRunning = OverlayService.isRunning
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var overlayPermissionGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var showRename by remember { mutableStateOf<Script?>(null) }
    var topMenuOpen by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { overlayPermissionGranted = Settings.canDrawOverlays(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: overlay still works without the notification */ }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val text = readTextFromUri(context, it) ?: return@let
            val name = it.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".txt") ?: "Imported script"
            viewModel.createScript(name, text) { id -> openEditor(id) }
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.buildBackup { json ->
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }.isSuccess
            Toast.makeText(
                context,
                context.getString(if (ok) R.string.backup_saved else R.string.backup_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = readTextFromUri(context, uri)
        if (json == null) {
            Toast.makeText(context, context.getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        viewModel.restoreBackup(json) { count ->
            val msg = if (count >= 0) context.getString(R.string.restore_done, count)
            else context.getString(R.string.restore_failed)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun startOverlay(script: Script) {
        if (!Settings.canDrawOverlays(context)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val started = viewModel.startOverlay(context, script)
        if (started) {
            overlayRunning = true
        } else {
            Diagnostics.w("Overlay", "system refused to start the foreground service")
            Toast.makeText(context, context.getString(R.string.overlay_start_failed), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenRecording) {
                        Icon(AppIcons.Videocam, contentDescription = stringResource(R.string.record_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_settings))
                    }
                    Box {
                        IconButton(onClick = { topMenuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.home_more))
                        }
                        DropdownMenu(expanded = topMenuOpen, onDismissRequest = { topMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_scripts)) },
                                onClick = {
                                    topMenuOpen = false
                                    createBackupLauncher.launch("floatscript-backup.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restore_scripts)) },
                                onClick = {
                                    topMenuOpen = false
                                    openBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.diagnostics_open)) },
                                onClick = {
                                    topMenuOpen = false
                                    onOpenDiagnostics()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(if (wide) 0.45f else 1f)
            ) {
            if (!overlayPermissionGranted) {
                OverlayPermissionCard(
                    onGrant = {
                        overlayPermissionLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openEditor(0L) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.home_new_script), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("text/plain")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.home_import_txt), maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = {
                    val pasted = readClipboard(context)
                    if (!pasted.isNullOrBlank()) {
                        viewModel.createScript("Pasted script", pasted) { id -> openEditor(id) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Text(stringResource(R.string.home_paste_clipboard))
            }

            if (scripts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        ScriptCard(
                            script = script,
                            isActive = overlayRunning,
                            onUse = { startOverlay(script) },
                            onEdit = { openEditor(script.id) },
                            onRename = { showRename = script },
                            onDuplicate = { viewModel.duplicate(script) },
                            onDelete = { viewModel.delete(script) }
                        )
                    }
                }
            }
        }

            if (wide) {
                VerticalDivider()
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(0.55f)
                ) {
                    val id = selectedId
                    if (id != null) {
                        ScriptEditorContent(
                            scriptId = id,
                            onSaved = { saved -> selectedId = saved }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.select_script_to_edit),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Start / stop the overlay from the FAB area.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        if (scripts.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (overlayRunning) {
                        viewModel.stopOverlay(context)
                        overlayRunning = false
                    } else {
                        startOverlay(scripts.first())
                    }
                },
                icon = {
                    Icon(
                        if (overlayRunning) AppIcons.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        stringResource(
                            if (overlayRunning) R.string.home_stop_overlay else R.string.home_start_overlay
                        )
                    )
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    showRename?.let { script ->
        RenameDialog(
            initial = script.title,
            onDismiss = { showRename = null },
            onConfirm = { newTitle ->
                viewModel.rename(script, newTitle)
                showRename = null
            }
        )
    }
}

@Composable
private fun OverlayPermissionCard(onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.permission_overlay_rationale),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
        }
    }
}

@Composable
private fun ScriptCard(
    script: Script,
    isActive: Boolean,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(script.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    script.body.take(80).replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    DateUtils.getRelativeTimeSpanString(script.updatedAt).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onUse) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_use))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate)) },
                        onClick = { menuOpen = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.editor_name_hint)) }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.ifBlank { initial }) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun readTextFromUri(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
}.getOrNull()
