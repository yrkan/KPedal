package io.github.kpedal.engine.checkpoint

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CheckpointDecider")
class CheckpointDeciderTest {

    companion object {
        private const val CURRENT_TIME = 1_000_000_000L
        private const val CHECKPOINT_INTERVAL = CheckpointDecider.CHECKPOINT_INTERVAL_MS
        private const val STALE_TIMEOUT = CheckpointDecider.STALE_TIMEOUT_MS
        private const val MIN_SAMPLES = CheckpointDecider.MIN_SAMPLES_FOR_CHECKPOINT
    }

    @Nested
    @DisplayName("shouldSaveCheckpoint")
    inner class ShouldSaveCheckpointTests {

        @Test
        fun `returns true when recording and interval elapsed with enough samples`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL - 1000,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES + 10,
                isRecording = true
            )

            assertThat(decision.shouldSave).isTrue()
            assertThat(decision.reason).isEqualTo("interval_elapsed")
        }

        @Test
        fun `returns false when not recording`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL - 1000,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES + 10,
                isRecording = false
            )

            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("not_recording")
        }

        @Test
        fun `returns false when too few samples`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL - 1000,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES - 1,
                isRecording = true
            )

            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("too_few_samples")
        }

        @Test
        fun `returns false when interval not reached`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - 30_000, // 30 seconds ago
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES + 10,
                isRecording = true
            )

            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("interval_not_reached")
        }

        @Test
        fun `returns true when interval exactly reached`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES,
                isRecording = true
            )

            assertThat(decision.shouldSave).isTrue()
            assertThat(decision.reason).isEqualTo("interval_elapsed")
        }

        @Test
        fun `returns true when sample count equals minimum`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES,
                isRecording = true
            )

            assertThat(decision.shouldSave).isTrue()
        }

        @Test
        fun `not_recording takes priority over too_few_samples`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME - CHECKPOINT_INTERVAL - 1000,
                currentTimeMs = CURRENT_TIME,
                sampleCount = 0,
                isRecording = false
            )

            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("not_recording")
        }
    }

    @Nested
    @DisplayName("shouldRestoreCheckpoint")
    inner class ShouldRestoreCheckpointTests {

        @Test
        fun `returns true for valid checkpoint`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME - 60_000, // 1 minute ago
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isTrue()
            assertThat(decision.reason).isEqualTo("valid_checkpoint")
        }

        @Test
        fun `returns false when no checkpoint exists`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = false,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME - 60_000,
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("no_checkpoint")
        }

        @Test
        fun `returns false when checkpoint was not recording`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = false,
                lastCheckpointMs = CURRENT_TIME - 60_000,
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("not_recording")
        }

        @Test
        fun `returns false when checkpoint is stale (over 24 hours)`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME - STALE_TIMEOUT - 1000, // 24 hours + 1 second ago
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("stale_checkpoint")
        }

        @Test
        fun `returns true when checkpoint is just under 24 hours old`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME - STALE_TIMEOUT + 1000, // 24 hours - 1 second
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isTrue()
            assertThat(decision.reason).isEqualTo("valid_checkpoint")
        }

        @Test
        fun `returns false when checkpoint is exactly 24 hours old`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME - STALE_TIMEOUT, // exactly 24 hours
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("stale_checkpoint")
        }

        @Test
        fun `no_checkpoint takes priority over not_recording`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = false,
                wasRecording = false,
                lastCheckpointMs = CURRENT_TIME - 60_000,
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("no_checkpoint")
        }

        @Test
        fun `not_recording takes priority over stale_checkpoint`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = false,
                lastCheckpointMs = CURRENT_TIME - STALE_TIMEOUT - 1000,
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isFalse()
            assertThat(decision.reason).isEqualTo("not_recording")
        }
    }

    @Nested
    @DisplayName("Constants")
    inner class ConstantsTests {

        @Test
        fun `checkpoint interval is 1 minute`() {
            assertThat(CheckpointDecider.CHECKPOINT_INTERVAL_MS).isEqualTo(60_000L)
        }

        @Test
        fun `stale timeout is 24 hours`() {
            assertThat(CheckpointDecider.STALE_TIMEOUT_MS).isEqualTo(24 * 60 * 60 * 1000L)
        }

        @Test
        fun `minimum samples is 10`() {
            assertThat(CheckpointDecider.MIN_SAMPLES_FOR_CHECKPOINT).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCaseTests {

        @Test
        fun `handles zero sample count`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = 0L,
                currentTimeMs = CURRENT_TIME,
                sampleCount = 0,
                isRecording = true
            )

            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("too_few_samples")
        }

        @Test
        fun `handles very large sample count`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = 0L,
                currentTimeMs = CURRENT_TIME,
                sampleCount = Int.MAX_VALUE,
                isRecording = true
            )

            assertThat(decision.shouldSave).isTrue()
        }

        @Test
        fun `handles lastCheckpointMs of 0 (first checkpoint)`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = 0L,
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES,
                isRecording = true
            )

            // Should save since interval from 0 to CURRENT_TIME is huge
            assertThat(decision.shouldSave).isTrue()
        }

        @Test
        fun `handles clock skew (lastCheckpointMs in future)`() {
            val decision = CheckpointDecider.shouldSaveCheckpoint(
                lastCheckpointMs = CURRENT_TIME + 1000, // future time
                currentTimeMs = CURRENT_TIME,
                sampleCount = MIN_SAMPLES,
                isRecording = true
            )

            // Negative elapsed time means interval not reached
            assertThat(decision.shouldSave).isFalse()
            assertThat(decision.reason).isEqualTo("interval_not_reached")
        }

        @Test
        fun `handles fresh checkpoint (just created)`() {
            val decision = CheckpointDecider.shouldRestoreCheckpoint(
                checkpointExists = true,
                wasRecording = true,
                lastCheckpointMs = CURRENT_TIME, // just now
                currentTimeMs = CURRENT_TIME
            )

            assertThat(decision.shouldRestore).isTrue()
        }
    }
}
