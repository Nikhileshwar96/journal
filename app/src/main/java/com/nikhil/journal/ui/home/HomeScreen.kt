@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.nikhil.journal.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nikhil.journal.R
import com.nikhil.journal.ui.AppViewModelProvider
import com.nikhil.journal.ui.components.centeredSidePadding
import com.nikhil.journal.ui.components.preview
import com.nikhil.journal.ui.components.relativeLabel

@Composable
fun HomeScreen(
    onNewEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    BackHandler(enabled = isSearching) { viewModel.setSearching(false) }

    Scaffold(
        topBar = {
            if (isSearching) {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onClose = { viewModel.setSearching(false) },
                )
            } else {
                TopAppBar(
                    title = { Text("Daily Journal") },
                    actions = {
                        IconButton(onClick = { viewModel.setSearching(true) }) {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!isSearching) {
                ExtendedFloatingActionButton(
                    onClick = onNewEntry,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("New entry") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.groups.isEmpty() && query.isNotBlank() -> EmptyState(
                    title = "No matches",
                    message = "Nothing in your journal matches “${query.trim()}”.",
                )
                state.groups.isEmpty() -> EmptyState(
                    title = "Your journal is empty",
                    message = "Every day has a story. Tap “New entry” to write your first one.",
                )
                else -> EntryList(groups = state.groups, onOpenEntry = onOpenEntry)
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Close search")
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search entries") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
                }
            }
        },
    )
}

@Composable
private fun EntryList(groups: List<DateGroup>, onOpenEntry: (Long) -> Unit) = BoxWithConstraints {
    val side = centeredSidePadding(maxWidth)
    LazyColumn(
        contentPadding = PaddingValues(start = side, end = side, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        groups.forEach { group ->
            stickyHeader(key = "header-${group.date}") {
                Text(
                    text = group.date.relativeLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(group.entries, key = { it.id }) { entry ->
                EntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
            }
        }
    }
}

@Composable
private fun EntryCard(entry: EntryListItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val hasTitle = entry.title.isNotBlank()
                if (hasTitle) {
                    Text(
                        text = entry.title.trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val preview = entry.body.preview()
                when {
                    preview.isNotEmpty() -> Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (hasTitle) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    !hasTitle -> Text(
                        text = if (entry.thumbnail != null) "Photo entry" else "Untitled entry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry.thumbnail != null) {
                Spacer(Modifier.width(12.dp))
                AsyncImage(
                    model = entry.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
