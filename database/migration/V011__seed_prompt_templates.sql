-- ============================================
-- V011__seed_prompt_templates.sql — 预设提示词模板种子数据
-- ============================================
-- V004 约定：数据库只保留 PK + NOT NULL + DEFAULT，
-- 不添加业务索引、UNIQUE 约束、CHECK 约束、触发器。
-- 提示词内容为 MVP-1 占位标语，后续迭代中替换为正式 prompt。
-- skill 名称分为 5 类：issue_parse_skill、issue_classify_skill、
-- knowledge_rag_skill、report_draft_skill、qa_skill。

INSERT INTO prompt_templates (name, version, system_template, user_template) VALUES
('issue_parse_skill', 1,
 'You are an issue parser. Extract title, description, and raw text from the document.',
 'Parse the following document: {{document}}'),
('issue_classify_skill', 1,
 'Classify the issue. Output JSON: {category, priority, severity, system, assignee, tags}',
 'Classify this issue: {{issue}}'),
('knowledge_rag_skill', 1,
 'Search for similar historical issues.',
 'Find similar issues for: {{query}}'),
('report_draft_skill', 1,
 'Draft a handling report based on the classified issue.',
 'Generate a report for: {{issue}}'),
('qa_skill', 1,
 'Generate a QA reply based on the classified issue.',
 'Generate QA reply for: {{issue}}');
