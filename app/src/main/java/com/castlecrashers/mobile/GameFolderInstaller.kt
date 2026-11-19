package com.castlecrashers.mobile

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

/** Copies a selected game directory into app-private storage for a future Windows runtime. */
object GameFolderInstaller {
    data class Result(val root: File, val exe: File, val filesCopied: Int, val bytesCopied: Long)

    fun install(context: Context, treeUri: Uri, destination: File, progress: (String) -> Unit): Result {
        val source = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Não foi possível abrir a pasta selecionada.")
        require(source.isDirectory) { "A origem não é uma pasta." }
        if (destination.exists() && !destination.deleteRecursively()) {
            throw IllegalStateException("Não foi possível limpar a importação anterior.")
        }
        if (!destination.mkdirs()) throw IllegalStateException("Não foi possível criar a pasta do jogo.")

        var files = 0
        var bytes = 0L
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
                    if (files % 40 == 0) progress("Copiados " + files + " arquivos • " + (bytes / (1024 * 1024)) + " MB")
                }
            }
        }
        copyDirectory(source, destination)
        val exe = destination.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
            ?: throw IllegalStateException("Castle.exe não foi encontrado na pasta importada.")
        progress("Importação concluída: " + files + " arquivos • " + (bytes / (1024 * 1024)) + " MB")
        return Result(destination, exe, files, bytes)
    }
}
