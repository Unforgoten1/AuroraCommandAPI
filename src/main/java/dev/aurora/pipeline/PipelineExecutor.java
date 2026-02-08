package dev.aurora.pipeline;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Executes command pipelines.
 * Runs each stage sequentially, passing output from one stage to the next.
 */
public class PipelineExecutor {
    private final Logger logger;
    private final Map<String, Object> sharedData;

    public PipelineExecutor() {
        this.logger = Logger.getLogger("AuroraCommands");
        this.sharedData = new HashMap<>();
    }

    /**
     * Execute a pipeline.
     *
     * @param sender The command sender
     * @param stages The pipeline stages
     * @return True if pipeline executed successfully
     */
    public boolean execute(CommandSender sender, List<PipelineStage> stages) {
        if (stages == null || stages.isEmpty()) {
            sender.sendMessage("§cInvalid pipeline!");
            return false;
        }

        logger.info("Executing pipeline with " + stages.size() + " stages for " + sender.getName());

        Object currentOutput = null;
        sharedData.clear();

        for (int i = 0; i < stages.size(); i++) {
            PipelineStage stage = stages.get(i);
            logger.info("Executing pipeline stage " + (i + 1) + "/" + stages.size() + ": " +
                       stage.getCommand().getName());

            // Create pipeline context
            PipelineContext context = new PipelineContext(currentOutput, i, stages.size());

            // Copy shared data to context
            for (Map.Entry<String, Object> entry : sharedData.entrySet()) {
                context.setPipelineData(entry.getKey(), entry.getValue());
            }

            try {
                // Execute the command with pipeline context
                boolean success = executeStage(sender, stage, context);

                if (!success) {
                    sender.sendMessage("§cPipeline failed at stage " + (i + 1) + ": " +
                                     stage.getCommand().getName());
                    logger.warning("Pipeline failed at stage " + (i + 1));
                    return false;
                }

                // Get output for next stage
                currentOutput = context.getPipelineOutput();

                // Update shared data
                sharedData.putAll(context.getAllPipelineData());

                logger.info("Pipeline stage " + (i + 1) + " completed, output: " +
                          (currentOutput != null ? currentOutput.getClass().getSimpleName() : "null"));

            } catch (Exception e) {
                sender.sendMessage("§cError in pipeline stage " + (i + 1) + ": " + e.getMessage());
                logger.severe("Pipeline execution error at stage " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }

            // Apply cooldown for this stage
            stage.getCommand().applyCooldown(sender);
        }

        logger.info("Pipeline execution completed successfully");
        sender.sendMessage("§aPipeline executed successfully!");
        return true;
    }

    /**
     * Execute a single pipeline stage.
     *
     * @param sender The command sender
     * @param stage The stage to execute
     * @param context The pipeline context
     * @return True if stage executed successfully
     */
    private boolean executeStage(CommandSender sender, PipelineStage stage, PipelineContext context) {
        try {
            // Get the pipeline output extractor if defined
            Function<PipelineContext, Object> outputExtractor =
                stage.getCommand().getPipelineOutputExtractor();

            // Execute the command
            stage.getCommand().execute(sender, stage.getArgs());

            // Extract output if extractor is defined
            if (outputExtractor != null) {
                Object output = outputExtractor.apply(context);
                context.setPipelineOutput(output);
                logger.info("Extracted pipeline output: " +
                          (output != null ? output.getClass().getSimpleName() : "null"));
            } else if (context.isFirstStage()) {
                // First stage with no extractor - log warning
                logger.warning("First pipeline stage has no output extractor, next stage will receive null input");
            }

            return true;

        } catch (Exception e) {
            logger.severe("Error executing pipeline stage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Clear shared data between pipeline executions.
     */
    public void clearSharedData() {
        sharedData.clear();
    }

    /**
     * Get shared data (for debugging).
     *
     * @return Map of shared data
     */
    public Map<String, Object> getSharedData() {
        return new HashMap<>(sharedData);
    }
}
