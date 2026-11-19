package com.castlecrashers.mobile

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

/** Copies a selected game directory safely, preserving the previous install until validation succeeds. */
object GameFolderInstaller {
    data class Result(val root: File, val exe: File, val filesCopied: Int, val bytesCopied: Long)

    fun install(context: Context, treeUri: Uri, destination: File, progress: (String) -> Unit): Result {
        val source = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Não foi possível abrir a pasta selecionada.")
        require(source.isDirectory) { "A origem não é uma pasta." }

        val parent = destination.parentFile ?: throw IllegalStateException("Destino de instalação inválido.")
        if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Não foi possível criar a pasta de instalação.")
        val staging = File(parent, destination.name + ".importing")
        val backup = File(parent, destination.name + ".previous")
        staging.deleteRecursively()
        backup.deleteRecursively()
        if (!staging.mkdirs()) throw IllegalStateException("Não foi possível preparar a importação.")

        var files = 0
        var bytes = 0L
        try {
            fun copyDirectory(from: DocumentFile, to: File) {
                for (child in from.listFiles()) {
                    val name = child.name ?: continue
                    if (name == "." || name == ".." || name.contains('/') || name.contains('\\')) continue
                    val target = File(to, name)
                    if (child.isDirectory) {
                        if (!target.exists() && !target.mkdirs()) throw IllegalStateException("Falha ao criar: $name")
                        copyDirectory(child, target)
                    } else if (child.isFile) {
                        context.contentResolver.openInputStream(child.uri)?.use { input ->
                            FileOutputStream(target).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                    bytes += count
                                }
                            }
                        } ?: throw IllegalStateException("Não foi possível ler: $name")
                        files++
                        if (files % 40 == 0) progress("Copiados $files arquivos • ${bytes / (1024 * 1024)} MB")
                    }
                }
            }

            copyDirectory(source, staging)
            val exe = staging.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
                ?: throw IllegalStateException("Castle.exe não foi encontrado na pasta importada.")
            val pe = PeExecutableInspector.inspect(exe)
            require(pe.machine == 0x014C && pe.bits == 32) {
                "O Castle.exe selecionado é ${pe.architecture}/${pe.bits}-bit; esta versão está preparada para validar o Castle Crashers x86."
            }

            if (destination.exists() && !destination.renameTo(backup)) {
                throw IllegalStateException("Não foi possível preservar a instalação anterior.")
            }
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                throw IllegalStateException("Não foi possível finalizar a instalação importada.")
            }
            backup.deleteRecursively()
            val installedExe = destination.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
                ?: throw IllegalStateException("O executável desapareceu após a instalação.")
            progress("Importação validada: $files arquivos • ${bytes / (1024 * 1024)} MB • PE x86/32-bit")
            return Result(destination, installedExe, files, bytes)
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (!destination.exists() && backup.exists()) backup.renameTo(destination)
            throw error
        }
    }    
    fun installZip(context: Context, archiveUri: Uri, destination: File, progress: (String) -> Unit): Result {
        val parent = destination.parentFile ?: throw IllegalStateException("Destino de instalação inválido.")
        if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Não foi possível criar a pasta de instalação.")
        val staging = File(parent, destination.name + ".importing")
        val backup = File(parent, destination.name + ".previous")
        staging.deleteRecursively()
        backup.deleteRecursively()
        if (!staging.mkdirs()) throw IllegalStateException("Não foi possível preparar a extração.")

        var files = 0
        var bytes = 0L
        try {
            val input = context.contentResolver.openInputStream(archiveUri)
                ?: throw IllegalArgumentException("Não foi possível abrir o arquivo ZIP.")
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val normalized = entry.name.replace('\\\\', '/')
                    val segments = normalized.split('/')
                    require(!normalized.startsWith("/") && segments.none { it == ".." || it.isEmpty() && normalized.startsWith("/") }) {
                        "O ZIP contém um caminho inválido."
                    }
                    val target = File(staging, normalized).canonicalFile
                    val rootPath = staging.canonicalPath + File.separator
                    require(target.path.startsWith(rootPath) || target == staging.canonicalFile) {
                        "O ZIP tenta gravar fora da pasta de instalação."
                    }
                    if (entry.isDirectory) {
                        if (!target.exists() && !target.mkdirs()) throw IllegalStateException("Falha ao criar pasta extraída.")
                    } else {
                        target.parentFile?.let {
                            if (!it.exists() && !it.mkdirs()) throw IllegalStateException("Falha ao preparar pasta extraída.")
                        }
                        FileOutputStream(target).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                bytes += count
                                require(bytes <= 1024L * 1024L * 1024L) { "O conteúdo descompactado excede o limite de segurança de 1 GB." }
                                output.write(buffer, 0, count)
                            }
                        }
                        files++
                        require(files <= 20000) { "O ZIP contém mais de 20.000 arquivos." }
                        if (files % 40 == 0) progress("Extraídos $files arquivos • ${bytes / (1024 * 1024)} MB")
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val exe = staging.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
                ?: throw IllegalStateException("Castle.exe não foi encontrado dentro do ZIP.")
            val pe = PeExecutableInspector.inspect(exe)
            require(pe.machine == 0x014C && pe.bits == 32) {
                "O Castle.exe selecionado é ${pe.architecture}/${pe.bits}-bit; esta versão espera Windows x86/32-bit."
            }

            if (destination.exists() && !destination.renameTo(backup)) {
                throw IllegalStateException("Não foi possível preservar a instalação anterior.")
            }
            if (!staging.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                throw IllegalStateException("Não foi possível finalizar a instalação extraída.")
            }
            backup.deleteRecursively()
            val installedExe = destination.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
                ?: throw IllegalStateException("O executável desapareceu após a extração.")
            progress("ZIP validado: $files arquivos • ${bytes / (1024 * 1024)} MB • PE x86/32-bit")
            return Result(destination, installedExe, files, bytes)
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (!destination.exists() && backup.exists()) backup.renameTo(destination)
            throw error
        }
    }

}
