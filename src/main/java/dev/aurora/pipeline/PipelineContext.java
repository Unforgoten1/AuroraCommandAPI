package dev.aurora.pipeline;

import dev.aurora.struct.CommandContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Extended command context that includes pipeline data.
 * Allows commands to pass data to subsequent stages in a pipeline.
 */
public class PipelineContext extends CommandContext {
    private final Object pipelineInput;
    private Object pipelineOutput;
    private final int stageIndex;
    private final int totalStages;
    private final Map<String, Object> pipelineData;

    /**
     * Create a pipeline context.
     *
     * @param pipelineInput Input from previous stage (null for first stage)
     * @param stageIndex Current stage index (0-based)
     * @param totalStages Total number of stages
     */
    public PipelineContext(Object pipelineInput, int stageIndex, int totalStages) {
        super();
        this.pipelineInput = pipelineInput;
        this.stageIndex = stageIndex;
        this.totalStages = totalStages;
        this.pipelineData = new HashMap<>();
    }

    /**
     * Get input from previous pipeline stage.
     *
     * @return The pipeline input, or null if this is the first stage
     */
    public Object getPipelineInput() {
        return pipelineInput;
    }

    /**
     * Set output for next pipeline stage.
     *
     * @param output The output to pass to next stage
     */
    public void setPipelineOutput(Object output) {
        this.pipelineOutput = output;
    }

    /**
     * Get the output that will be passed to next stage.
     *
     * @return The pipeline output
     */
    public Object getPipelineOutput() {
        return pipelineOutput;
    }

    /**
     * Get the current stage index (0-based).
     *
     * @return Stage index
     */
    public int getStageIndex() {
        return stageIndex;
    }

    /**
     * Get total number of stages in the pipeline.
     *
     * @return Total stages
     */
    public int getTotalStages() {
        return totalStages;
    }

    /**
     * Check if this is the first stage.
     *
     * @return True if first stage
     */
    public boolean isFirstStage() {
        return stageIndex == 0;
    }

    /**
     * Check if this is the last stage.
     *
     * @return True if last stage
     */
    public boolean isLastStage() {
        return stageIndex == totalStages - 1;
    }

    /**
     * Store data that persists across all pipeline stages.
     *
     * @param key The key
     * @param value The value
     */
    public void setPipelineData(String key, Object value) {
        pipelineData.put(key, value);
    }

    /**
     * Get data that persists across all pipeline stages.
     *
     * @param key The key
     * @return The value, or null if not found
     */
    public Object getPipelineData(String key) {
        return pipelineData.get(key);
    }

    /**
     * Get all pipeline data.
     *
     * @return Map of pipeline data
     */
    public Map<String, Object> getAllPipelineData() {
        return new HashMap<>(pipelineData);
    }
}
