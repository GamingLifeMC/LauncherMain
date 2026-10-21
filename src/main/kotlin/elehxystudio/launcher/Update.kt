package elehxystudio.launcher

import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

object Update {

    private const val REPO_ZIP =
        "https://github.com/GamingLifeMC/LauncherMain/archive/refs/heads/main.zip"

    private const val SOURCE_PATH =
        "src/main/kotlin/elehxystudio/launcher/Main/"

    @Throws(IOException::class)
    fun downloadMain(targetDirectory: Path) {
        val tempDirectory = Files.createTempDirectory("gaminglife-update-")
        val zipFile = tempDirectory.resolve("repository.zip")
        val extractedDirectory = tempDirectory.resolve("extracted")

        try {
            Files.createDirectories(extractedDirectory)

            URI(REPO_ZIP).toURL().openStream().use { input ->
                Files.copy(input, zipFile)
            }

            var foundMain = false

            ZipInputStream(Files.newInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry

                while (entry != null) {
                    val normalizedName = entry.name.replace('\\', '/')
                    val marker = "/$SOURCE_PATH"

                    val markerIndex = normalizedName.indexOf(marker)

                    if (markerIndex >= 0) {
                        foundMain = true
                    }

                    val output = extractedDirectory.resolve(normalizedName).normalize()

                    if (!output.startsWith(extractedDirectory)) {
                        throw IOException("Ungültiger Pfad im GitHub-ZIP.")
                    }

                    if (entry.isDirectory) {
                        Files.createDirectories(output)
                    } else {
                        Files.createDirectories(output.parent)
                        Files.copy(zip, output, StandardCopyOption.REPLACE_EXISTING)
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (!foundMain) {
                throw IOException(
                    "Der Ordner wurde nicht gefunden: $SOURCE_PATH"
                )
            }

            val repositoryRoot = Files.list(extractedDirectory).use { paths ->
                paths.findFirst().orElseThrow {
                    IOException("Das GitHub-Archiv ist leer.")
                }
            }

            val sourceDirectory = repositoryRoot.resolve(
                SOURCE_PATH.removeSuffix("/")
            )

            if (!Files.isDirectory(sourceDirectory)) {
                throw IOException(
                    "Der erwartete Main-Ordner existiert nicht: $sourceDirectory"
                )
            }

            Files.createDirectories(targetDirectory)

            Files.walk(sourceDirectory).use { paths ->
                paths.forEach { source ->
                    val relative = sourceDirectory.relativize(source)
                    val destination = targetDirectory.resolve(relative)

                    if (Files.isDirectory(source)) {
                        Files.createDirectories(destination)
                    } else {
                        Files.createDirectories(destination.parent)
                        Files.copy(
                            source,
                            destination,
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                }
            }

            println("GamingLife Main-Quellordner erfolgreich heruntergeladen.")

        } finally {
            Files.walk(tempDirectory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path ->
                    Files.deleteIfExists(path)
                }
            }
        }
    }
}


fun main() {
    try {
        val target = java.nio.file.Path.of("Main")
        Update.downloadMain(target)
        println("Update erfolgreich!")
    } catch (e: Exception) {
        println("Updater-Fehler: ${e.message}")
        e.printStackTrace()
    }
}