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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
fun SamsungIncomingCallUi(
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "samsung_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var answerOffsetX by remember { mutableFloatStateOf(0f) }
    var declineOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF020617)
                    )
                )
            )
            .padding(vertical = 48.dp, horizontal = 28.dp),
    ) {
        // Caller Info Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()
            if (hasPhoto) {
                AsyncImage(
                    model = File(photoPath!!),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(60.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = callerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = callerNumber,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF94A3B8),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.incoming_call_title),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Medium,
            )
        }

        // Bottom Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            // Quick Message button (Samsung Style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.call_send_message),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action Buttons with Swipe and Tap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decline (Red Button)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(declineOffsetX.roundToInt(), 0) }
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onDecline() }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(declineOffsetX) > 40f) {
                                        onDecline()
                                    }
                                    declineOffsetX = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                declineOffsetX += dragAmount.x
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = stringResource(R.string.incoming_call_decline),
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }

                // Center Swipe Hint
                Text(
                    text = stringResource(R.string.call_swipe_to_answer),
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                )

                // Answer (Green Button with pulsating ripple)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(answerOffsetX.roundToInt(), 0) }
                        .scale(pulseScale)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .clickable { onAnswer() }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(answerOffsetX) > 40f) {
                                        onAnswer()
                                    }
                                    answerOffsetX = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                answerOffsetX += dragAmount.x
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = stringResource(R.string.incoming_call_answer),
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
        }
    }
}
