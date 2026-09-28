package io.github.kellyson71.supaco.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            haptics.confirmar()
            // Dá tempo da flor crescer e virar a entrada do app
            if (!reduce) delay(420)
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    LoginContent(uiState = uiState, onLoginClick = viewModel::login)
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    onLoginClick: (String, String) -> Unit,
) {
    var matricula by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val submit = { if (!uiState.isLoading) onLoginClick(matricula, senha) }
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()

    // Senha errada: os campos chacoalham e o celular recusa
    val shake = remember { Animatable(0f) }
    // O aviso de sessão expirada já chega pronto na abertura: esse não chacoalha
    val erroInicial = remember { uiState.error }
    LaunchedEffect(uiState.error) {
        if (uiState.error == null || uiState.error == erroInicial) return@LaunchedEffect
        haptics.rejeitar()
        if (!reduce) {
            repeat(3) {
                shake.animateTo(8f, tween(50))
                shake.animateTo(-8f, tween(50))
            }
            shake.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 800f))
        }
    }

    // Flor: entra girando meia volta, gira sozinha enquanto valida e cresce no sucesso
    val arrive = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { arrive.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
    val loadingSpin = if (uiState.isLoading && !reduce) {
        rememberInfiniteTransition(label = "login_spin").animateFloat(
            0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "login_spin_v",
        ).value
    } else 0f
    val successGrow by animateFloatAsState(
        targetValue = if (uiState.isSuccess && !reduce) 1f else 0f,
        animationSpec = tween(420, easing = Motion.EmphasizedAccelerate),
        label = "login_success",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            // Brand
            ShapeContainer(
                shape = OrgShape.FLOWER,
                size = 88.dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                breathe = uiState.isLoading,
                extraRotation = (1f - arrive.value) * -180f + loadingSpin,
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        val sc = (0.4f + 0.6f * arrive.value) * (1f + successGrow * 12f)
                        scaleX = sc
                        scaleY = sc
                    },
            ) {
                Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                modifier = Modifier.enterOnce(1, stepMs = 60).graphicsLayer { alpha = 1f - successGrow },
                text = "Supaco",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Posso faltar hoje? Entre com o seu SUAP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .enterOnce(2, stepMs = 60)
                    .graphicsLayer { alpha = 1f - successGrow },
            )

            Spacer(Modifier.height(40.dp))

            // Form
            Column(
                Modifier
                    .enterOnce(3, stepMs = 60)
                    .graphicsLayer {
                        translationX = shake.value * density
                        alpha = 1f - successGrow
                    },
            ) {
            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it.filter { c -> c.isDigit() }.take(14) },
                label = { Text("Matrícula") },
                leadingIcon = { Icon(Icons.Rounded.AccountCircle, null) },
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            if (senhaVisivel) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (senhaVisivel) "Ocultar senha" else "Ver senha"
                        )
                    }
                },
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                isError = uiState.error != null,
            )
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { err ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = submit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .enterOnce(4, stepMs = 60)
                    .graphicsLayer { alpha = 1f - successGrow },
                enabled = !uiState.isLoading,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Entrar", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Enquanto valida, uma linha discreta no lugar do antigo overlay escuro
            AnimatedVisibility(visible = uiState.isLoading) {
                Text(
                    "Validando matrícula…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Sua senha vai direto para o SUAP (suap.ifrn.edu.br) por HTTPS e não fica guardada no aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Text(
                "App não-oficial, sem vínculo com o IFRN · feito por aluno, pra aluno",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
        }

    }
}
