package uz.muhammadyusuf.kurbonov.zhmodmanager.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo


class ModManager {
    private val currentDir = File(System.getProperty("user.dir"))
    private val logFile = File(currentDir, "MOD_INSTALLED_FILES.log")
    private val mutex = Mutex()


    fun listMods(): List<String> {
        return currentDir.resolve("Mods").list { dir, _ -> dir.isDirectory }?.toList() ?: emptyList()
    }

    private fun listModFiles(modName: String): List<String> {
        val modDir = currentDir.resolve("Mods").resolve(modName)
        val resultList = mutableListOf<String>()

        try {
            Files.walk(modDir.toPath()).use { stream ->
                stream
                    .filter(Files::isRegularFile)
                    .forEach {
                        resultList.add(it.absolutePathString())
                    }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return resultList
    }

    suspend fun installMod(modName: String) = withContext(Dispatchers.IO) {
        if (File(currentDir, "MOD_INSTALLED_FILES.log").exists())
            throw IllegalStateException("Uninstall mod first!")

        logFile.createNewFile()

        val writer = logFile.bufferedWriter()

        suspend fun installFile(modName: String, absolutePath: String) = withContext(Dispatchers.IO) {
            val sourceFile = File(absolutePath)
            val relativePath = Path(absolutePath).relativeTo(currentDir.resolve("Mods").resolve(modName).toPath())
            val destination = File(currentDir, relativePath.pathString)
            destination.parentFile.mkdirs()
            mutex.withLock {
                writer.appendLine(relativePath.pathString)
            }

            if (destination.exists()) {
                destination.renameTo(File(destination.path + ".backup"))
            }

            Files.move(sourceFile.toPath(), destination.toPath())
        }

        mutex.withLock {
            writer.appendLine(modName)
        }
        val modFiles = listModFiles(modName)

        modFiles.map { fileName -> async { installFile(modName, fileName) } }.awaitAll()

        writer.close()
    }

    suspend fun uninstallMod() = withContext(Dispatchers.IO) {
        if (!logFile.exists())
            throw IllegalStateException("Install mod first!")

        logFile.bufferedReader().use {
            val lines = it.lineSequence().toList()

            val modName = lines.first()
            val modFiles = lines.drop(1).toList()

            modFiles.map { fileName -> async { uninstallFile(modName, fileName) } }.awaitAll()
        }

        logFile.delete()
    }

    private suspend fun uninstallFile(modName: String, relative: String) = withContext(Dispatchers.IO) {
        val sourceFile = File(currentDir, relative)
        val destination = currentDir.resolve("Mods").resolve(modName).resolve(relative)
        destination.parentFile.mkdirs()
        Files.move(sourceFile.toPath(), destination.toPath())

        if (File(sourceFile.path + ".backup").exists()) {
            File(sourceFile.path + ".backup").renameTo(File(sourceFile.path))
        }
    }

    fun currentInstalledMod(): String? {
        if (!logFile.exists())
            return null

        return logFile.bufferedReader().use {
            it.lineSequence().first()
        }
    }

    fun checkCurrentDir(): Boolean {
        return currentDir.resolve("generals.exe").exists() && currentDir.resolve("game.dat").exists()
    }
}