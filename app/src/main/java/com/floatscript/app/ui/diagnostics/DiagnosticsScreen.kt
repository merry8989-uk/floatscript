package com.floatscript.app.ui.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.floatscript.app.R
import com.floatscript.app.diagnostics.Diagnostics
import java.io.File

/**
 * Local diagnostics viewer. Shows the recent log and any crash report from a
 * previous run, and lets the user share / save / clear it. Nothing is uploaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var report by remember { mutableStateOf(Diagnostics.fullReportText()) }
    var hasCrash by remember { mutableStateOf(Diagnostics.lastCrashFile?.exists() == true) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(Diagnostics.fullReportText().toByteArray())
            }
        }.isSuccess
        Toast.makeText(
            context,
            context.getString(if (ok) R.string.diagnostics_saved else R.string.diagnostics_save_failed),
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
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
                .padding(12.dp)
        ) {
            Text(
                stringResource(R.string.diagnostics_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasCrash) {
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.diagnostics_crash_banner),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { shareReport(context) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(stringResource(R.string.diagnostics_share_crash))
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { shareReport(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diagnostics_share))
                }
                OutlinedButton(
                    onClick = { saveLauncher.launch("floatscript-diagnostics.txt") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diagnostics_save))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { report = Diagnostics.fullReportText() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diagnostics_refresh))
                }
                OutlinedButton(
                    onClick = {
                        Diagnostics.clear()
                        report = Diagnostics.fullReportText()
                        hasCrash = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diagnostics_clear))
                }
            }

            Text(
                text = report,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

/** Writes the report to a file and hands it to the system share sheet. */
private fun shareReport(context: Context) {
    val file: File = Diagnostics.writeReportFile() ?: run {
        Toast.makeText(context, context.getString(R.string.diagnostics_save_failed), Toast.LENGTH_SHORT).show()
        return
    }
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "FloatScript diagnostics")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.diagnostics_share)))
}
