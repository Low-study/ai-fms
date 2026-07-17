package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.EmbeddingModelPort;
import com.aifms.modules.agent.domain.KnowledgeRagSkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.SimilarIssueItem;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.finding.infrastructure.FindingRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

/**
 * 知识库 RAG 检索技能适配器。
 * 通过 pgvector 向量相似度检索历史相似工单：
 * <ol>
 *   <li>将工单标题 + 描述组合为查询文本</li>
 *   <li>调用 {@link EmbeddingModelPort} 嵌入为向量</li>
 *   <li>在 {@code issue_embeddings} 表中执行余弦距离查询</li>
 *   <li>关联 {@code findings} 表获取完整工单信息</li>
 *   <li>返回 {@link SimilarIssues}</li>
 * </ol>
 *
 * @author aifms
 */
@Service
public class KnowledgeRagSkillAdapter implements KnowledgeRagSkill {

    private final EmbeddingModelPort embeddingModelPort;
    private final IssueEmbeddingRepository issueEmbeddingRepository;
    private final FindingRepository findingRepository;

    public KnowledgeRagSkillAdapter(EmbeddingModelPort embeddingModelPort,
                                    IssueEmbeddingRepository issueEmbeddingRepository,
                                    FindingRepository findingRepository) {
        this.embeddingModelPort = embeddingModelPort;
        this.issueEmbeddingRepository = issueEmbeddingRepository;
        this.findingRepository = findingRepository;
    }

    @Override
    public Mono<SimilarIssues> retrieveSimilar(ParsedIssue issue) {
        String queryText = buildQueryText(issue);

        if (queryText.isBlank()) {
            return Mono.just(new SimilarIssues(List.of()));
        }

        return Mono.fromCallable(() -> queryText)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(text -> embeddingModelPort.embed(text)
                        .flatMap(this::searchAndMapResults)
                );
    }

    /**
     * 构建用于向量嵌入的查询文本。
     * 组合标题和描述，截断过长的文本以适配嵌入模型的输入限制。
     */
    private String buildQueryText(ParsedIssue issue) {
        StringBuilder sb = new StringBuilder();
        if (issue.title() != null) {
            sb.append(issue.title());
        }
        if (issue.description() != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(issue.description());
        }
        return sb.toString().trim();
    }

    /**
     * 执行向量搜索并将结果映射为 {@link SimilarIssues}。
     * 将 {@code float[]} 转换为 pgvector 兼容格式，
     * 查询相似嵌入记录，再关联 findings 表获取完整信息。
     */
    private Mono<SimilarIssues> searchAndMapResults(float[] embedding) {
        String pgVector = convertToPgVector(embedding);

        return issueEmbeddingRepository.findSimilar(pgVector, 5)
                .flatMap(this::mapToSimilarIssueItem)
                .collectList()
                .map(SimilarIssues::new)
                .switchIfEmpty(Mono.just(new SimilarIssues(List.of())));
    }

    /**
     * 将浮点数向量转换为 pgvector 兼容的字符串格式。
     * 输出格式：{@code [0.123,0.456,...]}
     */
    private String convertToPgVector(float[] embedding) {
        return Arrays.toString(embedding).replace(" ", "");
    }

    /**
     * 将单条嵌入记录映射为 {@link SimilarIssueItem}。
     * 通过 findingId 查询对应的指摘记录，组装结果。
     * 若 finding 不存在（孤立记录），跳过该项。
     */
    private Mono<SimilarIssueItem> mapToSimilarIssueItem(IssueEmbeddingEntity embedding) {
        return findingRepository.findById(embedding.getFindingId())
                .map(finding -> {
                    double similarity = computeSimilarity(embedding.getDistance());
                    String resolution = finding.getResolution() != null
                            ? finding.getResolution()
                            : embedding.getContent();

                    return new SimilarIssueItem(
                            finding.getId().toString(),
                            finding.getTitle(),
                            similarity,
                            resolution,
                            finding.getReportDraft() != null ? finding.getReportDraft() : ""
                    );
                });
    }

    /**
     * 将 pgvector 余弦距离转换为相似度得分（0.0~1.0）。
     * pgvector 的 {@code vector_cosine_ops} 返回的距离 d 满足：
     * {@code d = 1 - cosine_similarity}，因此 {@code similarity = 1 - d}。
     * 结果截断至 [0.0, 1.0] 区间。
     */
    private double computeSimilarity(Double distance) {
        if (distance == null) {
            return 0.0;
        }
        double similarity = 1.0 - distance;
        if (similarity < 0.0) {
            return 0.0;
        }
        if (similarity > 1.0) {
            return 1.0;
        }
        return similarity;
    }
}
