package com.afrouzi.fakecall.call.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.afrouzi.fakecall.R
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PixelIncomingCallUi(
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pixel_chevron")
    val chevronOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_anim"
    )

    var pillDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1F))
            .padding(vertical = 48.dp, horizontal = 24.dp),
    ) {
        // Pixel Material You Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()
                if (hasPhoto) {
                    AsyncImage(
                        model = File(photoPath!!),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFD0BCFF), CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4A4458)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFFE8DEF8),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCAC4D0),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.incoming_call_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Bottom Pixel Dialer Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            // Swipe Up Chevron & Hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = chevronOffset.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.call_swipe_up_answer),
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Pill / Capsule Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decline Capsule Button
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF8C1D18))
                        .clickable { onDecline() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = stringResource(R.string.incoming_call_decline),
                            tint = Color(0xFFF9DEDC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.incoming_call_decline),
                            color = Color(0xFFF9DEDC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Answer Pill Slider (Supports Drag-Up and Click)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, pillDragY.roundToInt()) }
                        .size(width = 120.dp, height = 56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF386A20))
                        .clickable { onAnswer() }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (pillDragY < -40f) {
                                        onAnswer()
                                    }
                                    pillDragY = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                pillDragY = (pillDragY + dragAmount).coerceAtMost(0f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = stringResource(R.string.incoming_call_answer),
                            tint = Color(0xFFE8F5E9),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.incoming_call_answer),
                            color = Color(0xFFE8F5E9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
