@file:OptIn(ExperimentalMaterial3Api::class)

package com.nikhil.journal.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nikhil.journal.R
import com.nikhil.journal.ui.AppViewModelProvider
import com.nikhil.journal.ui.components.ZoomableImageDialog
import com.nikhil.journal.ui.components.relativeLabel
import java.io.File

@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: EntryDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var fullscreenIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(state) { if (state is DetailUiState.Gone) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is DetailUiState.Loaded && !isDeleting) {
                        IconButton(onClick = { onEdit(viewModel.entryId) }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is DetailUiState.Loaded -> EntryContent(
                state = s,
                onImageClick = { fullscreenIndex = it },
                modifier = Modifier.padding(padding),
            )
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    val loaded = state as? DetailUiState.Loaded
    val index = fullscreenIndex
    if (loaded != null && index != null) {
        loaded.images.getOrNull(index)?.let { file ->
            ZoomableImageDialog(file = file, onDismiss = { fullscreenIndex = null })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete entry?") },
            text = { Text("This entry and its photos will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EntryContent(
    state: DetailUiState.Loaded,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.entry.entryDate.relativeLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (state.entry.title.isNotBlank()) {
            Text(state.entry.title.trim(), style = MaterialTheme.typography.headlineSmall)
        }
        if (state.images.isNotEmpty()) {
            ImagePager(images = state.images, onImageClick = onImageClick)
        }
        if (state.entry.body.isNotBlank()) {
            SelectionContainer {
                Text(state.entry.body, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ImagePager(images: List<File>, onImageClick: (Int) -> Unit) {
    val pagerState = rememberPagerState { images.size }
    Box {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = "Photo ${page + 1} of ${images.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onImageClick(page) },
            )
        }
        if (images.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
