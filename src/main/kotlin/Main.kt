import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import uz.muhammadyusuf.kurbonov.zhmodmanager.core.ModManager

@Composable
@Preview
fun App() {
    val modManager = ModManager()

    var modSelectExpanded by remember { mutableStateOf(false) }
    val currentMod = remember { mutableStateOf<String?>(null) }
    val mods = produceState<List<String>?>(null) {
        value = modManager.listMods()
    }
    val isCurrentDirValid by produceState<Boolean?>(null) {
        value = modManager.checkCurrentDir()
    }

    fun checkCurrentMod() {
        currentMod.value = modManager.currentInstalledMod() ?: "Select mod to Install!"
    }

    LaunchedEffect(Unit) {
        checkCurrentMod()
    }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isCurrentDirValid != true) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.Red.copy(alpha = 0.4f)).padding(8.dp)) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart),
                        text = if (isCurrentDirValid == null) "Checking dir..," else "!!! Invalid dir. run from Game dir",
                        color = Color.White
                    )
                }
            }
            Box {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { modSelectExpanded = true }) {
                    Text(
                        text = currentMod.value ?: "Loading..."
                    )
                }
                DropdownMenu(
                    expanded = modSelectExpanded,
                    onDismissRequest = { modSelectExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp)
                ) {
                    if (mods.value == null) {
                        Text("Searching in Mods dir...")
                        return@DropdownMenu
                    }

                    if (mods.value?.isEmpty() == true) {
                        Text("No mods found in Mods dir...")
                    }

                    mods.value?.forEach { mod ->
                        DropdownMenuItem(
                            onClick = {
                                scope.launch {
                                    currentMod.value = null
                                    modManager.installMod(mod)
                                    checkCurrentMod()
                                    modSelectExpanded = false
                                }
                            }
                        ) {
                            Text(mod)
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = currentMod.value != "Select mod to Install!",
                onClick = {
                    scope.launch { modManager.uninstallMod() }
                }) {
                Text(
                    text = "Uninstall current mod: ${currentMod.value}"
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ZH MOD Manager") {
        App()
    }
}
