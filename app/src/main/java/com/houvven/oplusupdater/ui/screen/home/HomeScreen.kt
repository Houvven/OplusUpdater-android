package com.houvven.oplusupdater.ui.screen.home

import android.annotation.SuppressLint
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.houvven.oplusupdater.R
import com.houvven.oplusupdater.ui.screen.home.components.AboutInfoDialog
import com.houvven.oplusupdater.ui.screen.home.components.UpdateQueryResponseCard
import com.houvven.oplusupdater.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme
import updater.ResponseResult
import updater.Updater

@Keep
enum class OtaRegion(
    @StringRes val strRes: Int,
) {
    CN(R.string.china),
    EU(R.string.europe),
    IN(R.string.india),
    SG(R.string.singapore)
}

@delegate:SuppressLint("PrivateApi")
val systemOtaVersion: String by lazy {
    val clazz = Class.forName("android.os.SystemProperties")
    val method = clazz.getMethod("get", String::class.java, String::class.java)
    method.invoke(clazz, "ro.build.version.ota", "") as String
}

val simpleSystemOtaVersion: String by lazy {
    systemOtaVersion.split(".").dropLast(1).joinToString(".")
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val scrollState = rememberScrollState()
    var showAboutInfoDialog by remember { mutableStateOf(false) }

    var expandMoreParameters by rememberSaveable { mutableStateOf(false) }
    var otaVersion by rememberSaveable { mutableStateOf(simpleSystemOtaVersion) }
    var model by rememberSaveable { mutableStateOf("") }
    var carrier by rememberSaveable { mutableStateOf("") }
    var otaRegion by rememberSaveable { mutableStateOf(OtaRegion.CN) }
    var proxy by rememberSaveable { mutableStateOf("") }
    var responseResult by rememberSaveable { mutableStateOf<ResponseResult?>(null) }
    val msgFlow = MutableSharedFlow<String>()

    LaunchedEffect(msgFlow) {
        msgFlow.collectLatest {
            withContext(Dispatchers.Main) { context.toast(it) }
        }
    }

    LaunchedEffect(responseResult) {
        responseResult?.run {
            when (responseCode.toInt()) {
                200 -> msgFlow.emit(context.getString(R.string.msg_query_success))
                304 -> msgFlow.emit(context.getString(R.string.msg_no_update_available))
                else -> msgFlow.emit("code: $responseCode, $errMsg")
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Updater",
                actions = {
                    IconButton(onClick = {
                        showAboutInfoDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = otaVersion,
                onValueChange = { otaVersion = it.trim() },
                label = stringResource(R.string.ota_version),
                trailingIcon = {
                    IconButton(
                        onClick = { expandMoreParameters = !expandMoreParameters }
                    ) {
                        val icon = if (expandMoreParameters) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }
            )

            AnimatedVisibility(visible = expandMoreParameters) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = model,
                        onValueChange = { model = it.trim() },
                        label = stringResource(R.string.model)
                    )
                    TextField(
                        value = carrier,
                        onValueChange = { carrier = it.trim() },
                        label = stringResource(R.string.carrier)
                    )
                    TextField(
                        value = proxy,
                        onValueChange = { proxy = it.trim() },
                        label = stringResource(R.string.proxy)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                SuperDropdown(
                    title = stringResource(R.string.region),
                    items = OtaRegion.entries.map { stringResource(it.strRes) },
                    selectedIndex = OtaRegion.entries.indexOf(otaRegion)
                ) {
                    otaRegion = OtaRegion.entries[it]
                }
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val args = updater.QueryUpdateArgs().also {
                        it.otaVersion = otaVersion
                        it.region = otaRegion.name
                        it.model = model
                        it.nvCarrier = carrier
                        it.proxy = proxy
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            responseResult = null
                            responseResult = Updater.queryUpdate(args)
                        } catch (e: Exception) {
                            msgFlow.emit(e.message ?: e.stackTraceToString())
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                enabled = otaVersion.isNotBlank(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = stringResource(R.string.query),
                    color = MiuixTheme.colorScheme.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            AnimatedVisibility(visible = responseResult != null) {
                responseResult?.let { UpdateQueryResponseCard(it) }
            }

            if (showAboutInfoDialog) {
                AboutInfoDialog(
                    onDismissRequest = { showAboutInfoDialog = false }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HomeScreen()
}