import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dna_mock.ui.theme.BackgroundColor
import com.example.dna_mock.ui.theme.BrandColor

@Composable
fun AccountScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
        Box(contentAlignment = Alignment.Center) {
            Text("Account Screen", color = BrandColor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}