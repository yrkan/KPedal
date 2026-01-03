package io.github.kpedal.engine.checkpoint

/**
 * Pure logic for checkpoint decisions.
 * No side effects, fully testable.
 *
 * Follows the same pattern as SyncOnLaunchDecider.
 */
object CheckpointDecider {

    /** Checkpoint interval - 1 minute (user preference) */
    const val CHECKPOINT_INTERVAL_MS = 60_000L

    /** Stale timeout - 24 hours (user preference) */
    const val STALE_TIMEOUT_MS = 24 * 60 * 60 * 1000L

    /** Minimum samples before saving checkpoint (prevent empty checkpoints) */
    const val MIN_SAMPLES_FOR_CHECKPOINT = 10

    /**
     * Decision result for checkpoint save.
     */
    data class SaveDecision(
        val shouldSave: Boolean,
        val reason: String
    )

    /**
     * Decision result for checkpoint restore.
     */
    data class RestoreDecision(
        val shouldRestore: Boolean,
        val reason: String
    )

    /**
     * Determine if we should save a checkpoint now.
     *
     * @param lastCheckpointMs Timestamp of last checkpoint (0 if none)
     * @param currentTimeMs Current timestamp
     * @param sampleCount Number of samples collected
     * @param isRecording Whether ride is currently recording
     * @return Decision with shouldSave and reason
     */
    fun shouldSaveCheckpoint(
        lastCheckpointMs: Long,
        currentTimeMs: Long,
        sampleCount: Int,
        isRecording: Boolean
    ): SaveDecision {
        // Only save during active recording
        if (!isRecording) {
            return SaveDecision(false, "not_recording")
        }

        // Need minimum data to be worth saving
        if (sampleCount < MIN_SAMPLES_FOR_CHECKPOINT) {
            return SaveDecision(false, "too_few_samples")
        }

        // Check if interval has elapsed
        val elapsed = currentTimeMs - lastCheckpointMs
        if (elapsed >= CHECKPOINT_INTERVAL_MS) {
            return SaveDecision(true, "interval_elapsed")
        }

        return SaveDecision(false, "interval_not_reached")
    }

    /**
     * Determine if we should restore from a checkpoint.
     *
     * @param checkpointExists Whether a checkpoint exists in database
     * @param wasRecording Whether ride was recording when checkpoint was saved
     * @param lastCheckpointMs Timestamp of checkpoint
     * @param currentTimeMs Current timestamp
     * @return Decision with shouldRestore and reason
     */
    fun shouldRestoreCheckpoint(
        checkpointExists: Boolean,
        wasRecording: Boolean,
        lastCheckpointMs: Long,
        currentTimeMs: Long
    ): RestoreDecision {
        // No checkpoint to restore
        if (!checkpointExists) {
            return RestoreDecision(false, "no_checkpoint")
        }

        // Checkpoint was not from an active recording
        if (!wasRecording) {
            return RestoreDecision(false, "not_recording")
        }

        // Check if checkpoint is stale (older than 24 hours)
        val age = currentTimeMs - lastCheckpointMs
        if (age >= STALE_TIMEOUT_MS) {
            return RestoreDecision(false, "stale_checkpoint")
        }

        return RestoreDecision(true, "valid_checkpoint")
    }
}
