package com.example.sodam_diary.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * 음성을 텍스트로 변환하는 STT 유틸리티 (녹음 파일 저장 없음)
 */
class VoiceRecorder(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    // 콜백
    private var onTranscriptionResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onReadyForSpeech: (() -> Unit)? = null
    
    /**
     * STT 시작 (음성 인식만 수행, 파일 저장 안 함)
     */
    fun startRecording(): String? {
        try {
            Log.d("VoiceRecorder", "🎤 STT 시작")
            startSpeechRecognition()
            return null // 파일 경로 반환 안 함
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ STT 시작 실패", e)
            onError?.invoke("음성 인식을 시작할 수 없습니다: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * STT 중지
     */
    fun stopRecording() {
        try {
            speechRecognizer?.stopListening()
            Log.d("VoiceRecorder", "🎤 STT 중지")
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ STT 중지 실패", e)
            cleanup()
        }
    }
    
    /**
     * STT 취소
     */
    fun cancelRecording() {
        try {
            speechRecognizer?.cancel()
            Log.d("VoiceRecorder", "🎤 STT 취소")
            
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "❌ STT 취소 실패", e)
        } finally {
            cleanup()
        }
    }
    
    /**
     * 음성 인식 시작 (내부 사용)
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
                
                // ⚠️ 주의: 아래 설정들은 Google Speech Services에서 대부분 무시됩니다
                // Google은 내부적으로 침묵 감지 ~1-2초, 전체 타임아웃 ~6-8초를 강제 적용
                // 이는 서버 부하 방지와 배터리 절약을 위한 정책입니다
                
                // 침묵 감지 시간 (실제로는 1-2초로 제한됨)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                // 말하기 시작 전 대기 시간 (실제로는 6-8초로 제한됨)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
                
                // 추가 시도: 일부 기기에서 작동할 수 있는 추가 파라미터
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // 온라인 모드 강제
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
}

