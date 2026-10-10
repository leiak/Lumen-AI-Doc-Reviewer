package com.docreview.reviewer;

import static org.assertj.core.api.Assertions.assertThat;

import com.docreview.config.ReviewerDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiRuleGeneratorTest {
    private final AiRuleGenerator generator = new AiRuleGenerator(null);

    @Test
    void parsesGeneratedRulesFromModelContent() throws Exception {
        String content = """
                ```json
                [
                  {"role":"data_privacy","focus":"检查个人信息收集、存储和外发是否最小化。"},
                  {"role":"security_baseline","focus":"检查默认凭据、权限矩阵、加密存储和审计日志。"}
                ]
                ```
                """;

        List<ReviewerDefinition> reviewers = generator.parseRules(content);

        assertThat(reviewers).hasSize(2);
        assertThat(reviewers.get(0).getRole()).isEqualTo("data_privacy");
        assertThat(reviewers.get(0).getPrompt()).contains("个人信息");
    }
}
