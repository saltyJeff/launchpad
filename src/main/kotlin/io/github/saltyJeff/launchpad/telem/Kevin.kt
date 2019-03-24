package io.github.saltyJeff.launchpad.telem

import com.sun.speech.freetts.Voice
import com.sun.speech.freetts.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object Kevin {
    private val voice: Voice
    private var voicesGoingOn = AtomicInteger(0)
    init {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory")
        val voiceManager = VoiceManager.getInstance()
        voice = voiceManager.getVoice("kevin16")
        voice.durationStretch = 0.85f
        voice.allocate()
    }
    fun speak(text: String) {
        voicesGoingOn.incrementAndGet()
        GlobalScope.launch (Dispatchers.IO){
            voice.speak(text)
            voicesGoingOn.decrementAndGet()
        }
    }
    fun shutup() {
        voicesGoingOn.set(0)
        voice.deallocate()
    }
    fun finishUp() {
        while(voicesGoingOn.get() != 0);
    }
}