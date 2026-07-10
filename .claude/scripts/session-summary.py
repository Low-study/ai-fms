"""
Session Summary — 会话结束摘要脚本

触发时机：Stop Hook（Claude 会话结束时）。
仅当今天有实质内容（非纯会话结束标记）时才追加标记。

用法：
    python session-summary.py <project_dir>
"""

import sys
import os
from datetime import datetime


def has_content_today(lines):
    """
    检查今天的内容是否有实质条目。
    排除以下行：纯空行、标题行、会话结束标记行。
    """
    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith('#') or stripped.startswith('---'):
            continue
        if stripped.startswith('**') and '会话结束' in stripped:
            continue
        # 有实质内容
        return True
    return False


def main():
    if len(sys.argv) < 2:
        return

    project_dir = sys.argv[1]
    today = datetime.now().strftime('%Y-%m-%d')
    timestamp = datetime.now().strftime('%H:%M')

    worklog_dir = os.path.join(project_dir, 'docs', 'worklog')
    os.makedirs(worklog_dir, exist_ok=True)

    worklog_file = os.path.join(worklog_dir, f'{today}.md')

    # 读取现有内容，判断是否需要追加标记
    existing_lines = []
    if os.path.exists(worklog_file):
        with open(worklog_file, 'r', encoding='utf-8') as f:
            existing_lines = f.readlines()

    # 只在今天有实质内容时才追加会话结束标记
    if has_content_today(existing_lines):
        # 检查最后一行是否已经是会话结束标记
        last_line = existing_lines[-1].strip() if existing_lines else ''
        if '会话结束' not in last_line:
            with open(worklog_file, 'a', encoding='utf-8') as f:
                f.write(f'\n---\n**{timestamp}** — 会话结束\n')


if __name__ == '__main__':
    main()
