package com.example.sodam_diary.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.sodam_diary.BuildConfig
import com.naver.speech.clientapi.SpeechConfig
import com.naver.speech.clientapi.SpeechConfig.EndPointDetectType
import com.naver.speech.clientapi.SpeechConfig.LanguageType
import com.naver.speech.clientapi.SpeechRecognitionException
import com.naver.speech.clientapi.SpeechRecognitionListener
import com.naver.speech.clientapi.SpeechRecognitionResult
import com.naver.speech.clientapi.SpeechRecognizer
import java.lang.ref.WeakReference

/**
 * Naver CLOVA STT를 사용한 음성 인식 유틸리티
 * 
 * 타이머 기반 종료 로직:
 * 1. 발화 시작 없이 5초 경과 → 자동 취소
 * 2. 발화 후 5초 침묵 → 자동 종료
 * 3. 수동 중지 버튼 → 즉시 종료
 */
class VoiceRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecorder"
        
        // 메시지 ID
        private const val MSG_CLIENT_READY = 1
        private const val MSG_PARTIAL_RESULT = 2
        private const val MSG_FINAL_RESULT = 3
        private const val MSG_RECOGNITION_ERROR = 4
        private const val MSG_CLIENT_INACTIVE = 5
        
        // 타이머 설정
        private const val NO_SPEECH_TIMEOUT = 5000L  // 5초: 발화 없이 대기 시간
        private const val SILENCE_TIMEOUT = 5000L    // 5초: 발화 후 침묵 감지 시간
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = RecognitionHandler(this)
    
    // 콜백
    private var onTranscriptionResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onReadyForSpeech: (() -> Unit)? = null
    
    // 타이머 관리
    private val timerHandler = Handler(Looper.getMainLooper())
    private var noSpeechTimeoutRunnable: Runnable? = null
    private var silenceTimeoutRunnable: Runnable? = null
    private var cleanupRunnable: Runnable? = null
    private var hasSpeechStarted = false
    private var latestPartialResult = ""
    private var isCleanupScheduled = false
    
    /**
     * STT 시작 (Naver CLOVA 사용)
     */
    fun startRecording(): String? {
        try {
            Log.d(TAG, "🎤 Naver CLOVA STT 시작")
            
            // 기존 인식기 정리
            cleanup()
            
            // 상태 초기화
            hasSpeechStarted = false
            latestPartialResult = ""
            
            // SpeechRecognizer 생성 (공식 문서 방식 - Client ID만 필요)
            try {
                speechRecognizer = SpeechRecognizer(context, BuildConfig.NAVER_CLOVA_CLIENT_ID)
                speechRecognizer?.setSpeechRecognitionListener(NaverRecognitionListener(handler))
            } catch (e: SpeechRecognitionException) {
                Log.e(TAG, "❌ SpeechRecognizer 생성 실패", e)
                onError?.invoke("음성 인식기를 초기화할 수 없습니다: ${e.message}")
                return null
            }
            
            // 음성 인식 서버 초기화 (필수!)
            try {
                speechRecognizer?.initialize()
            } catch (e: Exception) {
                Log.e(TAG, "❌ SpeechRecognizer 초기화 실패", e)
                onError?.invoke("음성 인식 서버에 연결할 수 없습니다: ${e.message}")
                cleanup()
                return null
            }
            
            // 음성 인식 시작 (MANUAL 모드 - 자동 끝점 감지 비활성화)
            try {
                val config = SpeechConfig(LanguageType.KOREAN, EndPointDetectType.MANUAL)
                speechRecognizer?.recognize(config)
            } catch (e: SpeechRecognitionException) {
                Log.e(TAG, "❌ 음성 인식 시작 실패", e)
                onError?.invoke("음성 인식을 시작할 수 없습니다: ${e.message}")
                cleanup()
                return null
            }
            
            // 타이머 1: 5초 동안 발화 없으면 자동 취소
            startNoSpeechTimeout()
            
            return null  // 파일 저장 안 함
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ STT 시작 실패", e)
            onError?.invoke("음성 인식을 시작할 수 없습니다: ${e.message}")
            cleanup()
            return null
        }
    }
    
    /**
     * STT 중지 (사용자가 수동으로 중지)
     */
    fun stopRecording() {
        try {
            Log.d(TAG, "🎤 STT 수동 중지 - 결과 처리")
            
            // 모든 타이머 취소
            cancelAllTimers()
            
            // 인식기 중지 (onResult 콜백 호출됨)
            // 주의: stop() 후 cleanup()은 onResult/onError 콜백에서만 호출해야 함
            speechRecognizer?.stop()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ STT 중지 실패", e)
            // 예외 발생 시에도 지연 cleanup
            onError?.invoke("음성 인식 중지에 실패했습니다")
            scheduleCleanup(100)
        }
    }
    
    /**
     * STT 취소 (결과 받지 않음)
     * 주의: cancel() 대신 stop()을 사용하여 정상 종료 프로세스 따름
     */
    fun cancelRecording() {
        try {
            Log.d(TAG, "🎤 STT 취소 (사용자 요청)")
            
            // 모든 타이머 취소
            cancelAllTimers()
            
            // cancel() 대신 stop() 사용 (gRPC 크래시 방지)
            // stop()은 빈 결과를 반환하고 정상 종료됨
            speechRecognizer?.stop()
            
            // onResult/onError 콜백에서 scheduleCleanup() 호출될 것임
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ STT 취소 실패", e)
            // 예외 발생 시에도 지연 cleanup
            scheduleCleanup(100)
        }
    }
    
    /**
     * 타이머 1: 발화 시작 없이 5초 경과 시 자동 종료 (stop 사용)
     */
    private fun startNoSpeechTimeout() {
        noSpeechTimeoutRunnable?.let { timerHandler.removeCallbacks(it) }
        
        noSpeechTimeoutRunnable = Runnable {
            Log.w(TAG, "⏰ 발화 없이 5초 경과 - 자동 종료")
            onError?.invoke("음성이 감지되지 않았습니다")
            // cancel() 대신 stop() 사용 (정상 종료 프로세스)
            stopRecording()
        }
        
        timerHandler.postDelayed(noSpeechTimeoutRunnable!!, NO_SPEECH_TIMEOUT)
        Log.d(TAG, "⏱️ 발화 대기 타이머 시작 (5초)")
    }
    
    /**
     * 타이머 2: 발화 후 5초 침묵 시 자동 종료
     */
    private fun startSilenceTimeout() {
        // 기존 침묵 타이머 취소
        silenceTimeoutRunnable?.let { timerHandler.removeCallbacks(it) }
        
        silenceTimeoutRunnable = Runnable {
            Log.d(TAG, "⏰ 5초 침묵 감지 - 자동 종료")
            stopRecording()
        }
        
        timerHandler.postDelayed(silenceTimeoutRunnable!!, SILENCE_TIMEOUT)
    }
    
    /**
     * 모든 타이머 취소
     */
    private fun cancelAllTimers() {
        noSpeechTimeoutRunnable?.let { 
            timerHandler.removeCallbacks(it)
            noSpeechTimeoutRunnable = null
        }
        silenceTimeoutRunnable?.let { 
            timerHandler.removeCallbacks(it)
            silenceTimeoutRunnable = null
        }
        cleanupRunnable?.let {
            timerHandler.removeCallbacks(it)
            cleanupRunnable = null
        }
        isCleanupScheduled = false
    }
    
    /**
     * 부분 결과 수신 시 타이머 업데이트
     * (onRecord는 배경 소음에도 계속 호출되므로 부분 결과 기반으로 변경)
     */
    private fun onPartialResultReceived() {
        if (!hasSpeechStarted) {
            // 첫 발화 감지
            hasSpeechStarted = true
            
            // 타이머 1 취소 (발화 감지됨)
            noSpeechTimeoutRunnable?.let { timerHandler.removeCallbacks(it) }
            noSpeechTimeoutRunnable = null
            
            Log.d(TAG, "🗣️ 첫 발화 감지 - 침묵 타이머 시작")
        }
        
        // 타이머 2 재시작 (부분 결과 업데이트됨 = 아직 말하는 중)
        startSilenceTimeout()
        Log.d(TAG, "⏱️ 침묵 타이머 재시작 (부분 결과 수신)")
    }
    
    /**
     * cleanup() 예약 (중복 방지)
     */
    private fun scheduleCleanup(delayMs: Long) {
        if (isCleanupScheduled) {
            Log.d(TAG, "⚠️ cleanup이 이미 예약되어 있음")
            return
        }
        
        isCleanupScheduled = true
        cleanupRunnable = Runnable {
            cleanup()
        }
        timerHandler.postDelayed(cleanupRunnable!!, delayMs)
    }
    
    /**
     * 리소스 정리
     * 주의: SDK 내부 gRPC 연결 정리 완료를 위해 release()를 지연 실행
     */
    private fun cleanup() {
        try {
            // 타이머만 취소 (cleanupRunnable은 건드리지 않음)
            noSpeechTimeoutRunnable?.let { 
                timerHandler.removeCallbacks(it)
                noSpeechTimeoutRunnable = null
            }
            silenceTimeoutRunnable?.let { 
                timerHandler.removeCallbacks(it)
                silenceTimeoutRunnable = null
            }
            
            // release() 호출을 200ms 지연 (SDK 내부 gRPC 정리 완료 대기)
            speechRecognizer?.let { recognizer ->
                timerHandler.postDelayed({
                    try {
                        recognizer.release()
                        Log.d(TAG, "✅ SpeechRecognizer 리소스 해제 완료")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ SpeechRecognizer 해제 중 오류 (무시)", e)
                    }
                }, 200)
            }
            speechRecognizer = null
            isCleanupScheduled = false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 정리 실패", e)
            isCleanupScheduled = false
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
     * Handler 메시지 처리
     */
    private fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_CLIENT_READY -> {
                Log.d(TAG, "✅ STT 준비 완료")
                onReadyForSpeech?.invoke()
            }
            
            MSG_PARTIAL_RESULT -> {
                val result = msg.obj as? String ?: ""
                if (result.isNotBlank()) {
                    latestPartialResult = result
                    Log.d(TAG, "📝 부분 결과: $result")
                }
            }
            
            MSG_FINAL_RESULT -> {
                cancelAllTimers()
                
                val speechResult = msg.obj as? SpeechRecognitionResult
                val results = speechResult?.results
                val transcription = results?.firstOrNull() ?: latestPartialResult
                
                if (transcription.isNotBlank()) {
                    Log.d(TAG, "✅ 최종 결과: $transcription")
                    onTranscriptionResult?.invoke(transcription)
                } else {
                    Log.w(TAG, "⚠️ STT 결과 없음")
                    onError?.invoke("음성을 인식할 수 없습니다")
                }
                
                // 지연 cleanup (gRPC 정리 대기)
                scheduleCleanup(100)
            }
            
            MSG_RECOGNITION_ERROR -> {
                cancelAllTimers()
                
                val errorCode = msg.obj as? Int ?: -1
                val errorMessage = getErrorMessage(errorCode)
                Log.e(TAG, "❌ STT 오류: $errorMessage (code: $errorCode)")
                
                onError?.invoke(errorMessage)
                
                // 지연 cleanup (gRPC 정리 대기)
                scheduleCleanup(100)
            }
            
            MSG_CLIENT_INACTIVE -> {
                Log.d(TAG, "🔇 STT 비활성화")
                cancelAllTimers()
                
                // 지연 cleanup (gRPC 정리 대기)
                scheduleCleanup(100)
            }
        }
    }
    
    /**
     * 오류 메시지 변환
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            -1 -> "음성 인식에 실패했습니다"
            else -> "음성 인식 오류가 발생했습니다 (코드: $errorCode)"
        }
    }
    
    /**
     * Handler (메모리 누수 방지)
     */
    private class RecognitionHandler(recorder: VoiceRecorder) : Handler(Looper.getMainLooper()) {
        private val recorderRef = WeakReference(recorder)
        
        override fun handleMessage(msg: Message) {
            recorderRef.get()?.handleMessage(msg)
        }
    }
    
    /**
     * Naver CLOVA SpeechRecognitionListener 구현
     */
    private inner class NaverRecognitionListener(
        private val handler: Handler
    ) : SpeechRecognitionListener {
        
        @WorkerThread
        override fun onReady() {
            handler.obtainMessage(MSG_CLIENT_READY).sendToTarget()
        }
        
        @WorkerThread
        override fun onRecord(speech: ShortArray?) {
            // 음성 데이터 수신 (배경 소음에도 계속 호출되므로 타이머 처리 안 함)
        }
        
        @WorkerThread
        override fun onPartialResult(result: String?) {
            if (!result.isNullOrBlank()) {
                handler.obtainMessage(MSG_PARTIAL_RESULT, result).sendToTarget()
                // 부분 결과 수신 시에만 타이머 업데이트
                onPartialResultReceived()
            }
        }
        
        @WorkerThread
        override fun onEndPointDetected() {
            Log.d(TAG, "🎯 끝점 감지 (EndPoint Detected)")
        }
        
        @WorkerThread
        override fun onResult(result: SpeechRecognitionResult?) {
            handler.obtainMessage(MSG_FINAL_RESULT, result).sendToTarget()
        }
        
        @WorkerThread
        override fun onError(errorCode: Int) {
            handler.obtainMessage(MSG_RECOGNITION_ERROR, errorCode).sendToTarget()
        }
        
        @WorkerThread
        override fun onInactive() {
            handler.obtainMessage(MSG_CLIENT_INACTIVE).sendToTarget()
        }
        
        @WorkerThread
        override fun onEndPointDetectTypeSelected(epdType: EndPointDetectType?) {
            Log.d(TAG, "🔧 EndPoint 감지 타입: $epdType")
        }
    }
}
