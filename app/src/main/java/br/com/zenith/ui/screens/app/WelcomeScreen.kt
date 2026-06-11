package br.com.zenith.ui.screens.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.zenith.R
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme

@Composable
fun WelcomeScreen(navController: NavController) {
    ZenithTheme {
        WelcomeContent(
            onNavigateToLogin = { navController.navigate("login") },
            onNavigateToRegister = { navController.navigate("register") }
        )
    }
}

@Composable
fun WelcomeContent(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 250.dp, start = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
            ) {
                Text(
                    text = "Seja bem vindo ao",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = Inter,
                        lineHeight = 38.sp,
                        color = Color(0xFF094000)
                    )
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF238D25), fontWeight = FontWeight.Bold)) {
                            append("Zen")
                        }
                        withStyle(SpanStyle(color = Color(0xFF000000), fontWeight = FontWeight.Bold)) {
                            append("ith")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 52.sp)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.zenith),
                contentDescription = null,
                modifier = Modifier
                    .size(267.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 100.dp, y = (-20).dp),
                colorFilter = ColorFilter.tint(Color(0xFF2E7D32))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Button(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
            ) {
                Text(
                    text = "Criar uma conta",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W600,
                        fontSize = 17.sp
                    )
                )
            }

            OutlinedButton(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)),
            ) {
                Text(
                    text = "Entrar",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W600,
                        fontSize = 17.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePreview() {
    ZenithTheme {
        WelcomeContent(
            onNavigateToLogin = {},
            onNavigateToRegister = {}
        )
    }
}
