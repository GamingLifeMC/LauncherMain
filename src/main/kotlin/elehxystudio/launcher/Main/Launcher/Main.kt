package elehxystudio.launcher.Main.Launcher

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage

class Main : Application() {

    override fun start(stage: Stage) {
        val title = Label("GamingLife")
        title.style = """
            -fx-font-size: 34px;
            -fx-font-weight: bold;
            -fx-text-fill: white;
        """.trimIndent()

        val subtitle = Label("Hardcore Roleplay")
        subtitle.style = """
            -fx-font-size: 15px;
            -fx-text-fill: #aab4c5;
        """.trimIndent()

        val playButton = Button("SPIELEN")
        playButton.prefWidth = 220.0
        playButton.prefHeight = 48.0
        playButton.style = """
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-background-color: #20a7e8;
            -fx-text-fill: white;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
        """.trimIndent()

        playButton.setOnAction {
            println("GamingLife Launcher funktioniert!")
        }

        val layout = VBox(16.0, title, subtitle, playButton)
        layout.alignment = Pos.CENTER
        layout.padding = Insets(30.0)
        layout.style = "-fx-background-color: #151922;"

        stage.title = "GamingLife Launcher"
        stage.scene = Scene(layout, 600.0, 400.0)
        stage.minWidth = 500.0
        stage.minHeight = 350.0
        stage.show()
    }
}