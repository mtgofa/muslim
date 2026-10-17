package com.mtgofa.muslem.prayer

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtgofa.muslem.R
import com.mtgofa.muslem.data.Settings
import com.mtgofa.muslem.ui.MuslemTheme
import java.time.LocalDate

/** Shown over the lock screen while the adhan plays; any volume key or the button stops it. */
class AdhanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Settings.init(this)
        setContent {
            MuslemTheme {
                val s by AdhanService.sounding.collectAsState()
                LaunchedEffect(s) { if (s == null) finish() }
                val p = s?.prayer
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF14936F), Color(0xFF07382D)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.size(120.dp).background(Color(0x22FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_stat_mosque), null, tint = Color.White, modifier = Modifier.size(64.dp))
                        }
                        Spacer(Modifier.height(28.dp))
                        Text("حان الآن موعد", color = Color(0xCCFFFFFF), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (p != null) "صلاة ${p.label(LocalDate.now(Settings.zone))}" else (s?.title ?: ""),
                            color = Color.White, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(Settings.locationName, color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(48.dp))
                        Button(
                            onClick = { AdhanService.stop(this@AdhanActivity); finish() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0A5A48)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text("إيقاف الأذان", style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AdhanService.stop(this); finish(); return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
