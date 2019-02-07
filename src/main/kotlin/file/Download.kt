package file

import global.Log
import mu.KotlinLogging
import ui.MainWindow
import workers.DownloadWithProgressWorker
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object Download {
    private val log = KotlinLogging.logger {}

    private const val baseUrl = "grill.wurstlang.org/hudson/job/Wurst/lastSuccessfulBuild/artifact/downloads/"
    private const val bareboneUrl = "github.com/wurstscript/WurstBareboneTemplate/archive/master.zip"
    private const val compileName = "wurstpack_compiler.zip"

    @Throws(IOException::class)
    private fun downloadFile(filePath: String, callback: (Path) -> Unit) {
        if (SetupApp.setup.silent) {
            downloadDirect(filePath, callback)
        } else {
            DownloadWithProgressWorker(filePath, MainWindow.ui.progressBar, callback).execute()
        }
    }

    @Throws(IOException::class)
    fun downloadSetup(callback: (Path) -> Unit) {
        try {
            downloadFile("https://grill.wurstlang.org/hudson/job/WurstSetup/lastSuccessfulBuild/artifact/downloads/WurstSetup.jar", callback)
        } catch (e: Exception) {
            log.warn( "downloadCompiler Exception caught", e)
            Log.println("Https error, falling back to unsafe http.")
            downloadFile("http://grill.wurstlang.org/hudson/job/WurstSetup/lastSuccessfulBuild/artifact/downloads/WurstSetup.jar", callback)
        }
    }

    @Throws(IOException::class)
    fun downloadCompiler(callback: (Path) -> Unit) {
        try {
            downloadFile("https://$baseUrl$compileName", callback)
        } catch (e: Exception) {
            log.warn( "downloadCompiler Exception caught", e)
            Log.println("Https error, falling back to unsafe http.")
            downloadFile("http://$baseUrl$compileName", callback)
        }
    }

    @Throws(IOException::class)
    fun downloadBareboneProject(callback: (Path) -> Unit) {
        try {
            downloadFile("https://$bareboneUrl", callback)
        } catch (e: Exception) {
            log.warn( "downloadBareboneProject Exception caught", e)
            Log.println("Https error, falling back to unsafe http.")
            downloadFile("http://$bareboneUrl", callback)
        }
    }

    private fun downloadDirect(filePath: String, callback: (Path) -> Unit) {
        val url = URL(filePath)
        val httpConnection = url.openConnection() as HttpURLConnection
		httpConnection.connectTimeout = 14000
		httpConnection.readTimeout = 20000
		httpConnection.addRequestProperty("User-Agent", "Mozilla/4.76")
		val completeFileSize = httpConnection.contentLength
		val size = completeFileSize / 1024 / 1024
		Log.print("(" + (if (size == 0) "<1" else size) + "MB)")
		val input = java.io.BufferedInputStream(httpConnection.inputStream)
		var substring = filePath.substring(filePath.lastIndexOf("/") + 1)
		if (Files.exists(Paths.get(substring))) {
			substring += ".2.jar"
		}

		val fos = java.io.FileOutputStream(substring)
		val bout = BufferedOutputStream(fos, 1024)
		val data = ByteArray(1024)
		var downloadedFileSize: Long = 0
		var x = input.read(data, 0, 1024)
		do {
			downloadedFileSize += x.toLong()
			bout.write(data, 0, x)
			x = input.read(data, 0, 1024)
		} while (x >= 0)
		bout.close()
		input.close()
		fos.close()
        callback.invoke(Paths.get(substring))
    }
}
