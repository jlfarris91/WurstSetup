package file

import global.InstallationManager
import global.Log
import ui.MainWindow
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import javax.swing.JOptionPane


/**
 * Created by Frotty on 10.07.2017.
 */

data class WurstProjectConfigData(var projectName: String = "DefaultName",
                                  val dependencies: MutableList<String> = Arrays.asList("https://github.com/wurstscript/wurstStdlib2"),
                                  val buildMapData: WurstProjectBuildMapData = WurstProjectBuildMapData())

data class WurstProjectBuildMapData(val name: String = "DefaultName",
                                    val fileName: String = "DefaultFileName",
                                    val author: String = "DefaultAuthor")

object WurstProjectConfig {

    fun handleCreate(projectRoot: Path, gameRoot: Path?, projectConfig: WurstProjectConfigData) {
        try {
            createProject(projectRoot, gameRoot, projectConfig)
            MainWindow.ui!!.refreshComponents()
        } catch (e: Exception) {
            Log.print("\n===ERROR PROJECT CREATE===\n" + e.message + "\nPlease report here: github.com/wurstscript/WurstScript/issues\n")
        }

    }


    @Throws(IOException::class)
    fun loadProject(buildFile: Path): WurstProjectConfigData? {
        Log.print("Loading project..")
        if (Files.exists(buildFile) && buildFile.fileName.toString().equals("wurst.build", ignoreCase = true)) {
            val config = YamlHelper.loadProjectConfig(buildFile)
            val projectRoot = buildFile.parent
            if (config.projectName.isEmpty()) {
                config.projectName = projectRoot!!.fileName.toString()
                saveProjectConfig(projectRoot, config)
            }
            Log.print("done\n")
            return config
        }
        return null
    }

    @Throws(Exception::class)
    private fun createProject(projectRoot: Path, gameRoot: Path?, projectConfig: WurstProjectConfigData) {
        Log.print("Creating project root..")
        if (Files.exists(projectRoot)) {
            Log.print("\nError: Project root already exists!\n")
        } else {
            Files.createDirectories(projectRoot)
            Log.print("done\n")

            Log.print("Download template..")
            val zipFile = Download.downloadBareboneProject()
            Log.print("done\n")

            Log.print("Extracting template..")
            val extractSuccess = ZipArchiveExtractor.extractArchive(zipFile, projectRoot)

            if (extractSuccess) {
                Log.print("done\n")
                Log.print("Clean up..")
                val folder = projectRoot.resolve("WurstBareboneTemplate-master")
                copyFolder(folder, projectRoot)
                Files.walk(folder).sorted { a, b -> b.compareTo(a) }.
                        forEach { p ->
                            try {
                                Files.delete(p)
                            } catch (e: IOException) {
                            }
                        }
            } else {
                Log.print("error\n")
                JOptionPane.showMessageDialog(null,
                        "Error: Cannot extract patch files.\nWurst might still be in use.\nClose any Wurst, VSCode or Eclipse instances before updating.",
                        "Error Massage", JOptionPane.ERROR_MESSAGE)
            }

            Log.print("done\n")

            setupVSCode(projectRoot, gameRoot)

            saveProjectConfig(projectRoot, projectConfig)

            DependencyManager.updateDependencies(projectRoot, projectConfig)

            Log.print("---\n\n")
            if (gameRoot == null || !Files.exists(gameRoot)) {
                Log.print("Warning: Your game path has not been set.\nThis means you will be able to develop, but not run maps.\n")
            }
            Log.print("Your project has been successfully created!\n" + "You can now open your project folder in VSCode.\nOpen the wurst/Hello.wurst package to continue.\n")
        }
    }

    fun copyFolder(src: Path, dest: Path) {
        try {
            Files.walk(src)
                    .forEach { s ->
                        try {
                            val d = dest.resolve(src.relativize(s))
                            if (Files.isDirectory(s)) {
                                if (!Files.exists(d))
                                    Files.createDirectory(d)
                            } else {
                                Files.copy(s, d)// use flag to override existing
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun saveProjectConfig(projectRoot: Path, projectConfig: WurstProjectConfigData) {
        val projectYaml = YamlHelper.dumpProjectConfig(projectConfig)
        Files.write(projectRoot.resolve("wurst.build"), projectYaml.toByteArray())
    }

    @Throws(IOException::class)
    private fun setupVSCode(projectRoot: Path?, gamePath: Path?) {
        Log.print("Updating vsconfig..")
        if (!Files.exists(projectRoot)) {
            throw IOException("Project root does not exist!")
        }
        val vsCode = projectRoot!!.resolve(".vscode/settings.json")
        if (!Files.exists(vsCode)) {
            Files.createDirectories(vsCode.parent)
            Files.write(vsCode, VSCODE_MIN_CONFIG.toByteArray(), StandardOpenOption.CREATE_NEW)
        }
        var json = String(Files.readAllBytes(vsCode))
        val absolutePath = InstallationManager.compilerJar.toAbsolutePath().toString()
        json = json.replace("%wurstjar%", absolutePath.replace("\\\\".toRegex(), "\\\\\\\\"))

        if (gamePath != null && Files.exists(gamePath)) {
            json = json.replace("%gamepath%", gamePath.toAbsolutePath().toString().replace("\\\\".toRegex(), "\\\\\\\\"))
        }
        Files.write(vsCode, json.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        Log.print("done.\n")
    }

    fun handleUpdate(projectRoot: Path, gamePath: Path?, config: WurstProjectConfigData) {
        Log.print("Updating project...\n")
        try {
            setupVSCode(projectRoot, gamePath)
            saveProjectConfig(projectRoot, config)
            DependencyManager.updateDependencies(projectRoot, config)

            Log.print("Project successfully updated!\nReload vscode to apply the changed dependencies.\n")
            MainWindow.ui!!.refreshComponents()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.print("\n===ERROR PROJECT UPDATE===\n" + e.message + "\nPlease report here: github.com/wurstscript/WurstScript/issues\n")
        }

    }

    private val VSCODE_MIN_CONFIG = "{\n" +
            "    \"wurst.wurstJar\": \"%wurstjar%\",\n" +
            "    \"wurst.wc3path\": \"%gamepath%\"\n" +
            "}"
}