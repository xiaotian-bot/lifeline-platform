# -*- coding: utf-8 -*-
"""
静态知识库检索器（关键词匹配，无向量库依赖）。

实现：
    1. 启动时加载 knowledge_base/ 下所有 .md / .txt 文档，
       按 "## " 二级标题切分为知识条目；
    2. 维护一份领域词表，查询时先抽取查询命中的领域词，
       再统计这些词在各知识条目中的出现次数作为相关度得分；
    3. 返回得分最高的 top_k 条目摘要。
"""
import os
import re
from typing import Dict, List

from app import config

# 领域词表（无分词器时的关键词匹配基础）
DOMAIN_TERMS = [
    "燃气", "供水", "供热", "排水", "管网", "管道",
    "泄漏", "爆管", "腐蚀", "堵塞", "压力", "流量", "温度", "振动",
    "预警", "研判", "预案", "等级", "处置", "抢修", "巡检", "阀门",
    "降压", "放散", "隔离", "停气", "停水", "停热", "用户通知",
    "阴极保护", "听漏", "探测", "应急", "物资", "演练", "演练", "安全",
]

# 知识条目缓存：[{source, title, text}]
_SECTIONS: List[Dict[str, str]] = []
_LOADED = False


def _load() -> None:
    """扫描知识库目录并按二级标题切分条目。"""
    global _LOADED
    kb_dir = config.KNOWLEDGE_BASE_DIR
    if not os.path.isdir(kb_dir):
        _LOADED = True
        return

    for fname in sorted(os.listdir(kb_dir)):
        if not fname.endswith((".md", ".txt")):
            continue
        path = os.path.join(kb_dir, fname)
        try:
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
        except OSError:
            continue

        # 按二级标题切分；首段（标题前内容）以文件首行作为标题
        parts = re.split(r"^##\s+", content, flags=re.M)
        for idx, part in enumerate(parts):
            part = part.strip()
            if not part:
                continue
            lines = part.split("\n", 1)
            if idx == 0:
                # 文件头：取第一个 "# " 一级标题为条目名
                m = re.match(r"#\s*(.+)", part)
                title = m.group(1).strip() if m else fname
                text = part
            else:
                title = lines[0].strip()
                text = lines[1].strip() if len(lines) > 1 else ""
            if text:
                _SECTIONS.append({"source": fname, "title": title, "text": text})
    _LOADED = True


def retrieve(query: str, top_k: int = 3) -> List[Dict[str, str]]:
    """
    关键词检索知识库。

    :param query: 用户问题
    :param top_k: 返回条目数
    :return: [{source, title, summary}] 按相关度降序
    """
    if not _LOADED:
        _load()
    if not query or not _SECTIONS:
        return []

    # 抽取查询中出现的领域词
    hit_terms = [t for t in DOMAIN_TERMS if t in query]
    # 兜底：查询长度 >= 2 时用整串做子串匹配
    if not hit_terms and len(query) >= 2:
        hit_terms = [query]

    scored = []
    for sec in _SECTIONS:
        score = sum(sec["text"].count(t) for t in hit_terms)
        # 标题命中加权
        score += sum(3 for t in hit_terms if t in sec["title"])
        if score > 0:
            scored.append((score, sec))

    scored.sort(key=lambda x: x[0], reverse=True)
    results = []
    for _, sec in scored[:top_k]:
        summary = sec["text"].replace("\n", " ")[:120]
        results.append({"source": sec["source"], "title": sec["title"], "summary": summary})
    return results
