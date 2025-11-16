package com.example.sodam_diary.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * PCM 오디오 데이터를 WAV 파일로 저장하는 유틸리티
 * Naver CLOVA STT의 onRecord() 콜백에서 받은 short[] 데이터를 변환 없이 저장
 * 
 * WAV 파일 포맷:
 * - 44 byte 헤더 (RIFF, fmt, data 청크)
 * - 16-bit PCM, Little Endian
 * - 16000Hz, Mono
 */
class AudioWriterWAV(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioWriterWAV"
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
    }
    
    private var outputStream: FileOutputStream? = null
    private var outputFile: File? = null
    private var dataSize = 0
    
    /**
     * WAV 파일 생성 및 헤더 예약
     * @param filename 파일명 (확장자 제외)
     * @return 생성된 파일의 절대 경로
     */
    fun open(filename: String): String? {
        try {
            // 저장 디렉토리 생성
            val voicesDir = File(context.filesDir, "voices")
            if (!voicesDir.exists()) {
                voicesDir.mkdirs()
            }
            
            // 파일 경로 생성
            val timestamp = System.currentTimeMillis()
            outputFile = File(voicesDir, "${filename}_${timestamp}.wav")
            
            // 파일 스트림 열기
            outputStream = FileOutputStream(outputFile!!)
            
            // WAV 헤더 자리 예약 (나중에 데이터 크기를 알고 채움)
            outputStream?.write(ByteArray(44))
            
            dataSize = 0
            
            Log.d(TAG, "✅ WAV 파일 생성: ${outputFile!!.absolutePath}")
            return outputFile!!.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WAV 파일 생성 실패", e)
            close()
            return null
        }
    }
    
    /**
     * PCM 데이터 쓰기
     * @param pcmData Short 배열 (Naver CLOVA에서 전달)
     */
    fun write(pcmData: ShortArray) {
        if (outputStream == null || pcmData.isEmpty()) return
        
        try {
            // Short 배열을 Little Endian 바이트로 변환하여 쓰기
            pcmData.forEach { sample ->
                // Little Endian: 하위 바이트 먼저
                outputStream?.write(sample.toInt() and 0xFF)
                outputStream?.write((sample.toInt() shr 8) and 0xFF)
            }
            
            dataSize += pcmData.size * 2  // 2 bytes per sample
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ PCM 데이터 쓰기 실패", e)
        }
    }
    
    /**
     * WAV 파일 닫기 및 헤더 완성
     */
    fun close() {
        try {
            outputStream?.close()
            outputStream = null
            
            // WAV 헤더 채우기
            outputFile?.let { file ->
                if (file.exists()) {
                    val raf = RandomAccessFile(file, "rw")
                    raf.seek(0)
                    writeWavHeader(raf, dataSize)
                    raf.close()
                    
                    Log.d(TAG, "✅ WAV 파일 저장 완료: ${file.absolutePath} (${dataSize} bytes)")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WAV 파일 닫기 실패", e)
        }
    }
    
    /**
     * WAV 파일 헤더 작성
     * 
     * WAV 파일 구조:
     * - RIFF 청크 (12 bytes)
     * - fmt 청크 (24 bytes)
     * - data 청크 헤더 (8 bytes)
     * - PCM 데이터 (dataSize bytes)
     */
    private fun writeWavHeader(raf: RandomAccessFile, dataSize: Int) {
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8
        
        // RIFF 청크
        raf.writeBytes("RIFF")                              // ChunkID (4 bytes)
        writeInt32LE(raf, 36 + dataSize)                    // ChunkSize (4 bytes, Little Endian)
        raf.writeBytes("WAVE")                              // Format (4 bytes)
        
        // fmt 서브청크
        raf.writeBytes("fmt ")                              // Subchunk1ID (4 bytes)
        writeInt32LE(raf, 16)                               // Subchunk1Size (4 bytes, 16 for PCM)
        writeInt16LE(raf, 1)                                // AudioFormat (2 bytes, 1 = PCM)
        writeInt16LE(raf, CHANNELS)                         // NumChannels (2 bytes)
        writeInt32LE(raf, SAMPLE_RATE)                      // SampleRate (4 bytes)
        writeInt32LE(raf, byteRate)                         // ByteRate (4 bytes)
        writeInt16LE(raf, blockAlign)                       // BlockAlign (2 bytes)
        writeInt16LE(raf, BITS_PER_SAMPLE)                  // BitsPerSample (2 bytes)
        
        // data 서브청크
        raf.writeBytes("data")                              // Subchunk2ID (4 bytes)
        writeInt32LE(raf, dataSize)                         // Subchunk2Size (4 bytes)
    }
    
    /**
     * 32-bit 정수를 Little Endian으로 쓰기
     */
    private fun writeInt32LE(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value shr 8) and 0xFF)
        raf.writeByte((value shr 16) and 0xFF)
        raf.writeByte((value shr 24) and 0xFF)
    }
    
    /**
     * 16-bit 정수를 Little Endian으로 쓰기
     */
    private fun writeInt16LE(raf: RandomAccessFile, value: Int) {
        raf.writeByte(value and 0xFF)
        raf.writeByte((value shr 8) and 0xFF)
    }
}

