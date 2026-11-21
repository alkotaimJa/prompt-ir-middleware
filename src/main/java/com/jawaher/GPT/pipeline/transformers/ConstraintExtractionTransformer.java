package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T2 – Audience and Constraint Extraction.
 * <p>
 * Makes implicit requirements in the user's wording explicit: the intended
 * audience, exact counts ("5 bullet points"), length limits ("under 100 words"),
 * the programming language and the response language. Extracted constraints
 * are restated as explicit instructions by the compilers; an explicit length or
 * count constraint also disables the generic length guard (T7).
 */
@Component
@Order(20)
public class ConstraintExtractionTransformer implements PromptTransformer {

    public static final String ID = "constraint-extraction";

    private static final Pattern BEGINNER = Pattern.compile(
            "\\b(beginners?|newbies?|novices?|for dummies|no (?:prior )?(?:background|experience)|simple terms|layman'?s? terms|"
                    + "eli5|like i'?m (?:five|5)|new to)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_TECHNICAL = Pattern.compile(
            "\\b(non-?technical|manager|business audience|stakeholders?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHILD = Pattern.compile("\\b(a child|a kid|kids|children|my son|my daughter)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPERT = Pattern.compile(
            "\\b(experts?|advanced|in-?depth|deep dive|senior engineers?|for professionals)\\b", Pattern.CASE_INSENSITIVE);

    private static final String NUM = "(\\d{1,3}|one|two|three|four|five|six|seven|eight|nine|ten)";
    private static final Pattern COUNT = Pattern.compile(
            "\\b" + NUM + "\\s+(?:key\\s+|main\\s+|short\\s+|simple\\s+)?(bullet points?|points?|items?|steps?|examples?|reasons?|"
                    + "sentences?|paragraphs?|tips?|ways?|advantages?|disadvantages?|benefits?|differences?|questions?|ideas?|lines?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_LIMIT = Pattern.compile(
            "\\b(?:under|less than|fewer than|at most|maximum of|max|no more than|within)\\s+(\\d{1,4})\\s+words\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BRIEF = Pattern.compile("\\b(briefly|brief|short answer|in short|concise|one sentence|tl;?dr)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROG_LANG = Pattern.compile(
            "\\b(?:in|using|with)\\s+(Java|Python|JavaScript|TypeScript|C\\+\\+|C#|Go|Golang|Rust|Kotlin|Swift|SQL|PHP|Ruby|Dart)(?=\\W|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESPONSE_LANG = Pattern.compile(
            "\\b(?:answer|respond|reply|write|explain)(?:\\s+\\w+){0,3}\\s+in\\s+(Arabic|English|French|Spanish|German|Urdu|Turkish)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> WORD_NUMBERS = Map.of(
            "one", "1", "two", "2", "three", "3", "four", "4", "five", "5",
            "six", "6", "seven", "7", "eight", "8", "nine", "9", "ten", "10");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        String input = ir.getUserInput() == null ? "" : ir.getUserInput();

        if (CHILD.matcher(input).find()) {
            ir.setAudience("a child; use simple words and everyday examples");
        } else if (BEGINNER.matcher(input).find()) {
            ir.setAudience("a beginner with no prior background; define technical terms when first used");
        } else if (NON_TECHNICAL.matcher(input).find()) {
            ir.setAudience("a non-technical reader; avoid jargon and focus on practical implications");
        } else if (EXPERT.matcher(input).find()) {
            ir.setAudience("an expert reader; skip basic definitions and go into technical depth");
        }

        Matcher count = COUNT.matcher(input);
        if (count.find()) {
            String n = WORD_NUMBERS.getOrDefault(count.group(1).toLowerCase(Locale.ROOT), count.group(1));
            ir.getConstraints().add("Provide exactly " + n + " " + count.group(2).toLowerCase(Locale.ROOT) + ".");
            ir.setExplicitLengthConstraint(true);
        }
        Matcher words = WORD_LIMIT.matcher(input);
        if (words.find()) {
            ir.getConstraints().add("Keep the answer under " + words.group(1) + " words.");
            ir.setExplicitLengthConstraint(true);
        } else if (BRIEF.matcher(input).find()) {
            ir.getConstraints().add("Keep the answer brief.");
            ir.setExplicitLengthConstraint(true);
        }
        Matcher lang = PROG_LANG.matcher(input);
        if (lang.find()) {
            ir.getConstraints().add("Use " + canonicalLanguage(lang.group(1)) + ".");
        }
        Matcher resp = RESPONSE_LANG.matcher(input);
        if (resp.find()) {
            String l = resp.group(1);
            ir.getConstraints().add("Respond in " + Character.toUpperCase(l.charAt(0)) + l.substring(1).toLowerCase(Locale.ROOT) + ".");
        }
        return ir;
    }

    private static String canonicalLanguage(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "java" -> "Java";
            case "python" -> "Python";
            case "javascript" -> "JavaScript";
            case "typescript" -> "TypeScript";
            case "c++" -> "C++";
            case "c#" -> "C#";
            case "go", "golang" -> "Go";
            case "rust" -> "Rust";
            case "kotlin" -> "Kotlin";
            case "swift" -> "Swift";
            case "sql" -> "SQL";
            case "php" -> "PHP";
            case "ruby" -> "Ruby";
            case "dart" -> "Dart";
            default -> s;
        };
    }
}
