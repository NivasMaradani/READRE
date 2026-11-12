package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.HighlightRed
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryBackground
import com.example.ui.theme.SecondaryText

@Composable
fun PinSetupDialog(
    onPinSet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondaryBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = HighlightRed
            )
        },
        title = {
            Text(
                text = "Set Up Hidden Library PIN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create a 4 to 6 digit security PIN. This PIN protects your hidden manga collection.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_confirm_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = HighlightRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> errorMessage = "PIN must be at least 4 digits."
                        pin != confirmPin -> errorMessage = "PINs do not match."
                        else -> onPinSet(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("setup_pin_submit_button")
            ) {
                Text("Save PIN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("setup_pin_cancel_button")
            ) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
fun PinAuthDialog(
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onChangePinRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondaryBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = HighlightRed
            )
        },
        title = {
            Text(
                text = "Protected Hidden Library",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter your security PIN to access the protected manga collection.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_pin_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = HighlightRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onChangePinRequest,
                    modifier = Modifier.testTag("auth_change_pin_button")
                ) {
                    Text("Change PIN", color = SecondaryText, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = "Please enter your 4-6 digit PIN."
                    } else {
                        val valid = onVerifyPin(pin)
                        if (valid) {
                            onSuccess()
                        } else {
                            errorMessage = "Incorrect PIN. Please try again."
                            pin = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("auth_pin_submit_button")
            ) {
                Text("Unlock", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("auth_pin_cancel_button")
            ) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
fun ChangePinDialog(
    onChangePin: (oldPin: String, newPin: String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondaryBackground,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Password,
                contentDescription = null,
                tint = HighlightRed
            )
        },
        title = {
            Text(
                text = "Change Security PIN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            oldPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_old_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("New PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_new_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmNewPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = HighlightRed,
                        unfocusedBorderColor = SecondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_confirm_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = HighlightRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        oldPin.isEmpty() -> errorMessage = "Please enter your current PIN."
                        newPin.length < 4 -> errorMessage = "New PIN must be at least 4 digits."
                        newPin != confirmNewPin -> errorMessage = "New PINs do not match."
                        else -> {
                            val success = onChangePin(oldPin, newPin)
                            if (success) {
                                onSuccess()
                            } else {
                                errorMessage = "Current PIN is incorrect."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("change_pin_submit_button")
            ) {
                Text("Update PIN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("change_pin_cancel_button")
            ) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
