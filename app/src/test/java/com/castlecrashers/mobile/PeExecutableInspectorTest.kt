package com.castlecrashers.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class PeExecutableInspectorTest {
    @Test
    fun detectsWindowsX86Pe32() {
        val file = fakePe(machine = 0x014C, magic = 0x010B)
        try {
            val info = PeExecutableInspector.inspect(file)
            assertEquals("x86", info.architecture)
            assertEquals(32, info.bits)
            assertEquals(0x014C, info.machine)
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsMachineAndOptionalHeaderMismatch() {
        val file = fakePe(machine = 0x014C, magic = 0x020B)
        try {
            assertThrows(IllegalArgumentException::class.java) {
                PeExecutableInspector.inspect(file)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsNonPeFile() {
        val file = File.createTempFile("not-pe", ".bin")
        try {
            file.writeBytes(ByteArray(128))
            assertThrows(IllegalArgumentException::class.java) {
                PeExecutableInspector.inspect(file)
            }
        } finally {
            file.delete()
        }
    }

    private fun fakePe(machine: Int, magic: Int): File {
        val file = File.createTempFile("castle-pe", ".exe")
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(256)
            raf.seek(0); raf.write('M'.code); raf.write('Z'.code)
            raf.seek(0x3C); writeU32(raf, 0x80)
            raf.seek(0x80); writeU32(raf, 0x00004550)
            raf.seek(0x84); writeU16(raf, machine)
            raf.seek(0x98); writeU16(raf, magic)
        }
        return file
    }

    private fun writeU16(raf: RandomAccessFile, value: Int) {
        raf.write(value and 0xFF); raf.write((value ushr 8) and 0xFF)
    }

    private fun writeU32(raf: RandomAccessFile, value: Int) {
        repeat(4) { raf.write((value ushr (it * 8)) and 0xFF) }
    }
}
