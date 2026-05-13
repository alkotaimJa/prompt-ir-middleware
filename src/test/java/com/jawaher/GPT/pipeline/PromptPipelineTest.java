package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.compiler.AnthropicPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.GeminiPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import com.jawaher.GPT.pipeline.transformers.ContextDelimitationTransformer;
import com.jawaher.GPT.pipeline.transformers.InstructionScaffoldingTransformer;
import com.jawaher.GPT.pipeline.transformers.OutputFormatTransformer;
import com.jawaher.GPT.pipeline.transformers.PromptIRBuilderTransformer;
import com.jawaher.GPT.pipeline.transformers.ProviderCompilationTransformer;
import com.jawaher.GPT.pipeline.transformers.RoleConditioningTransformer;
import com.jawaher.GPT.pipeline.transformers.TokenBudgetingTransformer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style unit test for the full pipeline using real transformer
 * instances (no Spring context required).
 */
class PromptPipelineTest {

    private static final String DEFAULT_ROLE = "a helpful and precise AI assistant";

    private PromptPipeline buildPipeline() {
        List<ProviderPromptCompiler> compilers = List.of(new OpenAIPromptCompiler(), new AnthropicPromptCompiler(), new GeminiPromptCompiler());
        return new PromptPipeline(List.of(
                new InstructionScaffoldingTransformer("Your task is:", "Please complete this task clearly and accurately."),
                new RoleConditioningTransformer(DEFAULT_ROLE),
                new OutputFormatTransformer(),
                new ContextDelimitationTransformer(),
                new PromptIRBuilderTransformer(),
                new ProviderCompilationTransformer(compilers),
                new TokenBudgetingTransformer(128_000, 200_000, 1_000_000, compilers)
        ));
    }

    @Test
    void process_simpleMessage_populatesAllThreeCompiledPrompts() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is recursion?");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getCompiledPrompts()).containsKeys("openai", "claude", "gemini");
    }

    @Test
    void process_scaffoldsInstruction() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Explain polymorphism");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getTaskInstruction()).contains("Your task is:");
        assertThat(ir.getTaskInstruction()).contains("Explain polymorphism");
    }

    @Test
    void process_appliesDefaultRoleWhenNoneProvided() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getRole()).isEqualTo(DEFAULT_ROLE);
        assertThat(ir.getCompiledPrompts().get("openai").systemText()).contains(DEFAULT_ROLE);
    }

    @Test
    void process_appliesCustomRole() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Review this code");
        request.setRole("senior code reviewer");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getRole()).isEqualTo("senior code reviewer");
        assertThat(ir.getCompiledPrompts().get("openai").systemText()).contains("senior code reviewer");
    }

    @Test
    void process_appendsFormatDirectiveToInstruction() {
        ChatRequest request = new ChatRequest();
        request.setMessage("List the steps");
        request.setOutputFormat("numbered_list");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getTaskInstruction()).contains("numbered list");
    }

    @Test
    void process_detectsOutputFormatFromMessageWhenNotProvided() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Explain this and return it in JSON.");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getOutputFormat()).isEqualTo("json");
        assertThat(ir.getTaskInstruction()).contains("valid JSON");
    }

    @Test
    void process_delimitsContextBlock() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Summarise");
        request.setContext("This is background information.");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getContextBlock()).contains("### CONTEXT START ###");
        assertThat(ir.getContextBlock()).contains("This is background information.");
        assertThat(ir.getContextBlock()).contains("### CONTEXT END ###");
    }

    @Test
    void process_compiledUserTextContainsContextAndInput() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What should I do?");
        request.setContext("Project deadline is tomorrow.");

        PromptIR ir = buildPipeline().process(request);
        CompiledPrompt openAi = ir.getCompiledPrompts().get("openai");

        assertThat(openAi.userText()).contains("Project deadline is tomorrow.");
        assertThat(openAi.userText()).contains("What should I do?");
    }

    @Test
    void process_setsTokenBudgetToMinimumProviderBudget() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getTokenBudget()).isEqualTo(128_000);
    }

    @Test
    void process_preservesRawUserInput() {
        ChatRequest request = new ChatRequest();
        request.setMessage("original message");

        PromptIR ir = buildPipeline().process(request);

        assertThat(ir.getUserInput()).isEqualTo("original message");
    }
}
