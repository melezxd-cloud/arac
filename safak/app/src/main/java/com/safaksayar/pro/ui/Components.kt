package com.safaksayar.pro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safaksayar.pro.data.Plates

/** Yeşil halka: yeşil kısım kalan günü gösterir, günler geçtikçe azalır. */
@Composable
fun SafakRing(daysLeft: Int, remainingFraction: Float, finished: Boolean, size: Dp = 230.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2 + 1.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(SafakColors.RedSoft, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * remainingFraction.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    SafakColors.Green, -90f, sweep, false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ŞAFAK", color = SafakColors.RedDark, fontSize = 12.sp, letterSpacing = 2.sp)
            Text(
                daysLeft.toString(), color = SafakColors.Green,
                fontSize = 62.sp, fontWeight = FontWeight.Medium, lineHeight = 66.sp
            )
            Text(
                if (finished) "terhis oldun!" else "gün kaldı",
                color = SafakColors.RedDark, fontSize = 13.sp
            )
        }
    }
}

/** Krem zeminli, mavi yazılı plaka. Kalan gün 1..81 iken görünür. */
@Composable
fun PlateBadge(daysLeft: Int) {
    val city = Plates.cityUpper(daysLeft) ?: return
    Row(
        Modifier
            .width(230.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, SafakColors.Blue, RoundedCornerShape(6.dp))
            .background(SafakColors.Cream)
    ) {
        Box(
            Modifier.width(26.dp).fillMaxHeight().background(SafakColors.Blue),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text("TR", color = SafakColors.White, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Row(
            Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Plates.code(daysLeft), color = SafakColors.Blue, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(10.dp))
            Text(city, color = SafakColors.Blue, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun RowScope.StatCard(label: String, value: String, valueColor: Color = SafakColors.Red, valueSize: Int = 17) {
    Column(
        Modifier
            .weight(1f)
            .border(1.dp, SafakColors.RedBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = SafakColors.RedDark, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = valueSize.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProgressBar(fraction: Float) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SafakColors.RedSoft)
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(4.dp)).background(SafakColors.Green)
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, color = SafakColors.Red, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 14.dp, bottom = 8.dp)
    )
}
