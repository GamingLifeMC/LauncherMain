package elehxystudio.launcher

import elehxystudio.launcher.Main.Launcher.Main
import javafx.application.Application
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.zip.ZipInputStream

object Update {

    private const val REPOSITORY =
        "https://github.com/GamingLifeMC/LauncherMain"

    private const val BRANCH = "main"

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun startLauncher() {
        val projectDirectory = Path.of(".")
            .toAbsolutePath()
            .normalize()

        println("GamingLife Updater startet ...")

        try {
            updateMainFolder(projectDirectory)
            println("GamingLife wurde aktualisiert.")
        } catch (e: Exception) {
            System.err.println(
                "Updater-Warnung: ${e.message}"
            )
            System.err.println(
                "Der vorhandene Launcher wird trotzdem gestartet."
            )
        }

        println("Starte GamingLife Launcher ...")
        Application.launch(Main::class.java)
    }

    private fun updateMainFolder(projectDirectory: Path) {
        val zipFile = projectDirectory.resolve(
            ".gaminglife-update.zip"
        )

        val stagingDirectory = projectDirectory.resolve(
            ".gaminglife-main-staging"
        )

        val mainDirectory = projectDirectory.resolve("Main")

        val backupDirectory = projectDirectory.resolve(
            ".gaminglife-main-backup"
        )

        val zipUrl =
            "$REPOSITORY/archive/refs/heads/$BRANCH.zip"

        try {
            println("Lade GitHub-Repository herunter ...")

            downloadFile(zipUrl, zipFile)

            deleteDirectory(stagingDirectory)
            Files.createDirectories(stagingDirectory)

            extractMainFolder(zipFile, stagingDirectory)

            val files = Files.walk(stagingDirectory).use { stream ->
                stream.filter { Files.isRegularFile(it) }.count()
            }

            if (files == 0L) {
                throw IOException(
                    "Der Main-Ordner enthält keine Dateien."
                )
            }

            println(
                "Download geprüft: $files Dateien gefunden."
            )

            deleteDirectory(backupDirectory)

            var oldMainMoved = false

            try {
                if (Files.exists(mainDirectory)) {
                    moveDirectory(mainDirectory, backupDirectory)
                    oldMainMoved = true
                }

                moveDirectory(stagingDirectory, mainDirectory)

                deleteDirectory(backupDirectory)

                println(
                    "Main-Ordner erfolgreich aktualisiert."
                )
            } catch (e: Exception) {
                if (
                    oldMainMoved &&
                    Files.exists(backupDirectory) &&
                    !Files.exists(mainDirectory)
                ) {
                    moveDirectory(backupDirectory, mainDirectory)
                }

                throw e
            }
        } finally {
            deleteDirectory(zipFile)
            deleteDirectory(stagingDirectory)
        }
    }

    private fun downloadFile(
        url: String,
        destination: Path
    ) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMinutes(3))
            .header("User-Agent", "GamingLifeLauncher")
            .header(
                "Accept",
                "application/vnd.github+json"
            )
            .GET()
            .build()

        val response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofFile(destination)
        )

        if (response.statusCode() !in 200..299) {
            deleteDirectory(destination)

            throw IOException(
                "GitHub-Download fehlgeschlagen: HTTP " +
                        response.statusCode()
            )
        }
    }

    private fun extractMainFolder(
        zipFile: Path,
        destination: Path
    ) {
        var foundMainFolder = false

        ZipInputStream(
            Files.newInputStream(zipFile)
        ).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break

                val entryName = entry.name
                    .replace('\\', '/')
                    .trimEnd('/')

                val parts = entryName.split('/')
                val mainIndex = parts.indexOf("Main")

                if (mainIndex >= 0) {
                    foundMainFolder = true

                    val relativeParts = parts.drop(mainIndex + 1)

                    if (relativeParts.isNotEmpty()) {
                        val relativePath =
                            relativeParts.joinToString("/")

                        val outputFile = destination
                            .resolve(relativePath)
                            .normalize()

                        if (!outputFile.startsWith(destination)) {
                            throw IOException(
                                "Ungültiger Dateipfad im ZIP."
                            )
                        }

                        if (entry.isDirectory) {
                            Files.createDirectories(outputFile)
                        } else {
                            outputFile.parent?.let {
                                Files.createDirectories(it)
                            }

                            Files.copy(
                                zip,
                                outputFile,
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    }
                }

                zip.closeEntry()
            }
        }

        if (!foundMainFolder) {
            throw IOException(
                "Kein Ordner namens Main im GitHub-ZIP gefunden. " +
                        "Bitte Repository-Struktur prüfen."
            )
        }
    }

    private fun moveDirectory(
        source: Path,
        destination: Path
    ) {
        try {
            Files.move(
                source,
                destination,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(source, destination)
        }
    }

    private fun deleteDirectory(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path).use { stream ->
            stream
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}

fun main() {
    try {
        Update.startLauncher()
    } catch (e: Exception) {
        System.err.println(
            "Launcher-Fehler: ${e.message}"
        )
        e.printStackTrace()
    }
}