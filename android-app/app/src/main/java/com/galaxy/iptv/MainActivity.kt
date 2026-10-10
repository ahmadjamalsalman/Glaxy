package com.galaxy.iptv

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class MainActivity : ComponentActivity() {
    private var showSplash by mutableStateOf(true)
    private var isLoggedIn by mutableStateOf(false)
    private var activationInProgress by mutableStateOf(false)
    private var activationMessage by mutableStateOf<String?>(null)
    private var accountDetails by mutableStateOf<AccountDetails?>(null)
    private var rememberedCode by mutableStateOf("")
    private var preferredPlayer by mutableStateOf(PlaybackEngine.EXO_PLAYER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rememberedCode = ActivationCodeStore.load(this).orEmpty()
        preferredPlayer = getSharedPreferences("galaxy_settings", MODE_PRIVATE)
            .getString("playback_engine", PlaybackEngine.EXO_PLAYER.name)
            ?.let { runCatching { PlaybackEngine.valueOf(it) }.getOrNull() }
            ?: PlaybackEngine.EXO_PLAYER
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            showSplash = false
        }, 5000)

        if (rememberedCode.isNotBlank()) {
            activate(rememberedCode, showSuccessToast = false)
        }

        setContent {
            GalaxyIptvAppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (showSplash) {
                        SplashScreen()
                    } else if (!isLoggedIn) {
                        LoginScreen(
                            initialCode = rememberedCode,
                            isLoading = activationInProgress,
                            message = activationMessage,
                            onActivate = ::activate
                        )
                    } else {
                        GalaxyIptvScreen(
                            account = accountDetails,
                            preferredPlayer = preferredPlayer,
                            onPreferredPlayerChange = { engine ->
                                preferredPlayer = engine
                                getSharedPreferences("galaxy_settings", MODE_PRIVATE)
                                    .edit()
                                    .putString("playback_engine", engine.name)
                                    .apply()
                            },
                            onSessionExpired = { message ->
                                activationMessage = message
                                accountDetails = null
                                isLoggedIn = false
                            },
                            onLogout = {
                                ActivationCodeStore.clear(this)
                                rememberedCode = ""
                                accountDetails = null
                                isLoggedIn = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val code = accountDetails?.code
        if (!showSplash && isLoggedIn && !activationInProgress && !code.isNullOrBlank()) {
            activate(code, showSuccessToast = false)
        }
    }

    private fun activate(code: String, showSuccessToast: Boolean = true) {
        if (activationInProgress) return
        rememberedCode = code
        activationInProgress = true
        activationMessage = null

        Thread {
            val result = ActivationApi.activate(this, code)
            runOnUiThread {
                activationInProgress = false
                if (result.status == 100) {
                    ActivationCodeStore.save(this, code)
                    accountDetails = AccountDetails(
                        userId = result.codeId,
                        code = code,
                        expires = result.expire,
                        userAgent = result.userAgent
                    )
                    val notice = listOfNotNull(
                        result.message.takeIf(String::isNotBlank),
                        result.expire.takeIf(String::isNotBlank)?.let { "الصلاحية: $it" },
                        result.osd.takeIf(String::isNotBlank)
                    ).joinToString("\n")
                    if (showSuccessToast && notice.isNotBlank()) {
                        Toast.makeText(this, notice, Toast.LENGTH_LONG).show()
                    }
                    isLoggedIn = true
                } else {
                    isLoggedIn = false
                    accountDetails = null
                    activationMessage = if (result.status == 104) {
                        "انتهت مدة الاشتراك: ${result.message}"
                    } else {
                        result.message.ifBlank { "تعذر تفعيل الكود." }
                    }
                }
            }
        }.start()
    }
}

@Composable
fun LoginScreen(
    initialCode: String,
    isLoading: Boolean,
    message: String?,
    onActivate: (String) -> Unit
) {
    var activationCode by remember(initialCode) { mutableStateOf(initialCode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF050B14), Color(0xFF101827), Color(0xFF050B14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xCC0F172A),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Galaxy IPTV logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "ACTIVATE YOUR DEVICE",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "أدخل كود التفعيل للمتابعة",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = activationCode,
                                onValueChange = { value ->
                                    activationCode = value.filter { it in '0'..'9' }
                                },
                                label = { Text("كود التفعيل") },
                                placeholder = { Text("أدخل الكود هنا") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6D5EF7),
                                    focusedLabelColor = Color(0xFFB8B4FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    cursorColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0x1A0F172A),
                                    unfocusedContainerColor = Color(0x1A0F172A)
                                )
                            )

                            Button(
                                onClick = { onActivate(activationCode.trim()) },
                                enabled = activationCode.isNotBlank() && !isLoading,
                                modifier = Modifier
                                    .width(164.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6D5EF7),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isLoading) "جارٍ التحقق..." else "تفعيل الكود",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        if (message != null) {
                            Text(
                                text = message,
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ActivationResult(
    val status: Int,
    val message: String,
    val expire: String = "",
    val osd: String = "",
    val codeId: String = "",
    val userAgent: String = ""
)

private data class AccountDetails(
    val userId: String,
    val code: String,
    val expires: String,
    val userAgent: String
)

private data class ContentQuery(
    val mode: String,
    val argumentName: String? = null,
    val argumentValue: String? = null
)

private data class ContentEntry(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val details: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val nextQuery: ContentQuery? = null
)

private data class ContentFetchResult(
    val entries: List<ContentEntry>,
    val message: String? = null,
    val expired: Boolean = false
)

private class ContentExpiredException(message: String) : Exception(message)

private data class ApiCall(
    val mode: String,
    val arguments: Map<String, String> = emptyMap()
)

private data class ContentScreenState(
    val title: String,
    val query: ContentQuery,
    val detailEntry: ContentEntry? = null
)

private enum class PlaybackEngine {
    EXO_PLAYER,
    VLC
}

private object ActivationCodeStore {
    private const val KEY_ALIAS = "galaxy_activation_code_key"
    private const val PREFERENCES = "galaxy_activation_session"
    private const val CODE_KEY = "encrypted_code"

    fun save(context: Context, code: String) {
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(code.toByteArray(StandardCharsets.UTF_8))
            val storedValue = cipher.iv + encrypted
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(CODE_KEY, Base64.encodeToString(storedValue, Base64.NO_WRAP))
                .apply()
        }
    }

    fun load(context: Context): String? {
        val stored = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(CODE_KEY, null) ?: return null
        return runCatching {
            val encrypted = Base64.decode(stored, Base64.NO_WRAP)
            require(encrypted.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = encrypted.copyOfRange(0, 12)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted.copyOfRange(12, encrypted.size)), StandardCharsets.UTF_8)
        }.getOrElse {
            clear(context)
            null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(CODE_KEY)
            .apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }
}

private object ActivationApi {
    private const val ENDPOINT = "http://gean4563t.xyz:80/V6/API-V6.php"
    private const val XOR_KEY = "7wEp.20A8opr+3EM]xe.@y]("

    fun activate(context: Context, code: String): ActivationResult {
        return try {
            val responseText = postRequest(context, code, "active")
            val response = JSONObject(responseText.trim())
            ActivationResult(
                status = response.optInt("status", -1),
                message = response.optString("message", ""),
                expire = response.optString("expire", ""),
                osd = response.optString("osd", ""),
                codeId = response.optString("code_id", ""),
                userAgent = response.optString("user_agent", "")
            )
        } catch (_: Exception) {
            ActivationResult(-1, "تعذر الاتصال بالخادم، حاول مرة أخرى لاحقًا.")
        }
    }

    fun fetchContent(context: Context, code: String, query: ContentQuery): ContentFetchResult {
        return try {
            var filter: String? = null
            val calls = when (query.mode) {
                "sports" -> {
                    filter = "sport"
                    listOf(ApiCall("lite_packages"))
                }
                "kids" -> {
                    filter = "kids"
                    listOf(ApiCall("movies_cat"), ApiCall("series_cat"))
                }
                "ramadan" -> {
                    filter = "ramadan"
                    listOf(ApiCall("series_cat"))
                }
                else -> listOf(
                    ApiCall(
                        mode = query.mode,
                        arguments = if (query.argumentName != null && query.argumentValue != null) {
                            mapOf(query.argumentName to query.argumentValue)
                        } else {
                            emptyMap()
                        }
                    )
                )
            }

            val entries = calls.flatMap { call ->
                val responseText = postRequest(context, code, call.mode, call.arguments).trim()
                if (responseText.startsWith("{")) {
                    val responseObject = JSONObject(responseText)
                    if (responseObject.optInt("status") == 104) {
                        throw ContentExpiredException(
                            "انتهت مدة الاشتراك: ${responseObject.optString("message")}".trim()
                        )
                    }
                }
                val response = JSONArray(responseText)
                parseContent(response, call.mode, filter)
            }.distinctBy { "${it.id}:${it.title}" }

            ContentFetchResult(
                entries = entries,
                message = if (entries.isEmpty()) "لا توجد بيانات لهذا القسم في الخادم." else null
            )
        } catch (expired: ContentExpiredException) {
            ContentFetchResult(emptyList(), expired.message, expired = true)
        } catch (_: Exception) {
            ContentFetchResult(emptyList(), "تعذر جلب البيانات من الخادم، حاول مرة أخرى لاحقًا.")
        }
    }

    private fun postRequest(
        context: Context,
        code: String,
        mode: String,
        arguments: Map<String, String> = emptyMap()
    ): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty().ifBlank { UUID.randomUUID().toString().replace("-", "") }
        val model = Build.MODEL.ifBlank { "Android" }
        val payload = JSONObject()
            .put("mode", mode)
            .put("code", code)
            .put("mac", pseudoMac(androidId))
            .put("sn", androidId)
            .put("chipid", "")
            .put("model", model)
            .put("firmware_ver", Build.VERSION.RELEASE.orEmpty())
        arguments.forEach { (name, value) -> payload.put(name, value) }

        val encrypted = Base64.encodeToString(
            xor(payload.toString().toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )
        val requestBody = "json=" + URLEncoder.encode(encrypted, "UTF-8")
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", "REV $model ${Build.VERSION.RELEASE.orEmpty()}")
        }

        return try {
            connection.outputStream.use {
                it.write(requestBody.toByteArray(StandardCharsets.UTF_8))
            }
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            } ?: throw IllegalStateException("HTTP $responseCode")
            String(xor(responseStream.use { it.readBytes() }), StandardCharsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContent(response: JSONArray, mode: String, filter: String?): List<ContentEntry> {
        val entries = mutableListOf<ContentEntry>()
        for (index in 0 until response.length()) {
            val item = response.optJSONObject(index) ?: continue
            when (mode) {
                "movies_cat", "series_cat" -> {
                    val parentTitle = item.optString("cat_name")
                    val children = item.optJSONArray("sub_cats")
                    val nextMode = if (mode == "movies_cat") "movies_list" else "series_list"
                    if (children != null && children.length() > 0) {
                        for (childIndex in 0 until children.length()) {
                            val child = children.optJSONObject(childIndex) ?: continue
                            val childTitle = child.optString("sub_name").ifBlank { parentTitle }
                            if (matchesFilter(filter, "$parentTitle $childTitle")) {
                                val categoryId = child.optString("sub_id").ifBlank {
                                    item.optString("cat_id")
                                }
                                entries += ContentEntry(
                                    id = categoryId,
                                    title = childTitle,
                                    subtitle = parentTitle,
                                    imageUrl = child.optString("sub_icon"),
                                    nextQuery = ContentQuery(nextMode, "catid", categoryId)
                                )
                            }
                        }
                    } else if (matchesFilter(filter, parentTitle)) {
                        val categoryId = item.optString("cat_id")
                        entries += ContentEntry(
                            id = categoryId,
                            title = parentTitle,
                            imageUrl = item.optString("cat_icon"),
                            nextQuery = ContentQuery(nextMode, "catid", categoryId)
                        )
                    }
                }
                "lite_packages" -> {
                    val title = item.optString("pkg_name")
                        .ifBlank { item.optString("package_name") }
                        .ifBlank { item.optString("name") }
                    if (matchesFilter(filter, title)) {
                        val packageId = item.optString("id").ifBlank { item.optString("pkg_id") }
                        entries += ContentEntry(
                            id = packageId,
                            title = title,
                            imageUrl = item.optString("pkg_icon"),
                            nextQuery = ContentQuery("lite_channels", "pkg_id", packageId)
                        )
                    }
                }
                "lite_channels" -> entries += ContentEntry(
                    id = item.optString("id", index.toString()),
                    title = item.optString("stream_display_name"),
                    subtitle = "Live channel",
                    imageUrl = item.optString("stream_icon"),
                    streamUrl = item.optString("stream_url")
                )
                "sat2iptv" -> entries += ContentEntry(
                    id = index.toString(),
                    title = item.optString("channel_name"),
                    subtitle = "Sports",
                    streamUrl = item.optString("stream_url")
                )
                "movies_list" -> entries += ContentEntry(
                    id = item.optString("id", index.toString()),
                    title = item.optString("title"),
                    subtitle = item.optString("genre"),
                    description = item.optString("plot"),
                    details = item.optString("releaseDate"),
                    imageUrl = item.optString("icon"),
                    nextQuery = ContentQuery("movies_info", "movie_id", item.optString("id"))
                )
                "series_list" -> entries += ContentEntry(
                    id = item.optString("id", index.toString()),
                    title = item.optString("title"),
                    subtitle = item.optString("genre"),
                    description = item.optString("plot"),
                    details = listOfNotNull(
                        item.optString("releaseDate").takeIf(String::isNotBlank),
                        item.optString("rating").takeIf(String::isNotBlank)?.let { "التقييم $it" }
                    ).joinToString(" • "),
                    imageUrl = item.optString("icon"),
                    nextQuery = ContentQuery("series_info", "series_id", item.optString("id"))
                )
                "movies_info" -> entries += ContentEntry(
                    id = item.optString("id", index.toString()),
                    title = item.optString("stream_display_name").ifBlank { item.optString("title") },
                    subtitle = item.optString("genre"),
                    description = item.optString("plot"),
                    details = listOfNotNull(
                        item.optString("duration").takeIf(String::isNotBlank),
                        item.optString("rating").takeIf(String::isNotBlank)?.let { "التقييم $it" },
                        item.optString("releaseDate").takeIf(String::isNotBlank)
                    ).joinToString(" • "),
                    imageUrl = item.optString("movie_image").ifBlank { item.optString("stream_icon") },
                    streamUrl = item.optString("stream_url")
                )
                "series_info" -> entries += ContentEntry(
                    id = index.toString(),
                    title = item.optString("episode_name"),
                    subtitle = "Season ${item.optString("season_num")} · Episode ${item.optString("episode_num")}",
                    imageUrl = item.optString("stream_icon"),
                    streamUrl = item.optString("stream_url")
                )
            }
        }
        return entries
    }

    private fun matchesFilter(filter: String?, text: String): Boolean {
        if (filter == null) return true
        val normalized = text.lowercase(Locale.ROOT)
        return when (filter) {
            "kids" -> listOf("kid", "child", "cartoon", "animation").any(normalized::contains)
            "sport" -> listOf("sport", "bein", "football", "soccer", "رياض", "كورة", "كرة")
                .any(normalized::contains)
            else -> normalized.contains(filter)
        }
    }

    private fun xor(input: ByteArray): ByteArray {
        val key = XOR_KEY.toByteArray(StandardCharsets.UTF_8)
        return ByteArray(input.size) { index ->
            (input[index].toInt() xor key[index % key.size].toInt()).toByte()
        }
    }

    private fun pseudoMac(androidId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(androidId.toByteArray(StandardCharsets.UTF_8))
        digest[0] = ((digest[0].toInt() and 0xFE) or 0x02).toByte()
        return digest.take(6).joinToString(":") { "%02X".format(Locale.US, it.toInt() and 0xFF) }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030619)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_login),
            contentDescription = "Splash logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "© Galaxy 2026",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun GalaxyIptvAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6D5EF7),
            secondary = Color(0xFF2DD4BF),
            background = Color(0xFF070B14),
            surface = Color(0xFF0F172A),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content
    )
}

private data class Category(
    val title: String,
    val count: String,
    val color: Color,
    val icon: ImageVector
)

private data class Channel(
    val name: String,
    val category: String,
    val quality: String,
    val accent: Color
)

@Composable
private fun GalaxyIptvScreen(
    account: AccountDetails?,
    preferredPlayer: PlaybackEngine,
    onPreferredPlayerChange: (PlaybackEngine) -> Unit,
    onSessionExpired: (String) -> Unit,
    onLogout: () -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }
    var selectedSetting by remember { mutableStateOf("Account") }
    var contentStack by remember { mutableStateOf(emptyList<ContentScreenState>()) }
    var contentEntries by remember { mutableStateOf(emptyList<ContentEntry>()) }
    var contentLoading by remember { mutableStateOf(false) }
    var contentMessage by remember { mutableStateOf<String?>(null) }
    var selectedStreamUrl by remember { mutableStateOf("") }
    var fullScreenPlayback by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentContent = contentStack.lastOrNull()

    BackHandler(enabled = fullScreenPlayback || settingsOpen || contentStack.isNotEmpty()) {
        if (fullScreenPlayback) {
            fullScreenPlayback = false
        } else if (settingsOpen) {
            settingsOpen = false
        } else if (contentStack.isNotEmpty()) {
            contentStack = contentStack.dropLast(1)
        }
    }

    LaunchedEffect(currentContent, account?.code) {
        val activeContent = currentContent ?: return@LaunchedEffect
        val activeCode = account?.code ?: return@LaunchedEffect
        contentLoading = true
        contentMessage = null
        val result = withContext(Dispatchers.IO) {
            ActivationApi.fetchContent(context, activeCode, activeContent.query)
        }
        contentEntries = result.entries
        contentMessage = result.message
        selectedStreamUrl = result.entries.firstOrNull { it.streamUrl.isNotBlank() }?.streamUrl.orEmpty()
        contentLoading = false
        if (result.expired) {
            onSessionExpired(result.message ?: "انتهت مدة الاشتراك.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050611), Color(0xFF02030A), Color(0xFF07051A))
                )
            )
    ) {
        if (fullScreenPlayback && selectedStreamUrl.isNotBlank()) {
            IptvPlayer(
                streamUrl = selectedStreamUrl,
                userAgent = account?.userAgent.orEmpty(),
                engine = preferredPlayer,
                showController = false,
                onPlaybackError = {
                    if (preferredPlayer == PlaybackEngine.EXO_PLAYER) {
                        onPreferredPlayerChange(PlaybackEngine.VLC)
                    } else {
                        fullScreenPlayback = false
                        Toast.makeText(context, "تعذر تشغيل هذا البث.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (settingsOpen) {
            GalaxySettingsPage(
                account = account,
                preferredPlayer = preferredPlayer,
                onPreferredPlayerChange = onPreferredPlayerChange,
                selectedSetting = selectedSetting,
                onSelectSetting = { selectedSetting = it },
                onHome = { settingsOpen = false },
                onLogout = onLogout
            )
        } else if (currentContent != null) {
            GalaxyContentPage(
                title = currentContent.title,
                entries = contentEntries,
                isLoading = contentLoading,
                message = contentMessage,
                channelListMode = currentContent.query.mode in setOf(
                    "lite_packages",
                    "sports",
                    "lite_channels",
                    "sat2iptv"
                ),
                packageListMode = currentContent.query.mode == "lite_packages" || currentContent.query.mode == "sports",
                selectedStreamUrl = selectedStreamUrl,
                playerUserAgent = account?.userAgent.orEmpty(),
                playbackEngine = preferredPlayer,
                onSelectPlaybackEngine = onPreferredPlayerChange,
                detailEntry = when (currentContent.query.mode) {
                    "movies_info" -> contentEntries.firstOrNull() ?: currentContent.detailEntry
                    else -> currentContent.detailEntry
                },
                episodes = contentEntries.takeIf { currentContent.query.mode == "series_info" }.orEmpty(),
                onBack = { contentStack = contentStack.dropLast(1) },
                onSelectEntry = { entry ->
                    entry.nextQuery?.let { query ->
                        val selectedDetail = entry.takeIf {
                            query.mode == "movies_info" || query.mode == "series_info"
                        }
                        contentStack = contentStack + ContentScreenState(
                            title = entry.title,
                            query = query,
                            detailEntry = selectedDetail
                        )
                    }
                },
                onSelectStream = { selectedStreamUrl = it.streamUrl },
                onPlayMedia = { media ->
                    selectedStreamUrl = media.streamUrl
                    fullScreenPlayback = true
                },
                onPlaybackError = {
                    if (preferredPlayer == PlaybackEngine.EXO_PLAYER) {
                        onPreferredPlayerChange(PlaybackEngine.VLC)
                    } else {
                        Toast.makeText(context, "تعذر تشغيل هذا البث.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                GalaxyHomePage(
                    onSettings = { settingsOpen = true },
                    onSelectCategory = { category ->
                        val query = when (category) {
                            "Live" -> ContentQuery("lite_packages")
                            "Sports" -> ContentQuery("sports")
                            "Movies" -> ContentQuery("movies_cat")
                            "Series" -> ContentQuery("series_cat")
                            "Kids" -> ContentQuery("kids")
                            else -> ContentQuery("ramadan")
                        }
                        contentEntries = emptyList()
                        contentStack = listOf(ContentScreenState(category, query))
                    }
                )
            }
        }

    }
}

@Composable
private fun GalaxyWordmark() {
    Text(
        text = "GALAXY",
        style = TextStyle(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF25C8FF), Color(0xFF6276FF), Color(0xFFE044FF))
            ),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun GalaxyHomePage(onSettings: () -> Unit, onSelectCategory: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        val horizontalPadding = if (wideLayout) 48.dp else 22.dp
        val tileGridWidth = if (wideLayout) 230.dp else 180.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            GalaxyWordmark()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonCategoryTile(
                    title = "LIVE",
                    icon = Icons.Default.Tv,
                    accent = Color(0xFF16B9FF),
                    large = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.78f),
                    onClick = { onSelectCategory("Live") }
                )
                NeonCategoryTile(
                    title = "SPORTS",
                    icon = Icons.Default.SportsSoccer,
                    accent = Color(0xFFCE35FF),
                    large = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.78f),
                    onClick = { onSelectCategory("Sports") }
                )
                Column(
                    modifier = Modifier
                        .width(tileGridWidth)
                        .fillMaxHeight(0.78f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeonCategoryTile("movies", Icons.Default.Movie, Color(0xFFCE35FF), modifier = Modifier.weight(1f), onClick = { onSelectCategory("Movies") })
                        NeonCategoryTile("series", Icons.Default.PlayArrow, Color(0xFF28C9FF), modifier = Modifier.weight(1f), onClick = { onSelectCategory("Series") })
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeonCategoryTile("kids", Icons.Default.Star, Color(0xFFFF4DCB), modifier = Modifier.weight(1f), onClick = { onSelectCategory("Kids") })
                        NeonCategoryTile("ramadan", Icons.Default.Nightlight, Color(0xFFD23CFF), modifier = Modifier.weight(1f), onClick = { onSelectCategory("Ramadan") })
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        NeonCategoryTile(
                            "settings",
                            Icons.Default.Tune,
                            Color(0xFF29C9FF),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(),
                            onClick = onSettings
                        )
                    }
                }
            }

            Text(
                text = "© 2025 Galaxy OTT",
                color = Color(0xFF62647B),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun NeonCategoryTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF080916).copy(alpha = 0.88f))
            .border(1.dp, accent.copy(alpha = 0.85f), shape)
            .clickable(onClick = onClick)
            .padding(if (large) 14.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(if (large) 54.dp else 24.dp)
        )
        Spacer(modifier = Modifier.height(if (large) 10.dp else 4.dp))
        Text(
            text = title,
            color = accent,
            fontSize = if (large) 20.sp else 10.sp,
            fontWeight = if (large) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun GalaxyContentPage(
    title: String,
    entries: List<ContentEntry>,
    isLoading: Boolean,
    message: String?,
    channelListMode: Boolean,
    packageListMode: Boolean,
    selectedStreamUrl: String,
    playerUserAgent: String,
    playbackEngine: PlaybackEngine,
    onSelectPlaybackEngine: (PlaybackEngine) -> Unit,
    detailEntry: ContentEntry?,
    episodes: List<ContentEntry>,
    onBack: () -> Unit,
    onSelectEntry: (ContentEntry) -> Unit,
    onSelectStream: (ContentEntry) -> Unit,
    onPlayMedia: (ContentEntry) -> Unit,
    onPlaybackError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹",
                color = Color(0xFF42C4FF),
                fontSize = 36.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 18.dp)
            )
            Column {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("GALAXY IPTV", color = Color(0xFF73809F), fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        when {
            detailEntry != null -> GalaxyMediaDetailsPage(
                media = detailEntry,
                episodes = episodes,
                isLoading = isLoading,
                message = message,
                onBack = onBack,
                onPlay = onPlayMedia
            )
            isLoading -> Text("جارٍ تحميل البيانات...", color = Color(0xFFB8C5E2), fontSize = 14.sp)
            message != null -> Text(message, color = Color(0xFFFCA5A5), fontSize = 14.sp)
            entries.isEmpty() -> Text("لا توجد عناصر للعرض.", color = Color(0xFFB8C5E2), fontSize = 14.sp)
            channelListMode -> ChannelPlayerLayout(
                modifier = Modifier.weight(1f),
                entries = entries,
                packageListMode = packageListMode,
                selectedStreamUrl = selectedStreamUrl,
                playerUserAgent = playerUserAgent,
                playbackEngine = playbackEngine,
                onSelectPlaybackEngine = onSelectPlaybackEngine,
                onSelectEntry = onSelectEntry,
                onSelectStream = onSelectStream,
                onPlaybackError = onPlaybackError
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries.size, key = { "${entries[it].id}:${entries[it].title}" }) { index ->
                    val entry = entries[index]
                    val shape = RoundedCornerShape(6.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(shape)
                            .background(Color(0xFF090C19))
                            .border(1.dp, Color(0xFF1B2852), shape)
                            .clickable(enabled = entry.nextQuery != null) { onSelectEntry(entry) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .background(Color(0xFF111827)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = entry.imageUrl,
                                    contentDescription = entry.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color(0xFF43BFFF),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = entry.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.subtitle.isNotBlank()) {
                                Text(
                                    text = entry.subtitle,
                                    color = Color(0xFF8792AE),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalaxyMediaDetailsPage(
    media: ContentEntry,
    episodes: List<ContentEntry>,
    isLoading: Boolean,
    message: String?,
    onBack: () -> Unit,
    onPlay: (ContentEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹",
                color = Color(0xFF42C4FF),
                fontSize = 36.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 18.dp)
            )
            Column {
                Text(media.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("GALAXY IPTV", color = Color(0xFF73809F), fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (episodes.isEmpty()) 1f else 0.62f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111827)),
                contentAlignment = Alignment.Center
            ) {
                if (media.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = media.imageUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0xFF43BFFF),
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(media.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (media.subtitle.isNotBlank()) {
                    Text(media.subtitle, color = Color(0xFF62C8FF), fontSize = 13.sp, maxLines = 1)
                }
                if (media.details.isNotBlank()) {
                    Text(media.details, color = Color(0xFF9BA6C4), fontSize = 11.sp, maxLines = 1)
                }
                if (media.description.isNotBlank()) {
                    Text(
                        text = media.description,
                        color = Color(0xFFD6D9E4),
                        fontSize = 12.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (episodes.isEmpty() && media.streamUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onPlay(media) },
                        modifier = Modifier.width(190.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تشغيل الفيلم", fontWeight = FontWeight.Bold)
                    }
                }
                if (isLoading) {
                    Text("جارٍ تحميل التفاصيل...", color = Color(0xFF9BA6C4), fontSize = 11.sp)
                }
                if (message != null) {
                    Text(message, color = Color(0xFFFCA5A5), fontSize = 11.sp)
                }
            }
        }

        if (episodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text("الحلقات", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.38f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(episodes, key = { "${it.id}:${it.title}" }) { episode ->
                    Surface(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .clickable { onPlay(episode) },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0C1222),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25345E))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF43BFFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(episode.title, color = Color.White, fontSize = 12.sp, maxLines = 2)
                                Text(episode.subtitle, color = Color(0xFF9BA6C4), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelPlayerLayout(
    modifier: Modifier = Modifier,
    entries: List<ContentEntry>,
    packageListMode: Boolean,
    selectedStreamUrl: String,
    onSelectEntry: (ContentEntry) -> Unit,
    playerUserAgent: String,
    playbackEngine: PlaybackEngine,
    onSelectPlaybackEngine: (PlaybackEngine) -> Unit,
    onSelectStream: (ContentEntry) -> Unit,
    onPlaybackError: (String) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(2) { columnIndex ->
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(entries.filterIndexed { index, _ -> index % 2 == columnIndex }) { entry ->
                        val originalIndex = entries.indexOf(entry)
                        val selected = entry.streamUrl == selectedStreamUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) Color(0xFF14223A) else Color(0xD908090B))
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFF36B8FF) else Color(0x442C3040),
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .clickable {
                                    if (packageListMode) onSelectEntry(entry) else onSelectStream(entry)
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (originalIndex + 1).toString().padStart(3, '0'),
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.width(30.dp)
                            )
                            Surface(
                                color = Color(0xFFFF7417),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.size(width = 19.dp, height = 15.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("M", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = entry.title,
                                color = if (selected) Color(0xFF61D0FF) else Color(0xFFE4E5EA),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .border(1.dp, Color(0xFF262838), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (packageListMode) {
                Text("اختر باقة لعرض القنوات", color = Color(0xFF8991A7), fontSize = 13.sp)
            } else if (selectedStreamUrl.isNotBlank()) {
                IptvPlayer(
                    streamUrl = selectedStreamUrl,
                    userAgent = playerUserAgent,
                    engine = playbackEngine,
                    onPlaybackError = onPlaybackError
                )
            } else {
                Text("اختر قناة للمشاهدة", color = Color(0xFF8991A7), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun IptvPlayer(
    streamUrl: String,
    onPlaybackError: (String) -> Unit,
    userAgent: String,
    engine: PlaybackEngine,
    modifier: Modifier = Modifier,
    showController: Boolean = true
) {
    if (engine == PlaybackEngine.VLC) {
        VlcPlayer(
            streamUrl = streamUrl,
            userAgent = userAgent,
            onPlaybackError = onPlaybackError,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val player = androidx.compose.runtime.remember(context, userAgent) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent.ifBlank { "Galaxy IPTV" })
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(player, streamUrl, onPlaybackError) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError(streamUrl)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, streamUrl) {
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = showController
            }
        },
        update = { it.player = player },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun VlcPlayer(
    streamUrl: String,
    userAgent: String,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val libVlc = androidx.compose.runtime.remember(context) {
        LibVLC(context, arrayListOf("--network-caching=1200"))
    }
    val player = androidx.compose.runtime.remember(libVlc) { MediaPlayer(libVlc) }

    DisposableEffect(player, libVlc, streamUrl) {
        val listener = MediaPlayer.EventListener { event ->
            if (event.type == MediaPlayer.Event.EncounteredError) {
                onPlaybackError(streamUrl)
            }
        }
        player.setEventListener(listener)
        onDispose {
            player.setEventListener(null)
            player.stop()
            player.detachViews()
            player.release()
            libVlc.release()
        }
    }

    LaunchedEffect(player, streamUrl, userAgent) {
        val media = Media(libVlc, Uri.parse(streamUrl))
        if (userAgent.isNotBlank()) media.addOption(":http-user-agent=$userAgent")
        player.media = media
        media.release()
        player.play()
    }

    AndroidView(
        factory = { viewContext ->
            VLCVideoLayout(viewContext).also { videoLayout ->
                player.attachViews(videoLayout, null, false, false)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun GalaxySettingsPage(
    account: AccountDetails?,
    preferredPlayer: PlaybackEngine,
    onPreferredPlayerChange: (PlaybackEngine) -> Unit,
    selectedSetting: String,
    onSelectSetting: (String) -> Unit,
    onHome: () -> Unit,
    onLogout: () -> Unit
) {
    val settingItems = listOf(
        "Account" to Icons.Default.Person,
        "Advanced" to Icons.Default.Tune,
        "Appearance" to Icons.Default.Palette,
        "About" to Icons.Default.Info
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.clickable(onClick = onHome)) { GalaxyWordmark() }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = 1100.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xD9070916))
                .border(1.dp, Color(0xFF18254D), RoundedCornerShape(6.dp)),
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = Color(0xFF1C2444))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settingItems.forEach { (title, icon) ->
                    SettingsNavigationItem(
                        title = title,
                        icon = icon,
                        selected = selectedSetting == title,
                        onClick = { onSelectSetting(title) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                SettingsNavigationItem(
                    title = "Logout",
                    icon = Icons.Default.Logout,
                    selected = false,
                    accent = Color(0xFFFF4F9A),
                    onClick = onLogout
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = settingItems.first { it.first == selectedSetting }.second,
                        contentDescription = null,
                        tint = Color(0xFF48BFFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(selectedSetting, color = Color(0xFFE5E9FF), fontSize = 15.sp)
                }
                Divider(color = Color(0xFF19234A), modifier = Modifier.padding(vertical = 12.dp))

                when (selectedSetting) {
                    "Account" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountDetailRow("User ID", account?.userId ?: "--")
                        AccountDetailRow("Code", account?.code ?: "--")
                        AccountDetailRow("Expires", account?.expires ?: "--")
                    }
                    "Advanced" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("مشغل الفيديو", color = Color(0xFF9BA6C4), fontSize = 11.sp)
                        PlaybackEngineSelector(
                            selected = preferredPlayer,
                            onSelect = onPreferredPlayerChange
                        )
                        AccountDetailRow("Buffer", "Normal")
                        AccountDetailRow("Updates", "Automatic")
                    }
                    "Appearance" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccountDetailRow("Theme", "Galaxy Neon")
                        AccountDetailRow("Language", "English")
                    }
                    else -> Text(
                        text = "Galaxy IPTV\nVersion 1.0",
                        color = Color(0xFFB5BEDA),
                        fontSize = 13.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        Text(
            text = "© 2025 Galaxy OTT",
            color = Color(0xFF62647B),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PlaybackEngineSelector(
    selected: PlaybackEngine,
    onSelect: (PlaybackEngine) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF080C19))
            .border(1.dp, Color(0xFF1B2852), RoundedCornerShape(6.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            PlaybackEngine.EXO_PLAYER to "ExoPlayer",
            PlaybackEngine.VLC to "VLC"
        ).forEach { (engine, label) ->
            val isSelected = engine == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color(0xFF14345A) else Color.Transparent)
                    .clickable { onSelect(engine) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color(0xFF6AD3FF) else Color(0xFF9BA6C4),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color = Color(0xFF4BBFFF),
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFF0A122B) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) accent else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = if (selected) Color.White else Color(0xFFBBC2D8), fontSize = 12.sp)
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF090C1B))
            .border(1.dp, Color(0xFF131B38), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF9BA6C4), fontSize = 11.sp)
        Text(value, color = Color(0xFFB7CBFF), fontSize = 11.sp)
    }
}

@Composable
private fun LegacyGalaxyIptvScreen() {
    val categories = listOf(
        Category("Live", "120", Color(0xFF4F46E5), Icons.Default.Tv),
        Category("Movies", "540", Color(0xFFEC4899), Icons.Default.Movie),
        Category("Series", "360", Color(0xFF10B981), Icons.Default.PlayArrow),
        Category("Sports", "98", Color(0xFFF59E0B), Icons.Default.Cast)
    )

    val channels = listOf(
        Channel("MBC 1", "News / Family", "HD", Color(0xFF22C55E)),
        Channel("BeIN Sports", "Sports", "FHD", Color(0xFFF97316)),
        Channel("FOX Movies", "Drama", "HD", Color(0xFF8B5CF6)),
        Channel("Cartoon Network", "Kids", "SD", Color(0xFF06B6D4)),
        Channel("Al Jazeera", "News", "HD", Color(0xFFEF4444)),
        Channel("National Geo", "Documentary", "4K", Color(0xFF84CC16))
    )

    val continueWatching = listOf(
        "Top Movies",
        "Latest Series",
        "Live Channels",
        "Sports Night"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "GALAXY TV",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF111827),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Live now",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color(0xFFBBF7D0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Galaxy Premiere",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drama • Action • Adventure",
                            color = Color(0xFFBFDBFE),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch now",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF111827)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = category.color.copy(alpha = 0.18f)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = category.count,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue Watching",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(continueWatching) { title ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111827),
                        modifier = Modifier
                            .width(140.dp)
                            .height(100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1F2937),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular channels",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Updated",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                channels.forEach { channel ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111827)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = channel.accent.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SmartDisplay,
                                            contentDescription = null,
                                            tint = channel.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = channel.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = channel.category,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = channel.accent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = channel.quality,
                                    color = channel.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
