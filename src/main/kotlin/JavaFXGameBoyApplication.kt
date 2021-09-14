import javafx.application.Application
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class JavaFXGameBoyApplication : Application() {
    private lateinit var emulator: Emulator

    override fun start(primaryStage: Stage?) {
        if (primaryStage == null) {
            throw RuntimeException()
        }
        val romFile = chooseROMFile(primaryStage)
        if (romFile == null) {
            primaryStage.close()
            return
        }

        emulator = Emulator()
        emulator.start(romFile)
    }

    override fun stop() {
    }

    private fun chooseROMFile(primaryStage: Stage): File? {
        val fileChooser = FileChooser()
        fileChooser.title = "Open ROM File"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("GB Files", "*.gb"),
            FileChooser.ExtensionFilter("All Files", "*.*")
        )
        return fileChooser.showOpenDialog(primaryStage)
    }
}
