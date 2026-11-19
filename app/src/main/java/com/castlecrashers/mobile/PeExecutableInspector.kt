package com.castlecrashers.mobile

import java.io.File
import java.io.RandomAccessFile

/** Minimal PE/COFF inspection so the launcher never guesses the Windows target architecture. */
object PeExecutableInspector {
    data class Info(
        val machine: Int,
        val optionalHeaderMagic: Int,
        val architecture: String,
        val bits: Int
    )

    fun inspect(file: File): Info {
        require(file.isFile) { "Executável não encontrado: ${file.absolutePath}" }
        RandomAccessFile(file, "r").use { raf ->
            require(raf.length() >= 0x40) { "Arquivo pequeno demais para ser um executável PE." }
            require(readU16(raf, 0) == 0x5A4D) { "O arquivo não possui assinatura DOS MZ." }
            val peOffset = readU32(raf, 0x3C).toLong()
            require(peOffset >= 0x40 && peOffset + 26 <= raf.length()) { "Cabeçalho PE inválido." }
            require(readU32(raf, peOffset) == 0x00004550L) { "Assinatura PE não encontrada." }
            val machine = readU16(raf, peOffset + 4)
            val optionalMagic = readU16(raf, peOffset + 24)
            val bits = when (optionalMagic) {
                0x10B -> 32
                0x20B -> 64
                else -> throw IllegalArgumentException("Formato PE opcional desconhecido: 0x${optionalMagic.toString(16)}")
            }
            val architecture = when (machine) {
                0x014C -> "x86"
                0x8664 -> "x86-64"
                0xAA64 -> "ARM64"
                0x01C4 -> "ARMv7"
                else -> "desconhecida (0x${machine.toString(16)})"
            }
            require((machine == 0x014C && bits == 32) ||
                    (machine == 0x8664 && bits == 64) ||
                    (machine == 0xAA64 && bits == 64) ||
                    (machine == 0x01C4 && bits == 32)) {
                "Combinação de arquitetura PE inconsistente: $architecture/$bits-bit."
            }
            return Info(machine, optionalMagic, architecture, bits)
        }
    }

    private fun readU16(raf: RandomAccessFile, offset: Long): Int {
        raf.seek(offset)
        val a = raf.read()
        val b = raf.read()
        require(a >= 0 && b >= 0) { "Cabeçalho PE truncado." }
        return a or (b shl 8)
    }

    private fun readU32(raf: RandomAccessFile, offset: Long): Long {
        raf.seek(offset)
        var value = 0L
        repeat(4) { index ->
            val byte = raf.read()
            require(byte >= 0) { "Cabeçalho PE truncado." }
            value = value or (byte.toLong() shl (index * 8))
        }
        return value
    }
}
