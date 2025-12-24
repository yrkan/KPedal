package io.github.kpedal.ui.screens.help

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kpedal.data.HelpContent
import io.github.kpedal.ui.theme.Theme
import kotlinx.coroutines.launch

/**
 * Onboarding screen shown on first app launch.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = HelpContent.onboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Skip",
                color = Theme.colors.dim,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onComplete() }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPage(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                        .background(
                            if (pagerState.currentPage == index) Theme.colors.optimal
                            else Theme.colors.dim,
                            CircleShape
                        )
                )
            }
        }

        // Next/Done button
        val isLastPage = pagerState.currentPage == pages.size - 1
        Text(
            text = if (isLastPage) "Get Started" else "Next",
            color = Theme.colors.background,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Theme.colors.optimal)
                .clickable {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
                .padding(vertical = 14.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPage(
    page: HelpContent.OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon placeholder (using emoji for simplicity)
        Text(
            text = when (page.icon) {
                "bike" -> "\uD83D\uDEB4"       // cyclist emoji
                "layout" -> "\uD83D\uDCCA"     // chart
                "drill" -> "\uD83C\uDFAF"      // target
                "alert" -> "\uD83D\uDD14"      // bell
                "background" -> "\uD83D\uDD04" // arrows circle (refresh)
                "cloud" -> "\u2601"            // cloud
                else -> "\u2139"               // info
            },
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = page.title,
            color = Theme.colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            color = Theme.colors.dim,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
