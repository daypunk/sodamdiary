package com.example.sodam_diary.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

/**
 * 음성 재생 유틸리티
 * MediaPlayer 래퍼 클래스
 */
class VoicePlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null
    private var isPlaying: Boolean = false
    
    // 콜백
    private var onPlaybackComplete: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    
    /**
     * 음성 재생 시작
     * @param voicePath 음성 파일 경로
     * @return 재생 성공 여부
     */
    fun playVoice(voicePath: String): Boolean {
        try {
            // 이미 재생 중이면 중지
            if (isPlaying) {
                stopVoice()
            }
            
            val voiceFile = File(voicePath)
            if (!voiceFile.exists()) {
                Log.w("VoicePlayer", "⚠️ 파일이 존재하지 않음: $voicePath")
                onError?.invoke("음성 파일을 찾을 수 없습니다")
                return false
            }
            
            Log.d("VoicePlayer", "🔊 재생 시작 - Path: $voicePath")
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(voicePath)
                prepare()
                setOnCompletionListener {
                    Log.d("VoicePlayer", "✅ 재생 완료")
                    isPlaying = false
                    currentPlayingPath = null
                    onPlaybackComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("VoicePlayer", "❌ 재생 오류 - what: $what, extra: $extra")
                    onError?.invoke("음성 재생 중 오류가 발생했습니다")
                    cleanup()
                    true
                }
                start()
            }
            
            currentPlayingPath = voicePath
            isPlaying = true
            
            return true
            
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 재생 시작 실패", e)
            onError?.invoke("음성을 재생할 수 없습니다: ${e.message}")
            cleanup()
            return false
        }
    }
    
    /**
     * 음성 재생 중지
     */
    fun stopVoice() {
        try {
            mediaPlayer?.apply {
                if (isPlaying()) {
                    stop()
                }
                release()
            }
            
            Log.d("VoicePlayer", "⏹️ 재생 중지")
            
            isPlaying = false
            currentPlayingPath = null
            mediaPlayer = null
            
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 재생 중지 실패", e)
            cleanup()
        }
    }
    
    /**
     * 일시정지
     */
    fun pauseVoice() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
            Log.d("VoicePlayer", "⏸️ 일시정지")
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 일시정지 실패", e)
        }
    }
    
    /**
     * 재개
     */
    fun resumeVoice() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            Log.d("VoicePlayer", "▶️ 재개")
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 재개 실패", e)
        }
    }
    
    /**
     * 재생 중 여부
     */
    fun isPlaying(): Boolean = isPlaying
    
    /**
     * 현재 재생 중인 파일 경로
     */
    fun getCurrentPlayingPath(): String? = currentPlayingPath
    
    /**
     * 현재 재생 위치 (밀리초)
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 전체 재생 시간 (밀리초)
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 특정 위치로 이동 (밀리초)
     */
    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ seekTo 실패", e)
        }
    }
    
    /**
     * 음량 설정 (0.0 ~ 1.0)
     */
    fun setVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 음량 설정 실패", e)
        }
    }
    
    /**
     * 리소스 정리
     */
    private fun cleanup() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            currentPlayingPath = null
        } catch (e: Exception) {
            Log.e("VoicePlayer", "❌ 정리 실패", e)
        }
    }
    
    /**
     * 콜백 설정
     */
    fun setCallbacks(
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        this.onPlaybackComplete = onComplete
        this.onError = onError
    }
    
    /**
     * 리소스 해제 (Activity/Fragment 종료 시 호출)
     */
    fun release() {
        stopVoice()
        cleanup()
        Log.d("VoicePlayer", "🧹 리소스 해제 완료")
    }
}

