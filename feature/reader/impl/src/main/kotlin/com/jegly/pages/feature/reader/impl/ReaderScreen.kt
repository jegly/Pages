package com.jegly.pages.feature.reader.impl

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jegly.pages.core.model.BookFormat
import com.jegly.pages.core.model.Highlight
import com.jegly.pages.core.model.PageTurnMode
import com.jegly.pages.core.model.PdfFillMode
import com.jegly.pages.core.model.ReadingTheme
import com.jegly.pages.core.model.SliderTimeout
import com.jegly.pages.core.rendering.PageContent
import com.jegly.pages.core.rendering.SearchResult
import com.jegly.pages.core.rendering.TocEntry
import com.jegly.pages.core.tts.TtsState
import com.jegly.pages.core.tts.VoiceModel
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookUri: String,
    startPage: Int = -1,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(
        creationCallback = { factory: ReaderViewModel.Factory ->
            factory.create(bookUri, startPage)
        },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
    val foldingFeature by rememberFoldingFeature()
    val tableOfContents by viewModel.tableOfContents.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var showBars by remember { mutableStateOf(true) }
    var showSlider by remember { mutableStateOf(true) }
    var ttsAutoPlaying by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBrightnessSheet by remember { mutableStateOf(false) }
    var showTtsSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }

    val view = LocalView.current

    // Fullscreen mode: hide system bars when bars are hidden.
    // view.context is often a ContextThemeWrapper, not Activity directly — unwrap the chain.
    LaunchedEffect(showBars) {
        var ctx: android.content.Context = view.context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) {
            ctx = (ctx as android.content.ContextWrapper).baseContext
        }
        val window = (ctx as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
        if (showBars) {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Slider auto-hides after the configured timeout; Off = never show, AlwaysOn = never hide.
    LaunchedEffect(uiState.currentPage, showBars, prefs.sliderTimeout) {
        if (!showBars || prefs.sliderTimeout == SliderTimeout.Off) {
            showSlider = false
            return@LaunchedEffect
        }
        showSlider = true
        val timeoutMs = when (prefs.sliderTimeout) {
            SliderTimeout.AlwaysOn -> return@LaunchedEffect
            SliderTimeout.Short    -> 2_000L
            SliderTimeout.Medium   -> 3_000L
            SliderTimeout.Long     -> 5_000L
            SliderTimeout.Off      -> return@LaunchedEffect // unreachable, handled above
        }
        delay(timeoutMs)
        showSlider = false
    }

    // Apply the in-app brightness override to the window; restore system brightness on exit.
    LaunchedEffect(prefs.brightness) {
        var ctx: android.content.Context = view.context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) {
            ctx = (ctx as android.content.ContextWrapper).baseContext
        }
        val window = (ctx as? android.app.Activity)?.window ?: return@LaunchedEffect
        val attrs = window.attributes
        attrs.screenBrightness = prefs.brightness  // -1f = follow system, 0..1 = override
        window.attributes = attrs
    }
    DisposableEffect(Unit) {
        onDispose {
            var ctx: android.content.Context = view.context
            while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) {
                ctx = (ctx as android.content.ContextWrapper).baseContext
            }
            val window = (ctx as? android.app.Activity)?.window ?: return@onDispose
            val attrs = window.attributes
            attrs.screenBrightness = -1f
            window.attributes = attrs
        }
    }

    // When the user manually navigates to a different page while TTS is auto-playing,
    // stop whatever is currently being spoken and speak the new page.
    LaunchedEffect(uiState.currentPage) {
        if (!ttsAutoPlaying) return@LaunchedEffect
        viewModel.stopTts()
        withTimeoutOrNull(3_000L) {
            snapshotFlow { uiState.content }
                .filter { it is PageContent.TextPage && (it as PageContent.TextPage).text.isNotBlank() }
                .first()
        }
        delay(150)
        if (ttsAutoPlaying) viewModel.speakCurrentPage()
    }

    // TTS auto-advance: when a page finishes speaking, move to the next page and speak it.
    LaunchedEffect(ttsAutoPlaying) {
        if (!ttsAutoPlaying) return@LaunchedEffect
        viewModel.ttsPageCompleted.collect {
            if (!ttsAutoPlaying) return@collect
            val nextPage = uiState.currentPage + 1
            if (nextPage >= uiState.totalPages) {
                ttsAutoPlaying = false
                return@collect
            }
            viewModel.nextPage()
            // Wait up to 3s for the new page's text content to be ready before speaking.
            withTimeoutOrNull(3_000L) {
                snapshotFlow { uiState.content }
                    .filter { it is PageContent.TextPage && (it as PageContent.TextPage).text.isNotBlank() }
                    .first()
            }
            delay(100)
            if (ttsAutoPlaying) viewModel.speakCurrentPage()
        }
    }

    // Scroll state hoisted so auto-scroll and TextPageView share the same state.
    val scrollState = rememberScrollState()
    // Set to true when navigating backwards via scroll/swipe so the scroll position
    // lands at the bottom of the previous page rather than the top.
    var goingBack by remember { mutableStateOf(false) }
    var pageAdvancedByScroll by remember { mutableStateOf(false) }
    var savedScrollForAdvance by remember { mutableIntStateOf(0) }

    // Smooth continuous scroll: when user scrolls past the first-page boundary in the
    // two-page stacked column, advance the page counter and compensate the scroll offset
    // so content appears to continue in place — no jump back to position 0.
    val scrollCurrentBitmap = (uiState.content as? PageContent.BitmapPage)?.bitmap
    val scrollHasNextBitmap = uiState.nextContent is PageContent.BitmapPage
    LaunchedEffect(scrollState, uiState.content, prefs.pageTurnMode) {
        if (prefs.pageTurnMode != PageTurnMode.Scroll) return@LaunchedEffect
        if (scrollCurrentBitmap == null || scrollCurrentBitmap.height <= 0 || !scrollHasNextBitmap) return@LaunchedEffect
        val firstPageH = scrollCurrentBitmap.height
        snapshotFlow { scrollState.value }.collect { pos ->
            if (pos > firstPageH && !pageAdvancedByScroll) {
                savedScrollForAdvance = (pos - firstPageH - 2).coerceAtLeast(0)
                pageAdvancedByScroll = true
                viewModel.nextPage()
            }
        }
    }

    // On page change: restore scroll for natural scroll advances, go to bottom when
    // navigating backwards, or jump to top for explicit navigation (TOC, go-to-page).
    LaunchedEffect(uiState.currentPage) {
        if (pageAdvancedByScroll) {
            scrollState.scrollTo(savedScrollForAdvance)
            pageAdvancedByScroll = false
        } else if (goingBack) {
            goingBack = false
            withTimeoutOrNull(300L) {
                snapshotFlow { scrollState.maxValue }.filter { it > 0 }.first()
            }
            scrollState.scrollTo(scrollState.maxValue)
        } else {
            scrollState.scrollTo(0)
        }
    }

    // Re-render the current PDF page when crop-margins is toggled so the change is visible immediately.
    LaunchedEffect(prefs.pdfCropMargins) {
        if (uiState.bookFormat == BookFormat.PDF) viewModel.reloadCurrentPage()
    }

    LaunchedEffect(prefs.autoScrollEnabled, prefs.autoScrollSpeed) {
        if (prefs.autoScrollEnabled) {
            while (true) {
                delay(50L)
                scrollState.animateScrollTo(scrollState.value + (prefs.autoScrollSpeed * 2).toInt())
            }
        }
    }

    val eInkMode = prefs.eInkMode
    val bgColor = when {
        eInkMode -> Color.White
        prefs.customBackgroundColor != null -> Color(prefs.customBackgroundColor!!.toInt())
        else -> when (prefs.readingTheme) {
            ReadingTheme.Sepia -> Color(0xFFFAF0DC)
            ReadingTheme.Dark -> Color(0xFF1A1A1A)
            ReadingTheme.Night -> Color.Black
            ReadingTheme.Green -> Color(0xFFD4EDDA)
            ReadingTheme.Light -> Color.White
            else -> MaterialTheme.colorScheme.background
        }
    }
    val textColor = when {
        eInkMode -> Color.Black
        prefs.readingTheme == ReadingTheme.Dark -> Color(0xFFDDDDDD)
        prefs.readingTheme == ReadingTheme.Night -> Color.White
        else -> Color.Unspecified
    }
    // Blue light filter is suppressed in e-ink mode (grayscale display can't use it)
    val blueLightColor = if (prefs.blueLightFilter && !eInkMode) Color(0x33FF8C00) else Color.Transparent
    // In e-ink mode animations are replaced with instant transitions to avoid ghosting
    val barEnter = if (eInkMode) EnterTransition.None else fadeIn()
    val barExit = if (eInkMode) ExitTransition.None else fadeOut()

    val isTabletop = foldingFeature.isTableTop()
    val isDualPage = foldingFeature.isDualPage()
    val density = LocalDensity.current
    val hingeHeightDp = remember(foldingFeature) {
        foldingFeature?.bounds?.let { with(density) { it.height().toDp() } } ?: 0.dp
    }
    val hingeWidthDp = remember(foldingFeature) {
        foldingFeature?.bounds?.let { with(density) { it.width().toDp() } } ?: 8.dp
    }

    // Track zoom state at the screen level so it survives bitmap re-renders (theme/crop toggles)
    // and so we can disable page-turn gestures while zoomed in.
    var pdfScale by remember(uiState.currentPage) { mutableFloatStateOf(1f) }
    var pdfOffset by remember(uiState.currentPage) { mutableStateOf(Offset.Zero) }
    val isZoomed = pdfScale > 1.01f

    // Gesture handling — two modes:
    // Scroll mode: Initial pass so we see events before inner verticalScroll. Intercept horizontal
    //   drags (page turn) and taps (toggle bars). Vertical drags are NOT consumed here so the inner
    //   verticalScroll handles them natively.
    // Swipe mode: Main pass, handle taps and horizontal drags. Inner scroll is disabled, so there
    //   is no conflict with vertical events.
    val pageTurnMode = prefs.pageTurnMode
    val gestureModifier = Modifier.pointerInput(pageTurnMode, isZoomed) {
        if (pageTurnMode == PageTurnMode.Scroll) {
            // Scroll mode: tap toggles bars; overscroll at top/bottom advances pages.
            // Horizontal swipes are intentionally ignored — page turns only via overscroll.
            // While zoomed, skip page-turn logic so the user can pan freely.
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                val downY = down.position.y
                var isDrag = false
                var edgeScrollDelta = 0f
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    // If a second finger lands, this is a pinch — bail out and let the
                    // transformable gesture handler take over.
                    if (event.changes.count { it.pressed } >= 2) break
                    if (!change.pressed) {
                        if (!isDrag) showBars = !showBars
                        break
                    }
                    if (isZoomed) continue

                    val dy = abs(change.position.y - downY)
                    if (!isDrag && dy > viewConfiguration.touchSlop) isDrag = true
                    if (isDrag) {
                        val atBottom = scrollState.value >= scrollState.maxValue
                        val atTop = scrollState.value == 0
                        // fingerDy < 0 = finger moved up = scrolling down into content
                        // fingerDy > 0 = finger moved down = scrolling up to previous content
                        val fingerDy = change.position.y - change.previousPosition.y
                        if (atBottom && fingerDy < 0) {
                            edgeScrollDelta -= fingerDy
                            change.consume()
                            if (edgeScrollDelta > 80f) {
                                viewModel.nextPage()
                                break
                            }
                        } else if (atTop && fingerDy > 0) {
                            edgeScrollDelta += fingerDy
                            change.consume()
                            if (edgeScrollDelta > 80f) {
                                goingBack = true
                                viewModel.prevPage()
                                break
                            }
                        } else {
                            edgeScrollDelta = 0f // reset if user scrolls back mid-overscroll
                        }
                    }
                }
            }
        } else {
            awaitEachGesture {
                // requireUnconsumed = true so that touches claimed by the slider or other
                // foreground controls don't also fire a page-turn swipe.
                val down = awaitFirstDown(requireUnconsumed = true)
                val downX = down.position.x
                val downY = down.position.y
                var lastX = downX
                var isDrag = false
                var isHorizontal = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    // If a second finger lands, this is a pinch — bail out and let the
                    // transformable gesture handler take over.
                    if (event.changes.count { it.pressed } >= 2) break
                    if (!change.pressed) {
                        if (!isDrag) showBars = !showBars
                        break
                    }
                    if (isZoomed) continue

                    val currentX = change.position.x
                    val currentY = change.position.y
                    val dx = abs(currentX - downX)
                    val dy = abs(currentY - downY)

                    if (!isDrag && (dx > viewConfiguration.touchSlop || dy > viewConfiguration.touchSlop)) {
                        isDrag = true
                        isHorizontal = dx > dy
                    }

                    if (isHorizontal) {
                        change.consume()
                        val delta = currentX - downX
                        if (delta < -60f) { viewModel.nextPage(); break }
                        else if (delta > 60f) { viewModel.prevPage(); break }
                    }

                    lastX = currentX
                }
            }
        }
    }

    val isTextFormat = uiState.bookFormat == BookFormat.EPUB || uiState.bookFormat == BookFormat.TXT || uiState.bookFormat == BookFormat.MOBI

    // Box-based layout: bars are overlaid, so showing/hiding them never shifts the content.
    // gestureModifier lives on the inner content Box only — AppBar taps must not toggle showBars.
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor).onSizeChanged { size ->
            if (size.width > 0) viewModel.setRenderWidth(size.width)
        }.then(gestureModifier)) {
            when {
                isTabletop -> TableTopLayout(
                    content = uiState.content,
                    nextContent = uiState.nextContent,
                    prefs = prefs,
                    eInkMode = eInkMode,
                    scrollState = scrollState,
                    ttsState = ttsState,
                    bgColor = bgColor,
                    textColor = textColor,
                    blueLightColor = blueLightColor,
                    hingeHeightDp = hingeHeightDp,
                    showBars = showBars,
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    highlights = uiState.highlights,
                    onPrevPage = viewModel::prevPage,
                    onNextPage = viewModel::nextPage,
                    onAddHighlight = viewModel::addHighlight,
                    pdfFillMode = prefs.pdfFillMode,
                    pdfScale = pdfScale,
                    pdfOffset = pdfOffset,
                    onPdfScaleChange = { pdfScale = it },
                    onPdfOffsetChange = { pdfOffset = it },
                    modifier = Modifier.fillMaxSize(),
                )

                isDualPage -> DualPageLayout(
                    currentContent = uiState.content,
                    nextContent = uiState.nextContent,
                    prefs = prefs,
                    eInkMode = eInkMode,
                    scrollState = scrollState,
                    ttsState = ttsState,
                    bgColor = bgColor,
                    textColor = textColor,
                    blueLightColor = blueLightColor,
                    hingeWidthDp = hingeWidthDp,
                    showBars = showBars,
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    highlights = uiState.highlights,
                    onAddHighlight = viewModel::addHighlight,
                    pdfFillMode = prefs.pdfFillMode,
                    pdfScale = pdfScale,
                    pdfOffset = pdfOffset,
                    onPdfScaleChange = { pdfScale = it },
                    onPdfOffsetChange = { pdfOffset = it },
                    modifier = Modifier.fillMaxSize().background(bgColor),
                )

                else -> SinglePageLayout(
                    content = uiState.content,
                    nextContent = uiState.nextContent,
                    prefs = prefs,
                    eInkMode = eInkMode,
                    scrollState = scrollState,
                    ttsState = ttsState,
                    bgColor = bgColor,
                    textColor = textColor,
                    blueLightColor = blueLightColor,
                    showBars = showBars,
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    highlights = uiState.highlights,
                    onAddHighlight = viewModel::addHighlight,
                    pdfFillMode = prefs.pdfFillMode,
                    pdfScale = pdfScale,
                    pdfOffset = pdfOffset,
                    onPdfScaleChange = { pdfScale = it },
                    onPdfOffsetChange = { pdfOffset = it },
                    modifier = Modifier.fillMaxSize().background(bgColor),
                )
            }
        }

        // Top bar overlaid — show/hide via AnimatedVisibility doesn't move the content below.
        AnimatedVisibility(
            visible = showBars,
            enter = barEnter,
            exit = barExit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            TopAppBar(
                title = {
                    Text(uiState.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            imageVector = if (uiState.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark",
                        )
                    }
                    // TOC button — only visible for EPUB when TOC is available
                    if (tableOfContents.isNotEmpty()) {
                        IconButton(onClick = { showTocSheet = true }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.List,
                                contentDescription = "Table of contents",
                            )
                        }
                    }
                    // Search button — only for text-based formats
                    if (isTextFormat) {
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search in book")
                        }
                    }
                    IconButton(onClick = { showBrightnessSheet = true }) {
                        Icon(Icons.Rounded.Brightness6, contentDescription = "Brightness")
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Rounded.FormatSize, contentDescription = "Reading settings")
                    }
                    IconButton(onClick = { showTtsSheet = true }) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = "Text to speech")
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (prefs.pageTurnMode == PageTurnMode.Swipe) "Switch to scroll" else "Switch to swipe") },
                                onClick = {
                                    viewModel.togglePageTurnMode()
                                    overflowExpanded = false
                                },
                            )
                            if (uiState.bookFormat == BookFormat.PDF) {
                                val nextMode = when (prefs.pdfFillMode) {
                                    PdfFillMode.FitPage   -> PdfFillMode.FillWidth
                                    PdfFillMode.FillWidth -> PdfFillMode.FillScreen
                                    PdfFillMode.FillScreen -> PdfFillMode.FitPage
                                }
                                val modeLabel = when (prefs.pdfFillMode) {
                                    PdfFillMode.FitPage   -> "PDF view: Fit page (tap to fill width)"
                                    PdfFillMode.FillWidth -> "PDF view: Fill width (tap to fill screen)"
                                    PdfFillMode.FillScreen -> "PDF view: Fill screen (tap to fit page)"
                                }
                                DropdownMenuItem(
                                    text = { Text(modeLabel) },
                                    onClick = {
                                        viewModel.setPdfFillMode(nextMode)
                                        overflowExpanded = false
                                    },
                                )
                            }
                            if (isTextFormat) {
                                val textSizePresets = listOf(0.8f to "Small", 1.0f to "Normal", 1.4f to "Large", 1.8f to "X-Large")
                                val currentLabel = textSizePresets.minByOrNull { kotlin.math.abs(it.first - prefs.fontScaleFactor) }?.second ?: "Normal"
                                val nextPreset = textSizePresets.let { presets ->
                                    val idx = presets.indexOfFirst { kotlin.math.abs(it.first - prefs.fontScaleFactor) < 0.05f }
                                    presets[((idx + 1) % presets.size).coerceAtLeast(0)]
                                }
                                DropdownMenuItem(
                                    text = { Text("Text size: $currentLabel (tap for ${nextPreset.second})") },
                                    onClick = {
                                        viewModel.setFontScale(nextPreset.first)
                                        overflowExpanded = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (prefs.autoScrollEnabled) "Stop auto-scroll" else "Start auto-scroll") },
                                onClick = {
                                    viewModel.toggleAutoScroll()
                                    overflowExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Go to page…") },
                                onClick = {
                                    showGoToPageDialog = true
                                    overflowExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (showBars) "Full screen" else "Exit full screen") },
                                onClick = {
                                    showBars = !showBars
                                    overflowExpanded = false
                                },
                            )
                        }
                    }
                },
            )
        }

        // Bottom progress bar overlaid — tabletop has its own progress inside its controls area.
        if (!isTabletop) {
            AnimatedVisibility(
                visible = showBars && showSlider && uiState.totalPages > 0,
                enter = barEnter,
                exit = barExit,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Column {
                    if (prefs.showPageNumber) {
                        Text(
                            text = "${uiState.currentPage + 1} / ${uiState.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 2.dp),
                        )
                    }
                    Slider(
                        value = if (uiState.totalPages > 1) uiState.currentPage.toFloat() else 0f,
                        onValueChange = { viewModel.goToPage(it.toInt()) },
                        valueRange = 0f..(if (uiState.totalPages > 1) (uiState.totalPages - 1).toFloat() else 1f),
                        steps = (uiState.totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp),
                                    ),
                            )
                        },
                    )
                }
            }
        }


        // On-screen prev / next navigation buttons — shown when the user enables them in settings.
        // Always visible (not tied to showBars) so they remain usable in full-screen mode.
        if (prefs.showNavigationButtons && !isTabletop && uiState.totalPages > 0) {
            Surface(
                onClick = { viewModel.prevPage() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .graphicsLayer { alpha = 0.75f },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp,
                enabled = uiState.currentPage > 0,
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous page",
                    modifier = Modifier
                        .padding(vertical = 20.dp, horizontal = 4.dp)
                        .size(28.dp),
                    tint = if (uiState.currentPage > 0)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
            Surface(
                onClick = { viewModel.nextPage() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .graphicsLayer { alpha = 0.75f },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp,
                enabled = uiState.currentPage < uiState.totalPages - 1,
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "Next page",
                    modifier = Modifier
                        .padding(vertical = 20.dp, horizontal = 4.dp)
                        .size(28.dp),
                    tint = if (uiState.currentPage < uiState.totalPages - 1)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
        }
    }

    // Go to page dialog
    if (showGoToPageDialog && uiState.totalPages > 0) {
        GoToPageDialog(
            currentPage = uiState.currentPage + 1,
            totalPages = uiState.totalPages,
            onConfirm = { page ->
                viewModel.goToPage(page - 1)
                showGoToPageDialog = false
            },
            onDismiss = { showGoToPageDialog = false },
        )
    }

    // TOC bottom sheet
    if (showTocSheet && tableOfContents.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            TocSheet(
                entries = tableOfContents,
                onEntryClick = { entry ->
                    viewModel.goToPage(entry.pageIndex)
                    showTocSheet = false
                },
            )
        }
    }

    // Search bottom sheet
    if (showSearchSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            SearchSheet(
                results = searchResults,
                onSearch = { query -> viewModel.searchInBook(query) },
                onResultClick = { result ->
                    viewModel.goToPage(result.pageIndex)
                    showSearchSheet = false
                },
            )
        }
    }

    // Reading settings bottom sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            ReadingSettingsSheet(
                prefs = prefs,
                onFontScaleChange = viewModel::setFontScale,
                onLineSpacingChange = viewModel::setLineSpacing,
                onMarginChange = viewModel::setHorizontalMargin,
                onFontFamilyChange = viewModel::setFontFamily,
                onThemeChange = viewModel::setReadingTheme,
                onPageTurnModeChange = viewModel::setPageTurnMode,
            )
        }
    }

    // Brightness bottom sheet
    if (showBrightnessSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showBrightnessSheet = false },
            sheetState = sheetState,
        ) {
            BrightnessSheet(
                brightness = prefs.brightness,
                blueLightFilter = prefs.blueLightFilter,
                onBrightnessChange = viewModel::setBrightness,
                onBlueLightFilterChange = viewModel::setBlueLightFilter,
            )
        }
    }

    // TTS bottom sheet
    if (showTtsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTtsSheet = false },
            sheetState = sheetState,
        ) {
            TtsSheet(
                ttsState = ttsState,
                speed = prefs.ttsSpeed,
                pitch = prefs.ttsPitch,
                availableVoices = viewModel.getTtsAvailableVoices(),
                onEngineChange = viewModel::setTtsEngine,
                onVoiceSelect = viewModel::setTtsVoice,
                onSpeedChange = viewModel::setTtsSpeed,
                onPitchChange = viewModel::setTtsPitch,
                onSpeak = {
                    ttsAutoPlaying = true
                    viewModel.speakCurrentPage()
                },
                onPause = viewModel::pauseTts,
                onResume = viewModel::resumeTts,
                onStop = {
                    ttsAutoPlaying = false
                    viewModel.stopTts()
                },
                onClearError = viewModel::clearTtsError,
            )
        }
    }
}

@Composable
private fun GoToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderPage by remember { mutableFloatStateOf(currentPage.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Page ${sliderPage.toInt()} of $totalPages",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = sliderPage,
                    onValueChange = { sliderPage = it },
                    valueRange = 1f..totalPages.toFloat(),
                    steps = (totalPages - 2).coerceAtLeast(0),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderPage.toInt()) }) { Text("Go") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TocSheet(
    entries: List<TocEntry>,
    onEntryClick: (TocEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Table of Contents",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.title) },
                    supportingContent = { Text("Page ${entry.pageIndex + 1}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick(entry) },
                )
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun SearchSheet(
    results: List<SearchResult>,
    onSearch: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Search in book",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Enter search term") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = { onSearch(query) }) {
                Text("Search")
            }
        }
        if (results.isEmpty() && query.isNotBlank()) {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(results) { result ->
                ListItem(
                    headlineContent = { Text("Page ${result.pageIndex + 1}") },
                    supportingContent = { Text("…${result.snippet}…", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(result) },
                )
            }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun HighlightColorPickerDialog(
    onColorPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = listOf(
        0xFFFFFF00L to "Yellow",
        0xFF90EE90L to "Green",
        0xFFADD8E6L to "Blue",
        0xFFFFB6C1L to "Pink",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highlight color") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { (colorArgb, label) ->
                    Surface(
                        onClick = { onColorPicked(colorArgb) },
                        shape = MaterialTheme.shapes.small,
                        color = Color(colorArgb.toInt()),
                        modifier = Modifier.size(40.dp),
                        tonalElevation = 2.dp,
                    ) {}
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SinglePageLayout(
    content: PageContent,
    nextContent: PageContent?,
    prefs: com.jegly.pages.core.model.UserPreferences,
    eInkMode: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    ttsState: TtsState,
    bgColor: Color,
    textColor: Color,
    blueLightColor: Color,
    showBars: Boolean,
    currentPage: Int,
    totalPages: Int,
    highlights: List<Highlight>,
    onAddHighlight: (Int, Int, String, Long) -> Unit,
    pdfFillMode: PdfFillMode = PdfFillMode.FitPage,
    pdfScale: Float = 1f,
    pdfOffset: Offset = Offset.Zero,
    onPdfScaleChange: (Float) -> Unit = {},
    onPdfOffsetChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PageContentView(
            content = content,
            nextContent = nextContent,
            prefs = prefs,
            eInkMode = eInkMode,
            scrollState = scrollState,
            ttsState = ttsState,
            textColor = textColor,
            highlights = highlights,
            onAddHighlight = onAddHighlight,
            pdfFillMode = pdfFillMode,
            pdfScale = pdfScale,
            pdfOffset = pdfOffset,
            onPdfScaleChange = onPdfScaleChange,
            onPdfOffsetChange = onPdfOffsetChange,
            modifier = Modifier.fillMaxSize(),
        )
        if (prefs.blueLightFilter) {
            Box(modifier = Modifier.fillMaxSize().background(blueLightColor))
        }
    }
}

@Composable
private fun DualPageLayout(
    currentContent: PageContent,
    nextContent: PageContent?,
    prefs: com.jegly.pages.core.model.UserPreferences,
    eInkMode: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    ttsState: TtsState,
    bgColor: Color,
    textColor: Color,
    blueLightColor: Color,
    hingeWidthDp: androidx.compose.ui.unit.Dp,
    showBars: Boolean,
    currentPage: Int,
    totalPages: Int,
    highlights: List<Highlight>,
    onAddHighlight: (Int, Int, String, Long) -> Unit,
    pdfFillMode: PdfFillMode = PdfFillMode.FitPage,
    pdfScale: Float = 1f,
    pdfOffset: Offset = Offset.Zero,
    onPdfScaleChange: (Float) -> Unit = {},
    onPdfOffsetChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left page (current)
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                PageContentView(
                    content = currentContent,
                    prefs = prefs,
                    eInkMode = eInkMode,
                    scrollState = scrollState,
                    ttsState = ttsState,
                    textColor = textColor,
                    highlights = highlights,
                    onAddHighlight = onAddHighlight,
                    pdfFillMode = pdfFillMode,
                    pdfScale = pdfScale,
                    pdfOffset = pdfOffset,
                    onPdfScaleChange = onPdfScaleChange,
                    onPdfOffsetChange = onPdfOffsetChange,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Hinge gap — keeps text away from the crease
            if (hingeWidthDp > 0.dp) {
                Spacer(modifier = Modifier.width(hingeWidthDp).fillMaxHeight().background(Color.Black))
            }

            // Right page (next)
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (nextContent != null) {
                    PageContentView(
                        content = nextContent,
                        prefs = prefs,
                        eInkMode = eInkMode,
                        scrollState = rememberScrollState(),
                        ttsState = TtsState(),
                        textColor = textColor,
                        highlights = emptyList(),
                        onAddHighlight = { _, _, _, _ -> },
                        pdfFillMode = pdfFillMode,
                        pdfScale = 1f,
                        pdfOffset = Offset.Zero,
                        onPdfScaleChange = {},
                        onPdfOffsetChange = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("End of book", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (prefs.blueLightFilter) {
            Box(modifier = Modifier.fillMaxSize().background(blueLightColor))
        }
    }
}

@Composable
private fun TableTopLayout(
    content: PageContent,
    nextContent: PageContent?,
    prefs: com.jegly.pages.core.model.UserPreferences,
    eInkMode: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    ttsState: TtsState,
    bgColor: Color,
    textColor: Color,
    blueLightColor: Color,
    hingeHeightDp: androidx.compose.ui.unit.Dp,
    showBars: Boolean,
    currentPage: Int,
    totalPages: Int,
    highlights: List<Highlight>,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddHighlight: (Int, Int, String, Long) -> Unit,
    pdfFillMode: PdfFillMode = PdfFillMode.FitPage,
    pdfScale: Float = 1f,
    pdfOffset: Offset = Offset.Zero,
    onPdfScaleChange: (Float) -> Unit = {},
    onPdfOffsetChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(bgColor)) {
        // Top half — reading content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PageContentView(
                content = content,
                nextContent = nextContent,
                prefs = prefs,
                eInkMode = eInkMode,
                scrollState = scrollState,
                ttsState = ttsState,
                textColor = textColor,
                highlights = highlights,
                onAddHighlight = onAddHighlight,
                pdfFillMode = pdfFillMode,
                pdfScale = pdfScale,
                pdfOffset = pdfOffset,
                onPdfScaleChange = onPdfScaleChange,
                onPdfOffsetChange = onPdfOffsetChange,
                modifier = Modifier.fillMaxSize(),
            )
            if (prefs.blueLightFilter) {
                Box(modifier = Modifier.fillMaxSize().background(blueLightColor))
            }
            if (totalPages > 0) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                )
            }
        }

        // Hinge separator
        if (hingeHeightDp > 0.dp) {
            Spacer(modifier = Modifier.height(hingeHeightDp).fillMaxWidth().background(Color.Black))
        }

        // Bottom half — navigation controls (always visible in tabletop)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.FilledTonalButton(onClick = onPrevPage) {
                Text("← Previous")
            }
            if (totalPages > 0) {
                LinearProgressIndicator(
                    progress = { if (totalPages > 0) (currentPage + 1f) / totalPages else 0f },
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
            }
            androidx.compose.material3.FilledTonalButton(onClick = onNextPage) {
                Text("Next →")
            }
        }
    }
}

@Composable
private fun PageContentView(
    content: PageContent,
    nextContent: PageContent? = null,
    prefs: com.jegly.pages.core.model.UserPreferences,
    eInkMode: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    ttsState: TtsState,
    textColor: Color,
    highlights: List<Highlight>,
    onAddHighlight: (Int, Int, String, Long) -> Unit,
    pdfFillMode: PdfFillMode = PdfFillMode.FitPage,
    pdfScale: Float = 1f,
    pdfOffset: Offset = Offset.Zero,
    onPdfScaleChange: (Float) -> Unit = {},
    onPdfOffsetChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (content) {
        is PageContent.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is PageContent.Error -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(content.message, modifier = Modifier.padding(16.dp))
        }
        is PageContent.BitmapPage -> BitmapPageView(
            bitmap = content.bitmap,
            nextBitmap = (nextContent as? PageContent.BitmapPage)?.bitmap,
            highContrast = prefs.pdfHighContrast || eInkMode,
            scrollMode = prefs.pageTurnMode == PageTurnMode.Scroll,
            fillMode = if (prefs.pageTurnMode == PageTurnMode.Scroll) PdfFillMode.FillWidth else pdfFillMode,
            scrollState = scrollState,
            scale = pdfScale,
            offset = pdfOffset,
            onScaleChange = onPdfScaleChange,
            onOffsetChange = onPdfOffsetChange,
            modifier = modifier,
        )
        is PageContent.TextPage -> TextPageView(
            text = content.text,
            fontScale = prefs.fontScaleFactor,
            lineSpacing = prefs.lineSpacingMultiplier,
            horizontalMarginDp = prefs.horizontalMarginDp,
            fontFamily = prefs.fontFamily.toComposeFamily(),
            textColor = textColor,
            scrollMode = prefs.pageTurnMode == PageTurnMode.Scroll,
            scrollState = scrollState,
            highlights = highlights,
            onAddHighlight = onAddHighlight,
            ttsState = ttsState,
            modifier = modifier,
        )
    }
}

@Composable
private fun BitmapPageView(
    bitmap: Bitmap,
    highContrast: Boolean,
    scrollMode: Boolean,
    fillMode: PdfFillMode = PdfFillMode.FitPage,
    scrollState: androidx.compose.foundation.ScrollState,
    scale: Float,
    offset: Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    nextBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
) {
    val colorFilter = if (highContrast) {
        val matrix = ColorMatrix().apply { setToSaturation(0f) }
        ColorFilter.colorMatrix(matrix)
    } else null

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Pre-compute the displayed image size so zoom bounds are accurate.
    val renderedWidth: Float
    val renderedHeight: Float
    if (containerSize != IntSize.Zero && bitmap.width > 0 && bitmap.height > 0) {
        when (fillMode) {
            PdfFillMode.FitPage -> {
                val bitmapAspect = bitmap.height.toFloat() / bitmap.width
                val containerAspect = containerSize.height.toFloat() / containerSize.width.coerceAtLeast(1)
                if (bitmapAspect > containerAspect) {
                    renderedHeight = containerSize.height.toFloat()
                    renderedWidth = renderedHeight / bitmapAspect
                } else {
                    renderedWidth = containerSize.width.toFloat()
                    renderedHeight = renderedWidth * bitmapAspect
                }
            }
            PdfFillMode.FillWidth, PdfFillMode.FillScreen -> {
                renderedWidth = containerSize.width.toFloat()
                renderedHeight = renderedWidth * bitmap.height.toFloat() / bitmap.width
            }
        }
    } else {
        renderedWidth = 0f
        renderedHeight = 0f
    }

    // Single transformable state used in both scroll and swipe modes.
    // Pinch-to-zoom and two-finger pan both work; single-finger drags pass through to
    // verticalScroll (in scroll mode) or to the outer page-turn gesture (in swipe mode).
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        val maxOffsetX = (renderedWidth * (newScale - 1f)) / 2f
        val maxOffsetY = (renderedHeight * (newScale - 1f)) / 2f
        onScaleChange(newScale)
        onOffsetChange(
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY),
            )
        )
    }

    // Double-tap resets zoom — works in both modes.
    val doubleTapModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val firstUp = awaitPointerEvent()
            if (firstUp.changes.all { !it.pressed }) {
                val secondDown = withTimeoutOrNull(300L) { awaitFirstDown(requireUnconsumed = false) }
                if (secondDown != null) {
                    onScaleChange(1f)
                    onOffsetChange(Offset.Zero)
                }
            }
        }
    }

    if (scrollMode) {
        // Scroll mode: pages stack vertically at full screen width. Two pages are rendered
        // (current + next) so there is always content below to scroll into, even when a single
        // PDF page is shorter than the screen. The page-advance logic in ReaderScreen tracks
        // the first page's pixel height and advances the page counter once the user scrolls
        // past it, keeping the experience seamless.
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .transformable(state = transformableState, lockRotationOnZoomPan = true)
                .then(doubleTapModifier)
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (bitmap.width > 0 && bitmap.height > 0) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            },
                    )
                }
                if (nextBitmap != null && nextBitmap.width > 0 && nextBitmap.height > 0) {
                    Image(
                        bitmap = nextBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(nextBitmap.width.toFloat() / nextBitmap.height.toFloat()),
                    )
                }
            }
        }
    } else {
        // Swipe mode with pinch-to-zoom and double-tap reset.
        // fillMode controls how the bitmap is scaled within the screen:
        //   FitPage   — whole page always visible (may have gaps if aspect ratios differ)
        //   FillWidth — fills screen width, top-aligned (gap at bottom only)
        //   FillScreen — scales to fill entire screen, may crop content on short edges
        val boxAlignment = if (fillMode == PdfFillMode.FitPage) Alignment.Center else Alignment.TopCenter
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .transformable(state = transformableState)
                .then(doubleTapModifier),
            contentAlignment = boxAlignment,
        ) {
            if (bitmap.width > 0 && bitmap.height > 0) {
                when (fillMode) {
                    PdfFillMode.FitPage -> Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            },
                    )
                    PdfFillMode.FillWidth -> Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            },
                    )
                    PdfFillMode.FillScreen -> Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                translationX = offset.x; translationY = offset.y
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun TextPageView(
    text: String,
    fontScale: Float,
    lineSpacing: Float,
    horizontalMarginDp: Int,
    fontFamily: FontFamily,
    textColor: Color,
    scrollMode: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    highlights: List<Highlight>,
    onAddHighlight: (Int, Int, String, Long) -> Unit,
    ttsState: TtsState,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    var pendingHighlight by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }

    val originalToolbar = LocalTextToolbar.current
    val customToolbar = remember(originalToolbar) {
        object : TextToolbar {
            override val status: TextToolbarStatus get() = originalToolbar.status
            override fun hide() = originalToolbar.hide()
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) {
                originalToolbar.showMenu(
                    rect,
                    onCopyRequested = {
                        onCopyRequested?.invoke()
                        // SelectionContainer copies synchronously — clipboard is set now
                        val selected = clipboardManager.getText()?.text
                        if (!selected.isNullOrEmpty()) {
                            val start = text.indexOf(selected).takeIf { it >= 0 }
                            if (start != null) {
                                pendingHighlight = Triple(start, start + selected.length, selected)
                                showColorPicker = true
                            }
                        }
                    },
                    onPasteRequested = onPasteRequested,
                    onCutRequested = onCutRequested,
                    onSelectAllRequested = onSelectAllRequested,
                )
            }
        }
    }

    // Compute sentence highlight range — only apply when valid offsets are within text bounds
    val ttsSentenceStart = ttsState.sentenceStart
    val ttsSentenceEnd = ttsState.sentenceEnd
    val hasSentenceHighlight = ttsState.isActive &&
        ttsSentenceStart >= 0 &&
        ttsSentenceEnd > ttsSentenceStart &&
        ttsSentenceEnd <= text.length

    val annotatedText = remember(text, highlights, ttsSentenceStart, ttsSentenceEnd, hasSentenceHighlight) {
        buildAnnotatedString {
            append(text)
            highlights.forEach { h ->
                val start = h.startOffset.coerceIn(0, text.length)
                val end = h.endOffset.coerceIn(start, text.length)
                if (start < end) {
                    addStyle(
                        SpanStyle(background = Color(h.colorArgb.toInt()).copy(alpha = 0.4f)),
                        start,
                        end,
                    )
                }
            }
            if (hasSentenceHighlight) {
                addStyle(
                    SpanStyle(background = Color(0xFF90CAF9.toInt()).copy(alpha = 0.5f)),
                    ttsSentenceStart,
                    ttsSentenceEnd,
                )
            }
        }
    }

    if (showColorPicker) {
        HighlightColorPickerDialog(
            onColorPicked = { colorArgb ->
                pendingHighlight?.let { (start, end, selectedText) ->
                    onAddHighlight(start, end, selectedText, colorArgb)
                }
                showColorPicker = false
                pendingHighlight = null
            },
            onDismiss = {
                showColorPicker = false
                pendingHighlight = null
            },
        )
    }

    CompositionLocalProvider(LocalTextToolbar provides customToolbar) {
        Column(
            modifier = modifier
                .then(if (scrollMode) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = horizontalMarginDp.dp, vertical = 16.dp),
        ) {
            SelectionContainer {
                BasicText(
                    text = annotatedText,
                    style = TextStyle(
                        fontSize = (16 * fontScale).sp,
                        lineHeight = (16 * fontScale * lineSpacing).sp,
                        fontFamily = fontFamily,
                        color = textColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ReadingSettingsSheet(
    prefs: com.jegly.pages.core.model.UserPreferences,
    onFontScaleChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onMarginChange: (Int) -> Unit,
    onFontFamilyChange: (com.jegly.pages.core.model.FontFamily) -> Unit,
    onThemeChange: (ReadingTheme) -> Unit,
    onPageTurnModeChange: (PageTurnMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Reading settings", style = MaterialTheme.typography.titleMedium)

        Text("View mode", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PageTurnMode.Swipe to "Swipe pages", PageTurnMode.Scroll to "Scroll").forEach { (mode, label) ->
                val selected = prefs.pageTurnMode == mode
                Surface(
                    onClick = { onPageTurnModeChange(mode) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text("Font size", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = prefs.fontScaleFactor,
                onValueChange = onFontScaleChange,
                valueRange = 0.7f..2.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("A", style = MaterialTheme.typography.titleLarge)
        }

        Text("Line spacing", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = prefs.lineSpacingMultiplier,
            onValueChange = onLineSpacingChange,
            valueRange = 1.0f..2.5f,
        )

        Text("Margins", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = prefs.horizontalMarginDp.toFloat(),
            onValueChange = { onMarginChange(it.toInt()) },
            valueRange = 4f..48f,
            steps = 10,
        )

        Text("Font", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.jegly.pages.core.model.FontFamily.entries.forEach { family ->
                val selected = prefs.fontFamily == family
                Surface(
                    onClick = { onFontFamilyChange(family) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                ) {
                    Text(
                        text = family.name,
                        fontFamily = family.toComposeFamily(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text("Background", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ReadingTheme.Light to Color.White,
                ReadingTheme.Sepia to Color(0xFFFAF0DC),
                ReadingTheme.Green to Color(0xFFD4EDDA),
                ReadingTheme.Dark to Color(0xFF1A1A1A),
                ReadingTheme.Night to Color.Black,
            ).forEach { (theme, color) ->
                val selected = prefs.readingTheme == theme
                Surface(
                    onClick = { onThemeChange(theme) },
                    color = color,
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {}
            }
        }
    }
}

@Composable
private fun BrightnessSheet(
    brightness: Float,
    blueLightFilter: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onBlueLightFilterChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Display", style = MaterialTheme.typography.titleMedium)
        Text("Brightness", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = if (brightness < 0) 0.5f else brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..1f,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Blue light filter", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = blueLightFilter, onCheckedChange = onBlueLightFilterChange)
        }
    }
}

@Composable
private fun TtsSheet(
    ttsState: TtsState,
    speed: Float,
    pitch: Float,
    availableVoices: List<VoiceModel>,
    onEngineChange: (String) -> Unit,
    onVoiceSelect: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClearError: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Text to Speech", style = MaterialTheme.typography.titleMedium)

        // Engine toggle: System | Neural
        Text("Engine", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM" to "System", "NEURAL" to "Neural").forEach { (type, label) ->
                val selected = ttsState.engineType == type
                Surface(
                    onClick = { onEngineChange(type) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selected) 0.dp else 1.dp,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Neural voice selector — all voices are embedded, just pick one
        if (ttsState.engineType == "NEURAL") {
            Text("Voice", style = MaterialTheme.typography.labelMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                items(availableVoices) { voice ->
                    val isSelected = ttsState.selectedVoiceId == voice.id ||
                        (ttsState.selectedVoiceId.isEmpty() && voice == availableVoices.first())
                    ListItem(
                        headlineContent = { Text(voice.displayName) },
                        supportingContent = { Text(voice.language) },
                        leadingContent = {
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.RecordVoiceOver,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVoiceSelect(voice.id) },
                    )
                }
            }
        }

        // Speed slider
        Text("Speed: ${"%.1f".format(speed)}x", style = MaterialTheme.typography.labelMedium)
        Slider(value = speed, onValueChange = onSpeedChange, valueRange = 0.5f..3.0f)

        // Pitch (System TTS only)
        if (ttsState.engineType == "SYSTEM") {
            Text("Pitch: ${"%.1f".format(pitch)}x", style = MaterialTheme.typography.labelMedium)
            Slider(value = pitch, onValueChange = onPitchChange, valueRange = 0.5f..2.0f)
        }

        // Error message
        ttsState.errorMessage?.let { errorMsg ->
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClearError() },
            )
        }

        // Play / Pause / Resume / Stop row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                ttsState.isPlaying -> {
                    FilledTonalIconButton(onClick = onPause) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = "Pause")
                    }
                    TextButton(onClick = onStop) { Text("Stop") }
                }
                ttsState.isPaused -> {
                    FilledTonalIconButton(onClick = onResume) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = "Resume")
                    }
                    TextButton(onClick = onStop) { Text("Stop") }
                }
                else -> {
                    FilledTonalIconButton(onClick = onSpeak) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = "Play")
                    }
                }
            }
            if (ttsState.isActive) {
                Text(
                    text = if (ttsState.isPaused) "Paused" else "Playing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun com.jegly.pages.core.model.FontFamily.toComposeFamily(): FontFamily = when (this) {
    com.jegly.pages.core.model.FontFamily.Serif -> FontFamily.Serif
    com.jegly.pages.core.model.FontFamily.Monospace -> FontFamily.Monospace
    com.jegly.pages.core.model.FontFamily.SansSerif -> FontFamily.SansSerif
    com.jegly.pages.core.model.FontFamily.Default -> FontFamily.Default
}