package com.nikhil.journal.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/** Content is capped at this width and centered on tablets / landscape, so lines stay readable. */
val ContentMaxWidth = 840.dp

/** Side padding that keeps a full-width scrolling list's content centered and at most [ContentMaxWidth]. */
fun centeredSidePadding(availableWidth: Dp, minPadding: Dp = 16.dp): Dp =
    max(minPadding, (availableWidth - ContentMaxWidth) / 2)

/** Centers the content and limits its width; apply after any scroll modifier so the whole screen scrolls. */
fun Modifier.centeredContentWidth(): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = ContentMaxWidth)
