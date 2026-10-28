package com.floatscript.app.ui.update

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floatscript.app.R
import com.floatscript.app.update.InstallSource
import com.floatscript.app.viewmodel.AppViewModelProvider
import com.floatscript.app.viewmodel.UpdateUiState
import com.floatscript.app.viewmodel.UpdateViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fromPlay = remember { InstallSource.isFromPlay(context) }

    LaunchedEffect(Unit) {
        if (!fromPlay && state is UpdateUiState.Idle) viewModel.check()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.update_current_version, viewModel.currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (fromPlay) {
                // Play policy: apps installed from Play must update through Play.
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.update_managed_by_play),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { openPlayListing(context) },
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(stringResource(R.string.update_open_play))
                        }
                    }
                }
                return@Column
            }

            when (val s = state) {
                is UpdateUiState.Idle, is UpdateUiState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text(stringResource(R.string.update_checking))
                    }
                }

                is UpdateUiState.UpToDate -> {
                    Text(stringResource(R.string.update_uptodate))
                    OutlinedButton(onClick = { viewModel.check() }) {
                        Text(stringResource(R.string.update_check_again))
                    }
                }

                is UpdateUiState.Available -> {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.update_available, s.info.version),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (s.info.notes.isNotBlank()) {
                                Text(
                                    s.info.notes.take(1200),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            if (s.info.sizeBytes > 0) {
                                Text(
                                    stringResource(R.string.update_size, formatBytes(s.info.sizeBytes)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Button(
                                onClick = { viewModel.download(context, s.info) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(stringResource(R.string.update_download))
                            }
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    val total = s.total
                    val fraction = if (total > 0) (s.downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.update_downloading, s.info.version),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Box(Modifier.padding(top = 12.dp)) {
                                if (total > 0) {
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                            }
                            Text(
                                text = if (total > 0) {
                                    stringResource(
                                        R.string.update_progress,
                                        (fraction * 100).toInt(),
                                        formatBytes(s.downloaded),
                                        formatBytes(total)
                                    )
                                } else {
                                    stringResource(R.string.update_progress_unknown, formatBytes(s.downloaded))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedButton(
                                onClick = { viewModel.cancelDownload() },
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Text(stringResource(R.string.update_cancel))
                            }
                        }
                    }
                }

                is UpdateUiState.Downloaded -> {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.update_downloaded, s.info.version),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.update_install_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Button(
                                onClick = { installApk(context, s.file) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(stringResource(R.string.update_install))
                            }
                        }
                    }
                }

                is UpdateUiState.Error -> {
                    Text(stringResource(R.string.update_error, s.message))
                    OutlinedButton(onClick = { viewModel.check() }) {
                        Text(stringResource(R.string.update_check_again))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun installApk(context: Context, file: File) {
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, context.getString(R.string.update_install_failed), Toast.LENGTH_LONG).show()
    }
}

private fun openPlayListing(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = android.net.Uri.parse("market://details?id=${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
