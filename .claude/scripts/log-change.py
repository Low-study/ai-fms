"""
Worklog Automation — 自动工作留痕脚本

触发时机：每次 Edit/Write 操作后由 PostToolUse Hook 调用。
记录格式：追加到 docs/worklog/YYYY-MM-DD.md

用法：
    python log-change.py <tool_name> <file_path> <project_dir>
"""

import sys
import os
from datetime import datetime


def main():
    if len(sys.argv) < 4:
        return  # 参数不足，静默退出

    tool_name = sys.argv[1]
    file_path = sys.argv[2]
    project_dir = sys.argv[3]

    # 只记录项目内的文件
    if not file_path.startswith(project_dir):
        return

    # 排除 .git 和临时文件
    rel_path = os.path.relpath(file_path, project_dir)
    if rel_path.startswith('.git') or rel_path.endswith('~'):
        return

    today = datetime.now().strftime('%Y-%m-%d')
    timestamp = datetime.now().strftime('%H:%M')

    worklog_dir = os.path.join(project_dir, 'docs', 'worklog')
    os.makedirs(worklog_dir, exist_ok=True)

    worklog_file = os.path.join(worklog_dir, f'{today}.md')

    # 如果文件不存在，创建并写入标题
    if not os.path.exists(worklog_file):
        with open(worklog_file, 'w', encoding='utf-8') as f:
            f.write(f'# Worklog — {today}\n\n')

    # 追加变更记录
    with open(worklog_file, 'a', encoding='utf-8') as f:
        f.write(f'- **{timestamp}** | `{tool_name}` | `{rel_path}`\n')


if __name__ == '__main__':
    main()
