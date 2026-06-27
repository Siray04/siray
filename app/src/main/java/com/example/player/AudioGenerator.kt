package com.example.player

import android.content.Context
import android.util.Log
import com.example.database.Song
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.PI
import kotlin.math.sin

object AudioGenerator {
    private const val TAG = "AudioGenerator"
    private const val SAMPLE_RATE = 22050 // 22.05 kHz is fast and efficient
    private const val TRACK_DURATION_SEC = 10 // 10 seconds per track

    fun generateDefaultSongsIfMissing(context: Context): List<Song> {
        val songsList = mutableListOf<Song>()
        val directory = context.filesDir
        
        val trackConfigs = listOf(
            TrackConfig(
                filename = "ethereal_dreamscape.wav",
                title = "Ethereal Dreamscape",
                artist = "Luna Prism",
                album = "Celestia",
                genre = "Ambient",
                year = 2026,
                type = TrackType.AMBIENT
            ),
            TrackConfig(
                filename = "neon_cyberpunk.wav",
                title = "Neon Cyberpunk",
                artist = "Vector Grid",
                album = "Hyperdrive",
                genre = "Synthwave",
                year = 2026,
                type = TrackType.CYBERPUNK
            ),
            TrackConfig(
                filename = "lofi_rainfall.wav",
                title = "Lofi Rainfall",
                artist = "Cozy Chill",
                album = "Cafe Rainy Days",
                genre = "Lofi",
                year = 2026,
                type = TrackType.LOFI
            ),
            TrackConfig(
                filename = "cosmic_frequency.wav",
                title = "Cosmic Frequency",
                artist = "Solfeggio 528",
                album = "Zen Resonance",
                genre = "Meditation",
                year = 2026,
                type = TrackType.MEDITATION
            ),
            TrackConfig(
                filename = "aero_sunrise.wav",
                title = "Aero Sunrise",
                artist = "Sola",
                album = "Aura Vibes",
                genre = "Acoustic Ambient",
                year = 2026,
                type = TrackType.SUNRISE
            )
        )

        for (config in trackConfigs) {
            val file = File(directory, config.filename)
            if (!file.exists()) {
                Log.d(TAG, "Generating WAV file: ${config.filename}")
                try {
                    generateWavFile(file, config.type)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating WAV file: ${config.filename}", e)
                }
            }

            // Estimate bitrate and codec
            val size = file.length()
            val durationMs = TRACK_DURATION_SEC * 1000L
            val bitrateKbps = if (durationMs > 0) ((size * 8) / durationMs).toInt() else 352

            songsList.add(
                Song(
                    title = config.title,
                    artist = config.artist,
                    album = config.album,
                    genre = config.genre,
                    duration = durationMs,
                    year = config.year,
                    path = file.absolutePath,
                    bitrate = bitrateKbps,
                    codec = "WAV",
                    addedDate = System.currentTimeMillis() - (songsList.size * 3600000) // Stagger added time
                )
            )
        }

        return songsList
    }

    private fun generateWavFile(file: File, type: TrackType) {
        val totalSamples = SAMPLE_RATE * TRACK_DURATION_SEC
        val totalAudioLen = totalSamples * 2L // 16-bit = 2 bytes per sample
        val totalDataLen = totalAudioLen + 36

        FileOutputStream(file).use { out ->
            writeWavHeader(
                out = out,
                totalAudioLen = totalAudioLen,
                totalDataLen = totalDataLen,
                sampleRate = SAMPLE_RATE.toLong(),
                channels = 1,
                byteRate = (SAMPLE_RATE * 2).toLong()
            )

            val buffer = ShortArray(SAMPLE_RATE) // Generate 1-second chunks
            for (sec in 0 until TRACK_DURATION_SEC) {
                for (i in 0 until SAMPLE_RATE) {
                    val t = sec + i.toDouble() / SAMPLE_RATE
                    val sampleValue = when (type) {
                        TrackType.AMBIENT -> generateAmbientSample(t)
                        TrackType.CYBERPUNK -> generateCyberpunkSample(t)
                        TrackType.LOFI -> generateLofiSample(t)
                        TrackType.MEDITATION -> generateMeditationSample(t)
                        TrackType.SUNRISE -> generateSunriseSample(t)
                    }
                    // Clamp to short range
                    buffer[i] = (sampleValue.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                }
                
                // Write buffer to file
                val byteBuffer = ByteArray(SAMPLE_RATE * 2)
                for (i in 0 until SAMPLE_RATE) {
                    val sample = buffer[i].toInt()
                    byteBuffer[i * 2] = (sample and 0xff).toByte()
                    byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
                }
                out.write(byteBuffer)
            }
        }
    }

    private fun writeWavHeader(
        out: OutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        sampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.toByte() // WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // Subchunk1Size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat (1 = PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.toByte() // 'data' chunk
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }

    // TRACK GENERATORS (Acoustic and synthetic procedural waveforms)

    private fun generateAmbientSample(t: Double): Double {
        // Slow shifting chords: Cmaj7 (C, E, G, B) -> Am7 (A, C, E, G)
        val chordIndex = (t / 2.5).toInt() % 2
        val freqs = if (chordIndex == 0) {
            doubleArrayOf(130.81, 164.81, 196.00, 246.94) // C3, E3, G3, B3
        } else {
            doubleArrayOf(110.00, 130.81, 164.81, 196.00) // A2, C3, E3, G3
        }

        var wave = 0.0
        for (i in freqs.indices) {
            val f = freqs[i]
            val amplitude = 0.12 - (i * 0.02)
            // Add slight tremolo
            val tremolo = 0.8 + 0.2 * sin(2 * PI * 1.5 * t)
            wave += amplitude * sin(2 * PI * f * t) * tremolo
        }

        // Add slow filter sweep effect (simulated with low frequency sweep modulation)
        val filterMod = sin(2 * PI * 0.1 * t) * 0.5 + 0.5
        wave *= (0.4 + 0.6 * filterMod)

        // Subtle noise for texture
        val noise = (Math.random() * 2.0 - 1.0) * 0.005
        return wave + noise
    }

    private fun generateCyberpunkSample(t: Double): Double {
        // Neon Cyberpunk: Fast pulsing 120BPM 16th pluck bass and sweep
        val bpm = 125.0
        val beatDuration = 60.0 / bpm
        val sixteenthDuration = beatDuration / 4.0
        
        val sixteenthIndex = (t / sixteenthDuration).toLong()
        val beatInPattern = (sixteenthIndex % 16).toInt()

        // Cyberpunk bassline progression in A minor
        val baseFreq = when (sixteenthIndex / 16 % 4) {
            0L -> 55.0  // A1
            1L -> 65.4  // C2
            2L -> 49.0  // G1
            3L -> 58.27 // F1
            else -> 55.0
        }

        // Fast arpeggiated bass notes
        val multiplier = when (beatInPattern % 8) {
            0, 4 -> 1.0
            2, 6 -> 2.0
            else -> 1.5
        }
        val freq = baseFreq * multiplier

        // Cyberpunk saw-tooth wave bass synth (simulated)
        var sawWave = 0.0
        for (h in 1..4) {
            sawWave += sin(2 * PI * freq * h * t) / h
        }
        sawWave *= 0.12

        // Volume decay envelope on sixteenth notes (plucky)
        val envelopeTime = t % sixteenthDuration
        val decay = kotlin.math.exp(-15.0 * envelopeTime)
        sawWave *= decay

        // Add 4-on-the-floor synth-drum click (kick transient)
        val drumEnvelope = t % beatDuration
        val drumPulse = sin(2 * PI * 80.0 * kotlin.math.exp(-40.0 * drumEnvelope) * drumEnvelope)
        val kick = drumPulse * kotlin.math.exp(-12.0 * drumEnvelope) * 0.18

        return sawWave + kick
    }

    private fun generateLofiSample(t: Double): Double {
        // Lofi Rainfall: Warm electric piano chord loops + soft rain crackle
        val chordIndex = (t / 5.0).toInt() % 2
        // Chords: Fmaj7 (F3, A3, C4, E4) -> G6 (G3, B3, D4, E4)
        val freqs = if (chordIndex == 0) {
            doubleArrayOf(174.61, 220.00, 261.63, 329.63)
        } else {
            doubleArrayOf(196.00, 246.94, 293.66, 329.63)
        }

        var pianoWave = 0.0
        for (i in freqs.indices) {
            val f = freqs[i]
            // Sine waves with warm harmonics (2nd and 3rd harmonics)
            val sub = sin(2 * PI * f * t)
            val h1 = sin(2 * PI * f * 2 * t) * 0.15
            val h2 = sin(2 * PI * f * 3 * t) * 0.05
            pianoWave += (sub + h1 + h2) * 0.08
        }

        // Add slow gentle vibrato
        val vibrato = 1.0 + 0.02 * sin(2 * PI * 6.0 * t)
        pianoWave *= vibrato

        // Rain crackle noise
        val rainNoise = if (Math.random() < 0.07) (Math.random() * 2.0 - 1.0) * 0.025 else 0.0

        return pianoWave + rainNoise
    }

    private fun generateMeditationSample(t: Double): Double {
        // Solfeggio 528Hz Love Frequency (sine) + 524Hz sine = Binaural beat of 4Hz (Theta)
        // Facilitates deep meditation and cellular repair
        val sineA = sin(2 * PI * 528.0 * t) * 0.15
        val sineB = sin(2 * PI * 524.0 * t) * 0.15 // Binaural beat in stereo (simulated mono hum)

        // Super low deep grounding bass frequency (Solfeggio 174Hz)
        val bass = sin(2 * PI * 174.0 * t) * 0.1

        // Ocean wave rise and fall (modulated low-pass filtered noise)
        val waveEnvelope = 0.5 + 0.5 * sin(2 * PI * 0.15 * t) // 6.6 sec wave period
        val oceanNoise = (Math.random() * 2.0 - 1.0) * 0.015 * waveEnvelope

        return (sineA + sineB + bass) * 0.5 + oceanNoise
    }

    private fun generateSunriseSample(t: Double): Double {
        // Major pentatonic synthetic guitar plucks (Acoustic vibes)
        // Pentatonic Scale (A-C#-D-E-G#)
        val noteDur = 0.6
        val noteIdx = (t / noteDur).toInt()
        val pattern = intArrayOf(0, 2, 3, 5, 7, 5, 3, 2, 7, 9, 7, 5, 3, 5, 2, 0)
        val noteVal = pattern[noteIdx % pattern.size]
        
        // Frequencies starting from A3 (220Hz)
        val freq = 220.0 * Math.pow(2.0, noteVal / 12.0)

        // Synthetic pluck sound: rapidly decaying wave with second harmonic
        val pluckTime = t % noteDur
        val decay = kotlin.math.exp(-6.0 * pluckTime)
        val pluckWave = (sin(2 * PI * freq * t) + 0.4 * sin(2 * PI * freq * 2 * t)) * 0.15 * decay

        // Pad background (warm chords)
        val chordIdx = (t / 2.4).toInt() % 3
        val chordFreq = when (chordIdx) {
            0 -> 110.0 // A2
            1 -> 146.8 // D3
            2 -> 164.8 // E3
            else -> 110.0
        }
        val pad = sin(2 * PI * chordFreq * t) * 0.06

        return pluckWave + pad
    }

    private class TrackConfig(
        val filename: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val year: Int,
        val type: TrackType
    )

    private enum class TrackType {
        AMBIENT, CYBERPUNK, LOFI, MEDITATION, SUNRISE
    }
}
