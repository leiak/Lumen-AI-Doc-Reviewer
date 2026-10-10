package com.docreview.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReviewTemplateLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void savesLoadsAndResetsTemplateOverrides() {
        ReviewTemplateLoader loader = new ReviewTemplateLoader(tempDir);

        ReviewTemplate template = loader.load("contract");
        template.getReviewers().getFirst().setPrompt("自定义合同规则：重点检查责任对等和数据保护。");
        loader.save("contract", template);

        assertThat(loader.available()).contains("contract");
        assertThat(loader.load("contract").getReviewers().getFirst().getPrompt())
                .isEqualTo("自定义合同规则：重点检查责任对等和数据保护。");
        assertThat(tempDir.resolve("contract.yaml")).exists();

        loader.reset("contract");
        assertThat(loader.load("contract").getReviewers().getFirst().getPrompt())
                .contains("审查合同条款");
    }
}
