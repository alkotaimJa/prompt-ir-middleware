package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.compiler.AnthropicPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.GeminiPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import com.jawaher.GPT.pipeline.transformers.*;
import com.jawaher.GPT.services.RoleDetectionService;

import java.util.List;

/** Builds the production pipeline without a Spring context, for unit tests. */
public final class TestPipelines {

    public static final String LENGTH_GUIDANCE =
            "Make the answer as long as the task requires and no longer; avoid filler, repetition and unnecessary introductions.";

    public static final String ALL_CONTENT_LAYERS = String.join(",",
            TaskClassificationTransformer.ID, ConstraintExtractionTransformer.ID, InstructionScaffoldingTransformer.ID,
            RoleDetectionTransformer.ID, RoleConditioningTransformer.ID, OutputFormatTransformer.ID,
            LengthControlTransformer.ID, ContextDelimitationTransformer.ID, ResponseGuidelinesTransformer.ID);

    private TestPipelines() {
    }

    public static List<ProviderPromptCompiler> compilers() {
        return List.of(new OpenAIPromptCompiler(), new AnthropicPromptCompiler(), new GeminiPromptCompiler());
    }

    public static PromptPipeline pipeline(String disabledLayers) {
        List<ProviderPromptCompiler> c = compilers();
        return new PromptPipeline(List.of(
                new TaskClassificationTransformer(),
                new ConstraintExtractionTransformer(),
                new InstructionScaffoldingTransformer(),
                new RoleDetectionTransformer(new RoleDetectionService(true)),
                new RoleConditioningTransformer(""),
                new OutputFormatTransformer(),
                new LengthControlTransformer(LENGTH_GUIDANCE),
                new ContextDelimitationTransformer(),
                new ResponseGuidelinesTransformer(),
                new IRNormalisationTransformer(),
                new ProviderCompilationTransformer(c),
                new TokenBudgetingTransformer(128_000, 200_000, 1_000_000, c)), disabledLayers);
    }

    public static PromptIR run(String message, String taskType, String context) {
        ChatRequest r = new ChatRequest();
        r.setMessage(message);
        r.setTaskType(taskType);
        r.setContext(context);
        return pipeline("").process(r);
    }
}
