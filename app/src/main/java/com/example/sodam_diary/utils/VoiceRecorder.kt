package com.example.sodam_diary.utils

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File

/**
 * 음성 녹음 + STT 통합 유틸리티
 * MediaRecorder와 SpeechRecognizer를 동시에 실행
 */
class VoiceRecorder(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentVoiceFilePath: String? = null
    
    // 음성 디렉토리
    private val voicesDir: File by lazy {
        File(context.filesDir, "voices").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // 콜백
    private var onTranscriptionResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onReadyForSpeech: (() -> Unit)? = null
    
    /**
     * 녹음 시작 (MediaRecorder + SpeechRecognizer 동시 실행)
     * @return 녹음 파일 경로
     */
    fun startRecording(): String? {
        try {
            // 1. 녹음 파일 경로 생성
            val timestamp = System.currentTimeMillis()
            val voiceFile = File(voicesDir, "voice_$timestamp.m4a")
            currentVoiceFilePath = voiceFile.absolutePath
            
            // 2. MediaRecorder 설정 및 시작
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(voiceFile.absolutePath)
                prepare()
                start()
            }
            
            Log.d("VoiceRecorder", "🎤 녹음 시작 - Path: ${voiceFile.absolutePath}")
            
            // 3. SpeechRecognizer 시작
            startSpeechRecognition()
            
            return currentVoiceFilePath
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ 녹음 시작 실패", e)
            onError?.invoke("녹음을 시작할 수 없습니다: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * 녹음 중지
     * @return 전사된 텍스트 (콜백으로도 전달됨)
     */
    fun stopRecording() {
        try {
            // 1. MediaRecorder 중지
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // 2. SpeechRecognizer 중지
            speechRecognizer?.stopListening()
            
            Log.d("VoiceRecorder", "🎤 녹음 중지 - Path: $currentVoiceFilePath")
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ 녹음 중지 실패", e)
            cleanup()
        }
    }
    
    /**
     * 녹음 취소 (파일 삭제)
     */
    fun cancelRecording() {
        try {
            stopRecording()
            
            // 녹음 파일 삭제
            currentVoiceFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                    Log.d("VoiceRecorder", "🗑️ 녹음 파일 삭제: $path")
                }
            }
            
            currentVoiceFilePath = null
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ 녹음 취소 실패", e)
        } finally {
            cleanup()
        }
    }
    
    /**
     * STT 시작 (내부 사용)
     */
    private fun startSpeechRecognition() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("VoiceRecorder", "⚠️ 음성 인식을 사용할 수 없습니다")
                onError?.invoke("이 기기에서는 음성 인식을 사용할 수 없습니다")
                return
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("VoiceRecorder", "✅ STT 준비 완료")
                        onReadyForSpeech?.invoke()
                    }
                    
                    override fun onBeginningOfSpeech() {
                        Log.d("VoiceRecorder", "🗣️ 말하기 시작")
                    }
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // 음성 레벨 변화 (필요시 사용)
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {
                        // 버퍼 수신 (필요시 사용)
                    }
                    
                    override fun onEndOfSpeech() {
                        Log.d("VoiceRecorder", "🗣️ 말하기 종료")
                    }
                    
                    override fun onError(error: Int) {
                        val errorMessage = getErrorMessage(error)
                        Log.e("VoiceRecorder", "❌ STT 오류: $errorMessage (code: $error)")
                        
                        // 사용자에게 친절한 오류 메시지
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> {
                                onError?.invoke("음성을 인식할 수 없습니다. 다시 시도해주세요")
                            }
                            SpeechRecognizer.ERROR_NETWORK -> {
                                onError?.invoke("네트워크 연결을 확인해주세요")
                            }
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                onError?.invoke("마이크 권한이 필요합니다")
                            }
                            else -> {
                                onError?.invoke("음성 인식에 실패했습니다")
                            }
                        }
                    }
                    
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val transcription = matches?.firstOrNull() ?: ""
                        
                        if (transcription.isNotBlank()) {
                            Log.d("VoiceRecorder", "✅ STT 결과: $transcription")
                            onTranscriptionResult?.invoke(transcription)
                        } else {
                            Log.w("VoiceRecorder", "⚠️ STT 결과 없음")
                            onError?.invoke("음성을 인식할 수 없습니다")
                        }
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        // 부분 결과 (실시간 표시 시 사용)
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()
                        if (partial != null) {
                            Log.d("VoiceRecorder", "📝 부분 결과: $partial")
                        }
                    }
                    
                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // 기타 이벤트
                    }
                })
            }
            
            // Intent 설정
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer?.startListening(intent)
            Log.d("VoiceRecorder", "🎤 STT 시작")
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ STT 시작 실패", e)
            onError?.invoke("음성 인식을 시작할 수 없습니다")
        }
    }
    
    /**
     * 오류 메시지 변환
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "오디오 녹음 오류"
            SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
            SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
            SpeechRecognizer.ERROR_NO_MATCH -> "인식 결과 없음"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기 사용 중"
            SpeechRecognizer.ERROR_SERVER -> "서버 오류"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 타임아웃"
            else -> "알 수 없는 오류"
        }
    }
    
    /**
     * 리소스 정리
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            
            speechRecognizer?.destroy()
            speechRecognizer = null
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ 정리 실패", e)
        }
    }
    
    /**
     * 콜백 설정
     */
    fun setCallbacks(
        onTranscription: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: (() -> Unit)? = null
    ) {
        this.onTranscriptionResult = onTranscription
        this.onError = onError
        this.onReadyForSpeech = onReady
    }
    
    /**
     * 현재 녹음 파일 경로 반환
     */
    fun getCurrentVoiceFilePath(): String? = currentVoiceFilePath
    
    /**
     * 음성 파일 존재 여부 확인
     */
    fun voiceFileExists(path: String): Boolean {
        return File(path).exists()
    }
    
    /**
     * 음성 파일 삭제
     */
    fun deleteVoiceFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ 파일 삭제 실패: $path", e)
            false
        }
    }
}

