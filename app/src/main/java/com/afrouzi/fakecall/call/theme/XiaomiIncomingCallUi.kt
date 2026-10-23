package com.afrouzi.fakecall.call.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.afrouzi.fakecall.R
import java.io.File

@Composable
fun XiaomiIncomingCallUi(
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val answerInteraction = remember { MutableInteractionSource() }
    val isAnswerPressed by answerInteraction.collectIsPressedAsState()
    val answerScale by animateFloatAsState(targetValue = if (isAnswerPressed) 0.88f else 1f, label = "xiaomi_ans_press")

    val declineInteraction = remember { MutableInteractionSource() }
    val isDeclinePressed by declineInteraction.collectIsPressedAsState()
    val declineScale by animateFloatAsState(targetValue = if (isDeclinePressed) 0.88f else 1f, label = "xiaomi_dec_press")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
            .padding(vertical = 48.dp, horizontal = 24.dp),
    ) {
        // Xiaomi Glassmorphic Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF202024).copy(alpha = 0.95f)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()
                if (hasPhoto) {
                    AsyncImage(
                        model = File(photoPath!!),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF38383E), CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C32)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFFA0A0AB),
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF9E9EA8),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.incoming_call_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Bottom Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF26262C))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = Color(0xFFA0A0AB),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.call_quick_reply),
                    color = Color(0xFFA0A0AB),
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decline Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(declineScale)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .clickable(
                                interactionSource = declineInteraction,
                                indication = null,
                                onClick = onDecline
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = stringResource(R.string.incoming_call_decline),
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.incoming_call_decline),
                        color = Color(0xFF9E9EA8),
                        fontSize = 12.sp
                    )
                }

                // Accept Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(answerScale)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                            .clickable(
                                interactionSource = answerInteraction,
                                indication = null,
                                onClick = onAnswer
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = stringResource(R.string.incoming_call_answer),
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.incoming_call_answer),
                        color = Color(0xFF9E9EA8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
