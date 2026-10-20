package com.shohankhan.hisabghor.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Bottom navigation item. Icons come in filled (selected) and outlined
 * (unselected) pairs so the current destination is obvious without relying
 * on colour alone.
 */
data class HgNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * Restrained 5-destination bottom bar: hairline top border, white surface,
 * brand tint pill behind the selected icon, always-visible labels.
 */
@Composable
fun HgBottomBar(
    items: List<HgNavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Hg.colors.surface,
        shadowElevation = 0.dp
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hg.colors.hairline)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(min = Dimens.bottomBarHeight)
                    .padding(horizontal = Space.xs, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavBarButton(
                        item = item,
                        selected = selected,
                        onClick = { onSelect(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarButton(
    item: HgNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Hg.colors.brand else Hg.colors.inkTertiary
    Column(
        modifier
            .heightIn(min = Dimens.touchTarget)
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { this.selected = selected }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(44.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(if (selected) Hg.colors.brandTint else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(Dimens.navIcon)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
