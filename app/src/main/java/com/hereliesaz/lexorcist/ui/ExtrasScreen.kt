package com.hereliesaz.lexorcist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.lexorcist.R
import com.hereliesaz.lexorcist.common.state.SaveState
import com.hereliesaz.lexorcist.model.Script
import com.hereliesaz.lexorcist.model.SignInState
import com.hereliesaz.lexorcist.model.Template
import com.hereliesaz.lexorcist.model.UserInfo
import com.hereliesaz.lexorcist.utils.shareText
import com.hereliesaz.lexorcist.viewmodel.AddonsBrowserViewModel
import com.hereliesaz.lexorcist.viewmodel.AuthViewModel
import com.hereliesaz.lexorcist.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExtrasScreen(
    addonsBrowserViewModel: AddonsBrowserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isLoading by addonsBrowserViewModel.isLoading.collectAsState()
    val scripts by addonsBrowserViewModel.filteredScripts.collectAsState()
    val templates by addonsBrowserViewModel.filteredTemplates.collectAsState()
    val searchQuery by addonsBrowserViewModel.searchQuery.collectAsState()
    val shareState by addonsBrowserViewModel.shareOperationState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(shareState) {
        when (shareState) {
            is SaveState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.addon_shared_successfully))
                }
            }
            is SaveState.Error -> {
                val errorMessage = (shareState as SaveState.Error).message
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_sharing_addon, errorMessage),
                        withDismissAction = true)
                }
            }
            else -> {}
        }
    }


    var showDetailsDialog by remember { mutableStateOf<Any?>(null) } // Can be Script or Template

    showDetailsDialog?.let { itemToShow ->
        val dialogId: String
        val dialogName: String
        val dialogDescription: String
        val dialogContent: String
        val dialogAuthorName: String
        val dialogAuthorEmail: String
        val dialogType: String

        when (itemToShow) {
            is Script -> {
                dialogId = itemToShow.id
                dialogName = itemToShow.name
                dialogDescription = itemToShow.description
                dialogContent = itemToShow.content
                dialogAuthorName = itemToShow.authorName
                dialogAuthorEmail = itemToShow.authorEmail
                dialogType = "Script"
            }
            is Template -> {
                dialogId = itemToShow.id
                dialogName = itemToShow.name
                dialogDescription = itemToShow.description
                dialogContent = itemToShow.content
                dialogAuthorName = itemToShow.authorName
                dialogAuthorEmail = itemToShow.authorEmail
                dialogType = "Template"
            }
            else -> return@let // Should not happen
        }

        ItemDetailsDialog(
            id = dialogId,
            name = dialogName,
            description = dialogDescription,
            content = dialogContent,
            authorName = dialogAuthorName,
            authorEmail = dialogAuthorEmail,
            type = dialogType,
            onDismiss = { showDetailsDialog = null },
            onDelete = {
                showDetailsDialog = null
            },
            onEdit = {
                showDetailsDialog = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.extras).uppercase(),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AzLoad()
                }
            } else {
                val pagerState = rememberPagerState { 2 }
                val coroutineScope = rememberCoroutineScope()
                val tabTitles = listOf(stringResource(R.string.scripts), stringResource(R.string.templates))

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { addonsBrowserViewModel.onSearchQueryChanged(it) }
                )

                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> ScriptsPage(scripts, addonsBrowserViewModel, authViewModel, settingsViewModel) { script -> showDetailsDialog = script }
                        1 -> TemplatesPage(templates, addonsBrowserViewModel, authViewModel, settingsViewModel) { template -> showDetailsDialog = template }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        label = { Text(stringResource(R.string.search_addons)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                AzButton(onClick = { onQueryChange("") }, text = "X")
            }
        },
        singleLine = true
    )
}


@Composable
fun ScriptsPage(
    scripts: List<Script>,
    viewModel: AddonsBrowserViewModel,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onItemClick: (Script) -> Unit
) {
    val currentSignInState by authViewModel.signInState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    if (scripts.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results)
                else stringResource(R.string.no_scripts_available),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        items(scripts, key = { it.id }) { script ->
            AddonListItem(
                item = script,
                onItemClick = { onItemClick(script) },
                onShareClick = {
                    val (currentUserDisplayName, currentUserEmail) = getCurrentUserDetails(currentSignInState, settingsViewModel)
                    viewModel.shareAddon(
                        name = script.name,
                        description = script.description,
                        content = script.content,
                        type = "Script",
                        authorName = currentUserDisplayName ?: script.authorName,
                        authorEmail = currentUserEmail ?: script.authorEmail,
                        court = script.court
                    )
                },
                onRate = { rating -> viewModel.rateAddon(script.id, rating, "Script") }
            )
        }
    }
}

@Composable
fun TemplatesPage(
    templates: List<Template>,
    viewModel: AddonsBrowserViewModel,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    onItemClick: (Template) -> Unit
) {
    val currentSignInState by authViewModel.signInState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    if (templates.isEmpty()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results)
                else stringResource(R.string.no_templates_available),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        items(templates, key = { it.id }) { template ->
            AddonListItem(
                item = template,
                onItemClick = { onItemClick(template) },
                onShareClick = {
                     val (currentUserDisplayName, currentUserEmail) = getCurrentUserDetails(currentSignInState, settingsViewModel)
                    viewModel.shareAddon(
                        name = template.name,
                        description = template.description,
                        content = template.content,
                        type = "Template",
                        authorName = currentUserDisplayName ?: template.authorName,
                        authorEmail = currentUserEmail ?: template.authorEmail,
                        court = template.court
                    )
                },
                onRate = { rating -> viewModel.rateAddon(template.id, rating, "Template") }
            )
        }
    }
}

private fun getCurrentUserDetails(signInState: SignInState, settingsViewModel: SettingsViewModel): Pair<String?, String?> {
    val userInfo: UserInfo? = if (signInState is SignInState.Success) signInState.userInfo else null
    val authorNameFromSettings = settingsViewModel.authorName.value
    val authorEmailFromSettings = settingsViewModel.authorEmail.value

    val displayName = userInfo?.displayName?.takeIf { it.isNotBlank() } ?: authorNameFromSettings.takeIf { it.isNotBlank() }
    val email = userInfo?.email?.takeIf { it.isNotBlank() } ?: authorEmailFromSettings.takeIf { it.isNotBlank() }
    return Pair(displayName, email)
}


@Composable
fun AddonListItem(
    item: Any, // Script or Template
    onItemClick: () -> Unit,
    onShareClick: () -> Unit,
    onRate: (Int) -> Unit
) {
    val name: String
    val description: String
    val authorNameValue: String
    val authorEmailValue: String
    val ratingValue: Double

    when (item) {
        is Script -> {
            name = item.name
            description = item.description
            authorNameValue = item.authorName
            authorEmailValue = item.authorEmail
            ratingValue = item.rating
        }
        is Template -> {
            name = item.name
            description = item.description
            authorNameValue = item.authorName
            authorEmailValue = item.authorEmail
            ratingValue = item.rating
        }
        else -> return // Or handle error / show placeholder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onItemClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
            val authorDisplay = authorNameValue.ifBlank { authorEmailValue }
            if (authorDisplay.isNotBlank()) {
                Text(text = "by $authorDisplay", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            }
            Text(text = description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                RatingBar(rating = ratingValue.toFloat(), onRate = onRate)
                Spacer(modifier = Modifier.width(8.dp))
                AzButton(onClick = onShareClick, text = stringResource(R.string.share))
            }
        }
    }
}

@Composable
fun ItemDetailsDialog(
    id: String,
    name: String,
    description: String,
    content: String,
    authorName: String,
    authorEmail: String,
    type: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.End)
                val authorDisplay = authorName.ifBlank { authorEmail }
                if (authorDisplay.isNotBlank()) {
                    Text(text = "by $authorDisplay", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AzButton(
                        onClick = {
                            shareText(context, subject = "Check out this $type: $name", text = content)
                        },
                        text = stringResource(R.string.share_content),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    AzButton(onClick = onDismiss, text = stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRate: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        (1..5).forEach { index ->
            IconButton(onClick = { onRate(index) }) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Rate $index",
                    tint = if (index <= rating) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
