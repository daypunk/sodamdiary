package com.example.sodam_diary.utils

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * PCM 오디오 데이터를 M4A 파일로 인코딩하는 유틸리티
 * Naver CLOVA STT의 onRecord() 콜백에서 받은 short[] 데이터를 저장
 */
class AudioWriterPCM(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioWriterPCM"
        private const val SAMPLE_RATE = 16000 // Naver CLOVA STT 샘플레이트
        private const val CHANNEL_COUNT = 1 // 모노
        private const val BIT_RATE = 64000 // 64kbps
        private const val CODEC_TIMEOUT_US = 10000L
        private const val GAIN_MULTIPLIER = 20.0f // PCM amplitude 증폭 배율 (1048 → 20960)
    }
    
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var outputFile: File? = null
    private var totalSamplesWritten = 0L
    
    /**
     * 녹음 시작 - 파일 경로 반환
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
            outputFile = File(voicesDir, "${filename}_${timestamp}.m4a")
            
            // MediaFormat 설정
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLE_RATE,
                CHANNEL_COUNT
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }
            
            // MediaCodec 초기화
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            
            // MediaMuxer 초기화
            mediaMuxer = MediaMuxer(
                outputFile!!.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            
            audioTrackIndex = -1
            muxerStarted = false
            totalSamplesWritten = 0L
            
            Log.d(TAG, "오디오 녹음 시작: ${outputFile!!.absolutePath}")
            return outputFile!!.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "오디오 파일 열기 실패", e)
            close()
            return null
        }
    }
    
    /**
     * PCM 데이터 쓰기 (onRecord 콜백에서 호출)
     */
    fun write(pcmData: ShortArray) {
        if (mediaCodec == null || pcmData.isEmpty()) return
        
        try {
            // PCM amplitude 분석 (첫 10개 write에서만)
            val originalMaxAmplitude = if (totalSamplesWritten < 10) {
                pcmData.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
            } else 0
            
            // PCM 데이터 증폭 (Gain 적용)
            val amplifiedData = ShortArray(pcmData.size) { i ->
                val amplified = (pcmData[i] * GAIN_MULTIPLIER).toInt()
                // 클리핑 처리 (Short 범위: -32768 ~ 32767)
                when {
                    amplified > Short.MAX_VALUE -> Short.MAX_VALUE
                    amplified < Short.MIN_VALUE -> Short.MIN_VALUE
                    else -> amplified.toShort()
                }
            }
            
            // 증폭 결과 로그 (첫 10개 write에서만)
            if (totalSamplesWritten < 10) {
                val amplifiedMaxAmplitude = amplifiedData.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
                Log.d(TAG, "PCM amplitude - 원본: $originalMaxAmplitude, 증폭: $amplifiedMaxAmplitude")
            }
            
            // Short 배열을 ByteBuffer로 변환 (Little Endian 명시!)
            val byteBuffer = ByteBuffer.allocate(amplifiedData.size * 2)
                .order(ByteOrder.LITTLE_ENDIAN)
            
            amplifiedData.forEach { sample ->
                byteBuffer.putShort(sample)
            }
            byteBuffer.flip()
            
            // MediaCodec 입력 버퍼에 쓰기
            val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(byteBuffer)
                
                val presentationTimeUs = (totalSamplesWritten * 1_000_000L) / SAMPLE_RATE
                mediaCodec!!.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    byteBuffer.limit(),
                    presentationTimeUs,
                    0
                )
                
                totalSamplesWritten += pcmData.size.toLong()
            }
            
            // MediaCodec 출력 처리
            drainEncoder(false)
            
        } catch (e: Exception) {
            Log.e(TAG, "PCM 데이터 쓰기 실패", e)
        }
    }
    
    /**
     * 녹음 종료
     */
    fun close() {
        try {
            // 남은 데이터 플러시
            mediaCodec?.let {
                val inputBufferIndex = it.dequeueInputBuffer(CODEC_TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    it.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
                drainEncoder(true)
            }
            
            // MediaMuxer 중지
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
            
            // 리소스 해제
            mediaMuxer?.release()
            mediaCodec?.stop()
            mediaCodec?.release()
            
            mediaCodec = null
            mediaMuxer = null
            muxerStarted = false
            audioTrackIndex = -1
            
            Log.d(TAG, "오디오 파일 저장 완료: ${outputFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "오디오 파일 닫기 실패", e)
        }
    }
    
    /**
     * 인코더에서 데이터 추출
     */
    private fun drainEncoder(endOfStream: Boolean) {
        val bufferInfo = MediaCodec.BufferInfo()
        
        while (true) {
            val outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)
            
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // 출력 포맷 변경 → Muxer에 트랙 추가
                    if (muxerStarted) {
                        throw RuntimeException("Output format changed after muxer started")
                    }
                    val newFormat = mediaCodec!!.outputFormat
                    audioTrackIndex = mediaMuxer!!.addTrack(newFormat)
                    mediaMuxer!!.start()
                    muxerStarted = true
                    Log.d(TAG, "Muxer 시작")
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferIndex)
                    
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        if (!muxerStarted) {
                            throw RuntimeException("Muxer hasn't started")
                        }
                        
                        // 데이터를 Muxer에 쓰기
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer!!.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                    }
                    
                    mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                    
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }
}

