package com.example.features.sourcemanager

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.core.common.isInternalOrLocalAddress
import com.example.core.verification.VerificationWebView
import com.example.domain.models.ContentRating
import com.example.domain.models.MangaSummary
import com.example.domain.models.SourceConfiguration
import com.example.domain.models.SourceDiagnosticReport
import com.example.domain.models.SourceEngineType
import com.example.domain.models.SourceFullTestReport
import com.example.domain.models.SourceInfo
import com.example.domain.models.SourceStatus
import com.example.features.AddSiteDialogState
import com.example.features.navigation.SourceBadgeIcon
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SourceManagerScreen(
    sources: List<SourceInfo>,
    showRestartBanner: Boolean,
    onNavigateBack: () -> Unit,
    onOpenAddSiteDialog: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onToggleSource: (String, Boolean) -> Unit,
    onDeleteSource: (String) -> Unit,
    onOpenSourceCatalog: (String) -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onClearAllSources: () -> Unit = {},
    onOpenConfigEditor: (String) -> Unit = {},
    onRunFullTest: (String) -> Unit = {},
    onUpdateSourceMetadata: (String, String, String, String, ContentRating, SourceEngineType) -> Unit = { _, _, _, _, _, _ -> },
    onExportSourceJson: (String) -> Unit = {},
    onImportSourceJson: (String) -> Unit = {},
    exportedConfigJson: String? = null,
    onDismissExportedJson: () -> Unit = {},
    editingConfig: SourceConfiguration? = null,
    previewItems: List<MangaSummary> = emptyList(),
    isPreviewingConfig: Boolean = false,
    onPreviewConfig: (SourceConfiguration) -> Unit = {},
    onSaveConfig: (SourceConfiguration) -> Unit = {},
    onCloseConfigEditor: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var metadataEditingSource by remember { mutableStateOf<SourceInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Title "إدارة المواقع", Back arrow, Diagnostics, Import JSON, Browser & Add icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("source_manager_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إدارة المواقع",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showImportJsonDialog = true },
                        modifier = Modifier.testTag("top_import_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "استيراد إعدادات مصدر JSON"
                        )
                    }
                    IconButton(
                        onClick = onOpenDiagnostics,
                        modifier = Modifier.testTag("top_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "تشخيص المصادر"
                        )
                    }
                    if (sources.isNotEmpty()) {
                        IconButton(
                            onClick = { onOpenAddSiteDialog("") },
                            modifier = Modifier.testTag("top_add_site_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "إضافة موقع"
                            )
                        }
                        IconButton(
                            onClick = { onOpenBrowser("https://www.google.com") },
                            modifier = Modifier.testTag("top_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "المتصفح"
                            )
                        }
                        IconButton(
                            onClick = onClearAllSources,
                            modifier = Modifier.testTag("top_clear_all_sources_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "حذف جميع المواقع"
                            )
                        }
                    }
                }
            }

            if (sources.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFF454B59),
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_sources_message),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = { onOpenAddSiteDialog("") },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp)
                                .testTag("add_site_empty_button")
                        ) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة موقع", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onOpenBrowser("https://www.google.com") },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .width(200.dp)
                                .height(48.dp)
                                .testTag("open_browser_empty_button")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المتصفح", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showImportJsonDialog = true },
                            modifier = Modifier.testTag("import_json_empty_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد إعدادات مصدر محفوظة (JSON)")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sources_list")
                ) {
                    items(sources, key = { it.id }) { src ->
                        var showMenu by remember { mutableStateOf(false) }
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSourceCatalog(src.id) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "ترتيب",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Switch(
                                        checked = src.isEnabled,
                                        onCheckedChange = { onToggleSource(src.id, it) },
                                        modifier = Modifier.testTag("source_switch_${src.id}")
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    SourceBadgeIcon(
                                        text = src.iconText,
                                        colorHex = src.iconColorHex,
                                        sizeDp = 38
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = src.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = Color(src.contentRating.badgeColorHex).copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = src.contentRating.badgeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(src.contentRating.badgeColorHex),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${src.baseUrl} • ${src.engineType.labelAr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "الحالة: ${src.status.labelAr} • الأعمال: ${src.extractedMangaCount} • آخر تحديث: ${dateFormat.format(Date(src.lastCheckedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!src.lastError.isNullOrBlank()) {
                                            Text(
                                                text = "تنبيه: ${src.lastError}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFEF5350),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.testTag("source_menu_${src.id}")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات المصدر")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("فتح أعمال المصدر") },
                                            onClick = {
                                                showMenu = false
                                                onOpenSourceCatalog(src.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("اختبار المصدر الشامل (11 مرحلة)") },
                                            onClick = {
                                                showMenu = false
                                                onRunFullTest(src.id)
                                                onOpenDiagnostics()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("إعداد محددات CSS ومعاينة حية") },
                                            onClick = {
                                                showMenu = false
                                                onOpenConfigEditor(src.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("تعديل بيانات المصدر") },
                                            onClick = {
                                                showMenu = false
                                                metadataEditingSource = src
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("تصدير إعدادات المصدر (JSON)") },
                                            onClick = {
                                                showMenu = false
                                                onExportSourceJson(src.id)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("تشخيص المصدر") },
                                            onClick = {
                                                showMenu = false
                                                onOpenDiagnostics()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("فتح في المتصفح الداخلي") },
                                            onClick = {
                                                showMenu = false
                                                onOpenBrowser(src.baseUrl)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("حذف الموقع") },
                                            onClick = {
                                                showMenu = false
                                                onDeleteSource(src.id)
                                            }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        }

        if (showRestartBanner) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF232733),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "عليك إغلاق التطبيق واعادة تشغيله من اجل تطبيق التغييرات",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }

    if (editingConfig != null) {
        SourceSelectorConfigDialog(
            initialConfig = editingConfig,
            previewItems = previewItems,
            isPreviewing = isPreviewingConfig,
            onPreview = onPreviewConfig,
            onSave = onSaveConfig,
            onDismiss = onCloseConfigEditor
        )
    }

    if (metadataEditingSource != null) {
        val target = metadataEditingSource!!
        EditSourceMetadataDialog(
            source = target,
            onConfirm = { newName, newUrl, lang, rating, engine ->
                onUpdateSourceMetadata(target.id, newName, newUrl, lang, rating, engine)
                metadataEditingSource = null
            },
            onDismiss = { metadataEditingSource = null }
        )
    }

    if (showImportJsonDialog || !exportedConfigJson.isNullOrBlank()) {
        ImportExportSourceConfigDialog(
            exportedJson = exportedConfigJson,
            onImportJson = { json ->
                onImportSourceJson(json)
                showImportJsonDialog = false
            },
            onDismiss = {
                showImportJsonDialog = false
                onDismissExportedJson()
            }
        )
    }
}

@Composable
fun SourceSelectorConfigDialog(
    initialConfig: SourceConfiguration,
    previewItems: List<MangaSummary>,
    isPreviewing: Boolean,
    onPreview: (SourceConfiguration) -> Unit,
    onSave: (SourceConfiguration) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceName by remember(initialConfig) { mutableStateOf(initialConfig.sourceName) }
    var engineType by remember(initialConfig) { mutableStateOf(initialConfig.engineType) }
    var contentRating by remember(initialConfig) { mutableStateOf(initialConfig.contentRating) }
    var popularPath by remember(initialConfig) { mutableStateOf(initialConfig.popularPath) }
    var searchPath by remember(initialConfig) { mutableStateOf(initialConfig.searchPath) }
    var mangaCardSelector by remember(initialConfig) { mutableStateOf(initialConfig.mangaItemSelector) }
    var mangaTitleSelector by remember(initialConfig) { mutableStateOf(initialConfig.titleSelector) }
    var mangaUrlSelector by remember(initialConfig) { mutableStateOf(initialConfig.mangaUrlSelector) }
    var coverImageSelector by remember(initialConfig) { mutableStateOf(initialConfig.coverSelector) }
    var detailsDescriptionSelector by remember(initialConfig) { mutableStateOf(initialConfig.descriptionSelector) }
    var detailsGenresSelector by remember(initialConfig) { mutableStateOf(initialConfig.genreSelector) }
    var detailsStatusSelector by remember(initialConfig) { mutableStateOf(initialConfig.statusSelector) }
    var chapterListSelector by remember(initialConfig) { mutableStateOf(initialConfig.chapterListSelector) }
    var chapterUrlSelector by remember(initialConfig) { mutableStateOf(initialConfig.chapterUrlSelector) }
    var pageImageSelector by remember(initialConfig) { mutableStateOf(initialConfig.pageImageSelector) }
    var nextPageSelector by remember(initialConfig) { mutableStateOf(initialConfig.nextPageSelector) }

    fun buildCurrentConfig(): SourceConfiguration {
        return initialConfig.copy(
            sourceName = sourceName.trim().ifBlank { initialConfig.sourceName },
            engineType = engineType,
            contentRating = contentRating,
            popularPath = popularPath.trim(),
            searchPath = searchPath.trim(),
            mangaCardSelector = mangaCardSelector.trim(),
            mangaItemSelector = mangaCardSelector.trim(),
            mangaTitleSelector = mangaTitleSelector.trim(),
            titleSelector = mangaTitleSelector.trim(),
            mangaUrlSelector = mangaUrlSelector.trim(),
            coverImageSelector = coverImageSelector.trim(),
            coverSelector = coverImageSelector.trim(),
            detailsDescriptionSelector = detailsDescriptionSelector.trim(),
            descriptionSelector = detailsDescriptionSelector.trim(),
            detailsGenresSelector = detailsGenresSelector.trim(),
            genreSelector = detailsGenresSelector.trim(),
            detailsStatusSelector = detailsStatusSelector.trim(),
            statusSelector = detailsStatusSelector.trim(),
            chapterListSelector = chapterListSelector.trim(),
            chapterUrlSelector = chapterUrlSelector.trim(),
            pageImageSelector = pageImageSelector.trim(),
            nextPageSelector = nextPageSelector.trim(),
            updatedAt = System.currentTimeMillis()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(640.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Text(
                    text = "إعداد محددات المصدر (${initialConfig.domain})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("اسم المصدر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("نوع الموصل:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            SourceEngineType.MADARA,
                            SourceEngineType.MANGASTREAM,
                            SourceEngineType.MANGAREADER,
                            SourceEngineType.CUSTOM_HTML,
                            SourceEngineType.JSON_API,
                            SourceEngineType.RSS_FEED,
                            SourceEngineType.WEBVIEW_ONLY
                        ).forEach { eng ->
                            FilterChip(
                                selected = engineType == eng,
                                onClick = { engineType = eng },
                                label = { Text(eng.name) }
                            )
                        }
                    }

                    Text("تصنيف المحتوى:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ContentRating.entries.forEach { r ->
                            FilterChip(
                                selected = contentRating == r,
                                onClick = { contentRating = r },
                                label = { Text(r.labelAr) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = popularPath,
                        onValueChange = { popularPath = it },
                        label = { Text("مسار الأعمال الشائعة (popularPath)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mangaCardSelector,
                        onValueChange = { mangaCardSelector = it },
                        label = { Text("محدد بطاقة المانجا (mangaCardSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mangaTitleSelector,
                        onValueChange = { mangaTitleSelector = it },
                        label = { Text("محدد عنوان العمل (titleSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mangaUrlSelector,
                        onValueChange = { mangaUrlSelector = it },
                        label = { Text("محدد رابط التفاصيل (mangaUrlSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = coverImageSelector,
                        onValueChange = { coverImageSelector = it },
                        label = { Text("محدد صورة الغلاف (coverSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detailsDescriptionSelector,
                        onValueChange = { detailsDescriptionSelector = it },
                        label = { Text("محدد القصة/الوصف (descriptionSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detailsGenresSelector,
                        onValueChange = { detailsGenresSelector = it },
                        label = { Text("محدد التصنيفات (genreSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detailsStatusSelector,
                        onValueChange = { detailsStatusSelector = it },
                        label = { Text("محدد حالة العمل (statusSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = chapterListSelector,
                        onValueChange = { chapterListSelector = it },
                        label = { Text("محدد قائمة الفصول (chapterListSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = chapterUrlSelector,
                        onValueChange = { chapterUrlSelector = it },
                        label = { Text("محدد رابط الفصل (chapterUrlSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pageImageSelector,
                        onValueChange = { pageImageSelector = it },
                        label = { Text("محدد صور صفحات القراءة (pageImageSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nextPageSelector,
                        onValueChange = { nextPageSelector = it },
                        label = { Text("محدد الصفحة التالية (nextPageSelector)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (previewItems.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "نتيجة المعاينة الحية (${previewItems.size} عمل):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                previewItems.take(4).forEach { item ->
                                    Text(
                                        text = "• ${item.title} (${if (item.coverUrl.isNullOrBlank()) "بدون غلاف" else "غلاف متاح"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onPreview(buildCurrentConfig()) },
                        enabled = !isPreviewing,
                        modifier = Modifier.testTag("preview_config_button")
                    ) {
                        if (isPreviewing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("معاينة قبل الحفظ")
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { onSave(buildCurrentConfig()) },
                            modifier = Modifier.testTag("save_config_button")
                        ) {
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSourceMetadataDialog(
    source: SourceInfo,
    onConfirm: (String, String, String, ContentRating, SourceEngineType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(source) { mutableStateOf(source.name) }
    var baseUrl by remember(source) { mutableStateOf(source.baseUrl) }
    var lang by remember(source) { mutableStateOf(source.language) }
    var rating by remember(source) { mutableStateOf(source.contentRating) }
    var engine by remember(source) { mutableStateOf(source.engineType) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تعديل بيانات المصدر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المصدر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("الرابط الأساسي (Base URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text("اللغة (ar / en)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("تصنيف المحتوى:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContentRating.entries.forEach { r ->
                        FilterChip(
                            selected = rating == r,
                            onClick = { rating = r },
                            label = { Text(r.badgeText) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name, baseUrl, lang, rating, engine) }) {
                        Text("حفظ التعديلات")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportSourceConfigDialog(
    exportedJson: String?,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var jsonInput by remember(exportedJson) { mutableStateOf(exportedJson.orEmpty()) }
    val isExportMode = !exportedJson.isNullOrBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isExportMode) "تصدير إعدادات المصدر (JSON)" else "استيراد إعدادات مصدر محفوظة (JSON)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("كود إعدادات المصدر بصيغة JSON") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("json_config_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (isExportMode) {
                                clipboard.setText(AnnotatedString(jsonInput))
                            } else {
                                val clip = clipboard.getText()?.text.orEmpty()
                                if (clip.isNotBlank()) jsonInput = clip
                            }
                        }
                    ) {
                        Text(if (isExportMode) "نسخ JSON" else "لصق من الحافظة")
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("إغلاق") }
                        if (!isExportMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onImportJson(jsonInput) },
                                enabled = jsonInput.isNotBlank(),
                                modifier = Modifier.testTag("confirm_import_json_button")
                            ) {
                                Text("استيراد وحفظ")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 11: Dedicated 12-Point Source Diagnostics & 11-Stage Test Screen (`شاشة تشخيص المصادر`).
 */
@Composable
fun SourceDiagnosticsScreen(
    sources: List<SourceInfo>,
    reports: Map<String, SourceDiagnosticReport>,
    isRunningDiagnostics: Boolean,
    onNavigateBack: () -> Unit,
    onRunSingleDiagnostic: (String) -> Unit,
    onRunAllDiagnostics: () -> Unit,
    fullTestReports: Map<String, SourceFullTestReport> = emptyMap(),
    isRunningFullTest: Boolean = false,
    onRunFullTest: (String) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("diagnostics_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تشخيص المصادر",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (sources.isNotEmpty()) {
                Button(
                    onClick = onRunAllDiagnostics,
                    enabled = !isRunningDiagnostics && !isRunningFullTest,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("run_all_diagnostics_button")
                ) {
                    if (isRunningDiagnostics || isRunningFullTest) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص الكل")
                }
            }
        }

        if (sources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مواقع مضافة حالياً. قم بإضافة موقع من «إدارة المواقع» أولاً ثم افحصه هنا.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("diagnostics_list")
            ) {
                items(sources, key = { it.id }) { src ->
                    val report = reports[src.id]
                    val fullReport = fullTestReports[src.id]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    SourceBadgeIcon(text = src.iconText, colorHex = src.iconColorHex, sizeDp = 36)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "1. اسم المصدر: ${src.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "2. النطاق: ${src.domain}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onRunSingleDiagnostic(src.id) },
                                        enabled = !isRunningDiagnostics && !isRunningFullTest,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("diagnose_source_${src.id}")
                                    ) {
                                        Text("فحص")
                                    }
                                    Button(
                                        onClick = { onRunFullTest(src.id) },
                                        enabled = !isRunningDiagnostics && !isRunningFullTest,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.testTag("full_test_source_${src.id}")
                                    ) {
                                        Text("اختبار 11 مرحلة")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            val adapterStatus = fullReport?.finalStatus ?: report?.adapterStatus ?: src.status
                            val mangaCount = fullReport?.extractedMangaCount ?: report?.extractedMangaCount ?: src.extractedMangaCount
                            val chaptersCount = fullReport?.extractedChaptersCount ?: report?.extractedChaptersCount ?: src.extractedChaptersCount
                            val pagesCount = fullReport?.extractedPagesCount ?: report?.extractedPagesCount ?: src.extractedPagesCount

                            DiagnosticRow("3. حالة الاتصال", report?.connectionStatusAr ?: if (mangaCount > 0) "متصل بنجاح" else "في انتظار الفحص")
                            DiagnosticRow("4. حالة التحقق", report?.verificationStatusAr ?: "غير مطلوب")
                            DiagnosticRow("5. حالة الموصل", "${adapterStatus.name} (${adapterStatus.labelAr})")
                            DiagnosticRow("6. نوع الموصل والتصنيف", "${src.engineType.labelAr} • ${src.contentRating.labelAr}")
                            DiagnosticRow("7. عدد الأعمال المستخرجة", "$mangaCount")
                            DiagnosticRow(
                                "8. حالة الأغلفة المحملة",
                                if (report == null && fullReport == null) "لم يُختبر بعد"
                                else if (report?.imagesStatusOk == true || (fullReport?.extractedCoversCount ?: 0) > 0) "صالحة (${fullReport?.extractedCoversCount ?: mangaCount} غلاف)"
                                else "مفقودة"
                            )
                            DiagnosticRow("9. عدد الفصول المستخرجة", "$chaptersCount فصل")
                            DiagnosticRow(
                                "10. حالة صفحات القراءة",
                                if (pagesCount > 0 || report?.pagesStatusOk == true) "متاحة ($pagesCount صفحة)" else "لم تُختبر أو فارغة"
                            )
                            DiagnosticRow("11. آخر خطأ", fullReport?.lastError ?: report?.lastError ?: src.lastError ?: "لا يوجد")
                            DiagnosticRow(
                                "12. وقت آخر فحص",
                                dateFormat.format(Date(fullReport?.testedAt ?: report?.lastTestedAt ?: src.lastCheckedAt))
                            )

                            if (fullReport != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "نتائج الاختبار الشامل (${fullReport.passedCount}/${fullReport.stages.size} مراحل ناجحة):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                fullReport.stages.forEach { st ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (st.passed) "✓" else "✗"} ${st.stageNameAr}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (st.passed) Color(0xFF81C784) else Color(0xFFEF5350),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${st.detailsAr} (${st.durationMs}ms)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dialogs for Adding a Site, Checking Compatibility (14-point inspection),
 * Manual Site Name + Content Rating Confirmation, and Cloudflare Human Verification.
 */
@Composable
fun AddSiteAndVerificationDialogs(
    state: AddSiteDialogState,
    onInputChanged: (String) -> Unit,
    onStartCheck: () -> Unit,
    onDismiss: () -> Unit,
    onManualNameChanged: (String) -> Unit,
    onConfirmManualAdd: () -> Unit,
    onCompleteCloudflareVerify: (String?, String) -> Unit,
    onFailCloudflareVerify: (String) -> Unit = {},
    onCancelCloudflareVerify: () -> Unit = onDismiss,
    onSelectContentRating: (ContentRating) -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current

    // 1. Primary "إضافة موقع" Dialog
    if (state.isVisible && !state.showManualConfirmDialog && !state.showCloudflareDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "إضافة موقع",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val clipText = clipboard.getText()?.text.orEmpty()
                                if (clipText.isNotBlank()) {
                                    onInputChanged(clipText)
                                }
                            },
                            modifier = Modifier.testTag("paste_domain_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "لصق الرابط"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = state.inputUrl,
                            onValueChange = onInputChanged,
                            label = { Text("أدخل عنوان موقع") },
                            placeholder = { Text("example.com") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("domain_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.domain_privacy_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.isChecking) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.stepMessageAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!state.errorMessageAr.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.errorMessageAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF5350)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onStartCheck,
                            enabled = !state.isChecking,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("confirm_check_domain_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة")
                        }
                    }
                }
            }
        }
    }

    // 2. Manual Site Name & 14-Point Inspection Summary Confirmation Dialog ("إضافة موقع يدوياً")
    if (state.showManualConfirmDialog) {
        val pending = state.pendingValidationResult
        val connector = pending?.matchedSource
        val inspection = pending?.inspectionReport
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "إضافة موقع يدوياً",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (connector != null) {
                        SourceBadgeIcon(
                            text = connector.iconText,
                            colorHex = connector.iconColorHex,
                            sizeDp = 64
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.manualSiteName,
                        onValueChange = onManualNameChanged,
                        label = { Text("اسم الموقع") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_site_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "تصنيف محتوى الموقع:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ContentRating.entries.forEach { rating ->
                            FilterChip(
                                selected = state.selectedContentRating == rating,
                                onClick = { onSelectContentRating(rating) },
                                label = { Text(rating.labelAr) }
                            )
                        }
                    }

                    if (pending != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "نتيجة الفحص: ${pending.status.labelAr}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = pending.messageAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (inspection != null) {
                                    Text(
                                        text = "المحرك: ${inspection.detectedArchitecture.labelAr} • أعمال: ${inspection.extractedWorksCount} • فصول: ${inspection.extractedChaptersCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirmManualAdd,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("confirm_manual_site_button")
                        ) {
                            Text("تأكيد")
                        }
                    }
                }
            }
        }
    }

    // 3. Universal Website Verification WebView Dialog
    if (state.showCloudflareDialog) {
        VerificationWebView(
            domain = state.cloudflareDomain,
            sourceName = state.pendingValidationResult?.suggestedName ?: state.cloudflareDomain,
            stepMessageAr = state.cloudflareStepAr,
            onVerificationCompleted = { verifiedHtml, pageTitle ->
                onCompleteCloudflareVerify(verifiedHtml, pageTitle)
            },
            onVerificationFailed = { reasonAr ->
                onFailCloudflareVerify(reasonAr)
            },
            onCancel = onCancelCloudflareVerify
        )
    }
}

/**
 * Secure Internal WebView Browser with Floating "إضافة موقع" Button.
 * Supports real navigation for any website or Google search result without hardcoded site intercepts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InternalBrowserScreen(
    initialUrl: String,
    onCloseBrowser: () -> Unit,
    onAddCurrentDomain: (String) -> Unit
) {
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf("المتصفح") }
    var searchInput by remember { mutableStateOf(initialUrl) }
    var isEditingUrl by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCloseBrowser,
                            modifier = Modifier.testTag("browser_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق المتصفح")
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditingUrl = !isEditingUrl }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { isEditingUrl = !isEditingUrl }) {
                            Icon(Icons.Default.Search, contentDescription = "بحث أو إدخال رابط")
                        }

                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "خيارات المتصفح")
                            }
                            DropdownMenu(
                                expanded = showOverflow,
                                onDismissRequest = { showOverflow = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("نسخ URL") },
                                    onClick = {
                                        showOverflow = false
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(currentUrl))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إضافة هذا الموقع") },
                                    onClick = {
                                        showOverflow = false
                                        onAddCurrentDomain(currentUrl)
                                    }
                                )
                            }
                        }
                    }

                    if (isEditingUrl) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = { Text("ابحث عن موقع مانهوا أو أدخل الرابط...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val q = searchInput.trim()
                                currentUrl = when {
                                    q.startsWith("https://") -> q
                                    q.startsWith("http://") -> "https://" + q.removePrefix("http://")
                                    q.contains(".") && !q.contains(" ") -> "https://$q"
                                    else -> "https://www.google.com/search?q=$q"
                                }
                                isEditingUrl = false
                            }) {
                                Text("انتقال")
                            }
                        }
                    }
                }
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(false)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url ?: return true
                                val rawUrlStr = reqUrl.toString()

                                // Unwrap Google Search redirect links (/url?q=...) so single tap opens the target site immediately
                                if (reqUrl.host?.contains("google.") == true && reqUrl.path == "/url") {
                                    val targetQ = reqUrl.getQueryParameter("q") ?: reqUrl.getQueryParameter("url")
                                    if (!targetQ.isNullOrBlank() && targetQ.startsWith("http")) {
                                        val httpsTarget = if (targetQ.startsWith("http://")) {
                                            "https://" + targetQ.removePrefix("http://")
                                        } else {
                                            targetQ
                                        }
                                        currentUrl = httpsTarget
                                        searchInput = httpsTarget
                                        view?.loadUrl(httpsTarget)
                                        return true
                                    }
                                }

                                val scheme = reqUrl.scheme?.lowercase() ?: ""
                                val host = reqUrl.host?.lowercase() ?: ""
                                if (scheme == "http" && !isInternalOrLocalAddress(host)) {
                                    val upgraded = "https://" + rawUrlStr.removePrefix("http://")
                                    currentUrl = upgraded
                                    searchInput = upgraded
                                    view?.loadUrl(upgraded)
                                    return true
                                }
                                if (scheme != "https" || isInternalOrLocalAddress(host)) {
                                    return true
                                }
                                currentUrl = rawUrlStr
                                searchInput = rawUrlStr
                                return false
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.cancel()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    searchInput = url
                                }
                                if (!view?.title.isNullOrBlank()) pageTitle = view?.title.orEmpty()
                            }
                        }
                        loadUrl(currentUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl && !currentUrl.startsWith("javascript:")) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Floating "+ إضافة موقع" Button in bottom corner
        Button(
            onClick = { onAddCurrentDomain(currentUrl) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF283552),
                contentColor = Color(0xFFD6E2FF)
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .height(48.dp)
                .testTag("browser_floating_add_site_button")
        ) {
            Icon(Icons.Default.PlaylistAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إضافة موقع", style = MaterialTheme.typography.titleSmall)
        }
    }
}
