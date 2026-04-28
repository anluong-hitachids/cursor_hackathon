"""Build the AI Hackathon presentation: Java ETL -> Python with Cursor.

Run:
    python build_slides.py

Output:
    AI_Hackathon_ETL_Java_to_Python.pptx
"""

from __future__ import annotations

from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR


# ---------------------------------------------------------------------------
# Theme
# ---------------------------------------------------------------------------
PRIMARY = RGBColor(0x2B, 0x1B, 0x6B)        # deep indigo
ACCENT = RGBColor(0xF2, 0x6B, 0x21)         # orange (matches event)
ACCENT_SOFT = RGBColor(0xFD, 0xE1, 0xCE)    # soft peach
BG = RGBColor(0xF7, 0xF5, 0xEE)             # warm cream
INK = RGBColor(0x1B, 0x1B, 0x2E)
INK_SOFT = RGBColor(0x55, 0x55, 0x66)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
ROW_ALT = RGBColor(0xFF, 0xF1, 0xE6)

FONT_TITLE = "Calibri"
FONT_BODY = "Calibri"

SLIDE_W = Inches(13.333)
SLIDE_H = Inches(7.5)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def _set_fill(shape, color: RGBColor) -> None:
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    shape.line.fill.background()


def add_rect(slide, left, top, width, height, color: RGBColor):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, height)
    _set_fill(shape, color)
    shape.shadow.inherit = False
    return shape


def add_text(
    slide,
    left,
    top,
    width,
    height,
    text: str,
    *,
    size: int = 18,
    bold: bool = False,
    color: RGBColor = INK,
    align=PP_ALIGN.LEFT,
    font: str = FONT_BODY,
    anchor=MSO_ANCHOR.TOP,
):
    tb = slide.shapes.add_textbox(left, top, width, height)
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = Inches(0.05)
    tf.margin_right = Inches(0.05)
    tf.margin_top = Inches(0.02)
    tf.margin_bottom = Inches(0.02)
    tf.vertical_anchor = anchor

    lines = text.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        run = p.add_run()
        run.text = line
        run.font.name = font
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
    return tb


def add_bullets(
    slide,
    left,
    top,
    width,
    height,
    items,
    *,
    size: int = 16,
    color: RGBColor = INK,
    bullet_color: RGBColor = ACCENT,
    line_spacing: float = 1.25,
):
    """items: list[str] or list[(title, desc)]."""
    tb = slide.shapes.add_textbox(left, top, width, height)
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = Inches(0.05)
    tf.margin_top = Inches(0.05)

    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.LEFT
        p.line_spacing = line_spacing
        p.space_after = Pt(6)

        bullet = p.add_run()
        bullet.text = "▸  "
        bullet.font.name = FONT_BODY
        bullet.font.size = Pt(size)
        bullet.font.bold = True
        bullet.font.color.rgb = bullet_color

        if isinstance(item, tuple):
            title, desc = item
            t = p.add_run()
            t.text = f"{title} "
            t.font.name = FONT_BODY
            t.font.size = Pt(size)
            t.font.bold = True
            t.font.color.rgb = color

            d = p.add_run()
            d.text = desc
            d.font.name = FONT_BODY
            d.font.size = Pt(size)
            d.font.color.rgb = color
        else:
            r = p.add_run()
            r.text = item
            r.font.name = FONT_BODY
            r.font.size = Pt(size)
            r.font.color.rgb = color
    return tb


def add_page_chrome(slide, page_no: int, total: int, section: str, title: str):
    """Background + header band + footer + page number."""
    add_rect(slide, 0, 0, SLIDE_W, SLIDE_H, BG)

    add_rect(slide, 0, 0, SLIDE_W, Inches(0.9), PRIMARY)
    add_rect(slide, 0, Inches(0.9), SLIDE_W, Inches(0.06), ACCENT)

    add_text(
        slide,
        Inches(0.5),
        Inches(0.12),
        Inches(8),
        Inches(0.4),
        section.upper(),
        size=12,
        bold=True,
        color=ACCENT_SOFT,
        font=FONT_TITLE,
    )
    add_text(
        slide,
        Inches(0.5),
        Inches(0.36),
        Inches(11),
        Inches(0.55),
        title,
        size=26,
        bold=True,
        color=WHITE,
        font=FONT_TITLE,
    )

    add_text(
        slide,
        Inches(0.5),
        Inches(7.05),
        Inches(8),
        Inches(0.35),
        "AI Hackathon 2026  •  Java ETL → Python with Cursor",
        size=10,
        color=INK_SOFT,
    )
    add_text(
        slide,
        Inches(11.5),
        Inches(7.05),
        Inches(1.3),
        Inches(0.35),
        f"{page_no} / {total}",
        size=10,
        color=INK_SOFT,
        align=PP_ALIGN.RIGHT,
    )


def add_card(slide, left, top, width, height, title: str, body_lines, *, accent=ACCENT):
    add_rect(slide, left, top, width, height, WHITE)
    add_rect(slide, left, top, Inches(0.12), height, accent)

    add_text(
        slide,
        left + Inches(0.3),
        top + Inches(0.15),
        width - Inches(0.4),
        Inches(0.45),
        title,
        size=16,
        bold=True,
        color=PRIMARY,
        font=FONT_TITLE,
    )
    add_bullets(
        slide,
        left + Inches(0.3),
        top + Inches(0.65),
        width - Inches(0.4),
        height - Inches(0.75),
        body_lines,
        size=12,
        color=INK,
        bullet_color=accent,
        line_spacing=1.15,
    )


def add_table(slide, left, top, width, height, headers, rows):
    cols = len(headers)
    n_rows = len(rows) + 1
    tbl_shape = slide.shapes.add_table(n_rows, cols, left, top, width, height)
    tbl = tbl_shape.table

    for j, h in enumerate(headers):
        cell = tbl.cell(0, j)
        cell.fill.solid()
        cell.fill.fore_color.rgb = PRIMARY
        cell.text = ""
        p = cell.text_frame.paragraphs[0]
        p.alignment = PP_ALIGN.LEFT
        r = p.add_run()
        r.text = h
        r.font.name = FONT_TITLE
        r.font.size = Pt(13)
        r.font.bold = True
        r.font.color.rgb = WHITE
        cell.margin_left = Inches(0.1)
        cell.margin_right = Inches(0.1)

    for i, row in enumerate(rows, start=1):
        for j, val in enumerate(row):
            cell = tbl.cell(i, j)
            cell.fill.solid()
            cell.fill.fore_color.rgb = WHITE if i % 2 == 1 else ROW_ALT
            cell.text = ""
            p = cell.text_frame.paragraphs[0]
            p.alignment = PP_ALIGN.LEFT
            r = p.add_run()
            r.text = str(val)
            r.font.name = FONT_BODY
            r.font.size = Pt(12)
            # Bold last column if it looks like a delta value
            if j == len(row) - 1 and any(c in str(val) for c in ("-", "+", "↓", "↑", "✅")):
                r.font.bold = True
                r.font.color.rgb = ACCENT
            else:
                r.font.color.rgb = INK
            cell.margin_left = Inches(0.1)
            cell.margin_right = Inches(0.1)
            cell.margin_top = Inches(0.05)
            cell.margin_bottom = Inches(0.05)
    return tbl_shape


def add_kpi(slide, left, top, width, height, value: str, label: str, color=ACCENT):
    add_rect(slide, left, top, width, height, WHITE)
    add_text(
        slide,
        left,
        top + Inches(0.2),
        width,
        Inches(0.9),
        value,
        size=36,
        bold=True,
        color=color,
        align=PP_ALIGN.CENTER,
        font=FONT_TITLE,
    )
    add_text(
        slide,
        left,
        top + Inches(1.05),
        width,
        Inches(0.4),
        label,
        size=12,
        color=INK_SOFT,
        align=PP_ALIGN.CENTER,
    )


def add_arrow_step(slide, left, top, width, height, idx: int, title: str, desc: str):
    add_rect(slide, left, top, width, height, WHITE)
    # number badge
    badge = slide.shapes.add_shape(MSO_SHAPE.OVAL, left + Inches(0.2), top + Inches(0.2),
                                   Inches(0.55), Inches(0.55))
    _set_fill(badge, PRIMARY)
    badge_tf = badge.text_frame
    badge_tf.margin_left = 0
    badge_tf.margin_right = 0
    p = badge_tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    r = p.add_run()
    r.text = str(idx)
    r.font.name = FONT_TITLE
    r.font.bold = True
    r.font.size = Pt(18)
    r.font.color.rgb = WHITE

    add_text(
        slide,
        left + Inches(0.85),
        top + Inches(0.18),
        width - Inches(1.0),
        Inches(0.45),
        title,
        size=14,
        bold=True,
        color=PRIMARY,
        font=FONT_TITLE,
    )
    add_text(
        slide,
        left + Inches(0.25),
        top + Inches(0.85),
        width - Inches(0.5),
        height - Inches(1.0),
        desc,
        size=11,
        color=INK,
    )


# ---------------------------------------------------------------------------
# Slides
# ---------------------------------------------------------------------------
def slide_title(prs):
    blank = prs.slide_layouts[6]
    slide = prs.slides.add_slide(blank)

    add_rect(slide, 0, 0, SLIDE_W, SLIDE_H, PRIMARY)
    # decorative band
    add_rect(slide, 0, Inches(5.7), SLIDE_W, Inches(0.12), ACCENT)
    add_rect(slide, Inches(0.6), Inches(0.6), Inches(0.18), Inches(1.4), ACCENT)

    add_text(
        slide,
        Inches(0.6),
        Inches(0.6),
        Inches(6),
        Inches(0.5),
        "AI HACKATHON 2026",
        size=14,
        bold=True,
        color=ACCENT_SOFT,
        font=FONT_TITLE,
    )
    add_text(
        slide,
        Inches(0.6),
        Inches(2.0),
        Inches(12),
        Inches(2.0),
        "From Legacy to Modern",
        size=60,
        bold=True,
        color=WHITE,
        font=FONT_TITLE,
    )
    add_text(
        slide,
        Inches(0.6),
        Inches(3.2),
        Inches(12),
        Inches(1.2),
        "AI‑Assisted ETL Migration: Java → Python with Cursor",
        size=30,
        color=ACCENT_SOFT,
        font=FONT_TITLE,
    )

    add_text(
        slide,
        Inches(0.6),
        Inches(6.0),
        Inches(8),
        Inches(0.4),
        "Team / Author:  <Your Name>",
        size=14,
        color=WHITE,
    )
    add_text(
        slide,
        Inches(0.6),
        Inches(6.4),
        Inches(8),
        Inches(0.4),
        "Topic:  Convert a production Java ETL pipeline to Python using Cursor AI",
        size=14,
        color=ACCENT_SOFT,
    )


def slide_agenda(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 2, total, "Overview", "Agenda")

    items = [
        ("Problem", "Pain points của ETL Java legacy"),
        ("Impact", "Vì sao migrate sang Python lại quan trọng"),
        ("Solution", "Pipeline 5 bước có Cursor làm co‑pilot"),
        ("Architecture", "Cách hệ thống vận hành end‑to‑end"),
        ("Cursor Usage", "AI đã tăng tốc & nâng chất lượng thế nào"),
        ("Results", "Số liệu định lượng + bài học"),
        ("Next Steps", "Lộ trình mở rộng sau hackathon"),
    ]

    add_bullets(
        slide,
        Inches(0.8),
        Inches(1.3),
        Inches(11.5),
        Inches(5.5),
        items,
        size=22,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.4,
    )


def slide_problem(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 3, total, "01  Problem", "Java ETL legacy đang kìm hãm team Data")

    add_text(
        slide,
        Inches(0.5),
        Inches(1.15),
        Inches(12),
        Inches(0.5),
        "Hệ thống ETL viết bằng Java/Spring Batch, ~50K LOC, 200+ jobs, ít test, khó tích hợp AI/ML.",
        size=14,
        color=INK_SOFT,
    )

    cards = [
        ("Năng suất thấp",
         ["Build Maven + redeploy JAR cho mỗi thay đổi nhỏ",
          "Cycle time feature: 3–5 ngày"]),
        ("Khó tích hợp AI/ML",
         ["scikit‑learn, PyTorch, LangChain… đều ở Python",
          "Cầu nối Java ↔ Python phức tạp, dễ vỡ"]),
        ("Chi phí hạ tầng cao",
         ["JVM tốn RAM, scale kém trên Kubernetes",
          "Container Python nhẹ hơn 40–60%"]),
        ("Onboarding chậm",
         ["Tuyển dev Java cho Data team ngày càng khó",
          "Dev mới mất 4–6 tuần để productive"]),
        ("Rủi ro khi viết tay",
         ["Migrate thủ công 50K LOC → dễ lệch logic",
          "Thiếu test ⇒ không tự tin release"]),
        ("Tài liệu mục nát",
         ["Knowledge nằm trong head của ex‑employees",
          "Reverse‑engineering tốn nhiều công sức"]),
    ]

    # 3 cols x 2 rows
    col_w = Inches(4.0)
    row_h = Inches(2.45)
    gap = Inches(0.15)
    start_left = Inches(0.5)
    start_top = Inches(1.85)

    for idx, (title, body) in enumerate(cards):
        col = idx % 3
        row = idx // 3
        left = start_left + col * (col_w + gap)
        top = start_top + row * (row_h + gap)
        add_card(slide, left, top, col_w, row_h, title, body)

    add_text(
        slide,
        Inches(0.5),
        Inches(6.85),
        Inches(12),
        Inches(0.4),
        "Câu hỏi: Làm sao migrate nhanh, an toàn và kiểm chứng được — không cần một đội 5–10 người trong nhiều quý?",
        size=12,
        bold=True,
        color=PRIMARY,
    )


def slide_impact(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 4, total, "02  Impact", "Tại sao bài toán này đáng giải")

    # Left: KPI cards
    add_text(
        slide,
        Inches(0.5),
        Inches(1.15),
        Inches(6),
        Inches(0.4),
        "Giá trị mang lại (ước tính trên dataset demo)",
        size=14,
        bold=True,
        color=PRIMARY,
    )

    kpi_w = Inches(2.85)
    kpi_h = Inches(1.5)
    add_kpi(slide, Inches(0.5), Inches(1.7), kpi_w, kpi_h, "‑40%", "TCO hạ tầng / tháng")
    add_kpi(slide, Inches(3.55), Inches(1.7), kpi_w, kpi_h, "10×", "Tốc độ migrate vs làm tay", color=PRIMARY)
    add_kpi(slide, Inches(0.5), Inches(3.4), kpi_w, kpi_h, "+86 pp", "Tăng test coverage")
    add_kpi(slide, Inches(3.55), Inches(3.4), kpi_w, kpi_h, "‑70%", "Time onboarding dev mới", color=PRIMARY)

    # Right: comparison table
    headers = ["Khía cạnh", "Java ETL (trước)", "Python + Cursor (sau)"]
    rows = [
        ["Time‑to‑feature", "3–5 ngày", "< 1 ngày"],
        ["Runtime trung bình", "Baseline", "+10–20% nhanh hơn"],
        ["Chi phí hạ tầng", "100%", "60–70%"],
        ["Test coverage", "< 20%", "> 80%"],
        ["Onboarding dev", "4–6 tuần", "1–2 tuần"],
        ["Tích hợp AI/ML", "Khó", "Native"],
    ]
    add_table(slide, Inches(7.0), Inches(1.7), Inches(5.85), Inches(3.2), headers, rows)

    # Stakeholder strip
    items = [
        ("Business:", "rút ngắn time‑to‑insight, giảm TCO, mở khoá AI/ML."),
        ("Engineering:", "code base hiện đại, dễ test, CI/CD nhanh."),
        ("People:", "dev tập trung vào logic data, không còn boilerplate Java."),
    ]
    add_bullets(
        slide,
        Inches(0.5),
        Inches(5.2),
        Inches(12.4),
        Inches(1.6),
        items,
        size=14,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.25,
    )


def slide_solution(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 5, total, "03  Solution", "Cursor như “Migration Co‑Pilot” — pipeline 5 bước")

    steps = [
        ("Inventory & AST",
         "Quét Java repo, dựng dependency graph, phân loại job theo độ phức tạp."),
        ("AI Translation",
         "Cursor Agent + custom rules dịch Spring Batch → Airflow, JDBC → SQLAlchemy/Polars."),
        ("Auto Test Gen",
         "Sinh pytest từ chữ ký + golden dataset chạy từ Java cũ làm oracle."),
        ("Validation Harness",
         "Chạy song song Java vs Python, diff row‑by‑row + KPI report HTML."),
        ("Human Review",
         "Cursor đề xuất refactor (typing, async, vectorize). PR kèm checklist đối chiếu."),
    ]

    step_w = Inches(2.46)
    step_h = Inches(2.4)
    gap = Inches(0.08)
    left0 = Inches(0.5)
    top = Inches(1.4)

    for i, (t, d) in enumerate(steps):
        left = left0 + i * (step_w + gap)
        add_arrow_step(slide, left, top, step_w, step_h, i + 1, t, d)
        if i < len(steps) - 1:
            arrow = slide.shapes.add_shape(
                MSO_SHAPE.RIGHT_ARROW,
                left + step_w - Inches(0.05),
                top + step_h / 2 - Inches(0.18),
                gap + Inches(0.1),
                Inches(0.36),
            )
            _set_fill(arrow, ACCENT)

    add_text(
        slide,
        Inches(0.5),
        Inches(4.1),
        Inches(12),
        Inches(0.5),
        "Cursor capabilities được dùng",
        size=16,
        bold=True,
        color=PRIMARY,
    )

    feats = [
        ("Cursor Agent", "dịch nhiều file cùng lúc, hiểu cả module."),
        ("Custom rules (.cursor/rules)", "ép coding style, type hints, cấm API deprecated."),
        ("Background Agents", "chạy migration song song nhiều job qua đêm."),
        ("Inline edit & Chat", "giải thích, refactor từng đoạn business logic."),
    ]
    add_bullets(
        slide,
        Inches(0.5),
        Inches(4.6),
        Inches(12.4),
        Inches(1.7),
        feats,
        size=14,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.25,
    )

    add_text(
        slide,
        Inches(0.5),
        Inches(6.55),
        Inches(12),
        Inches(0.4),
        "Tech stack:  Python 3.12 · Polars / PySpark · SQLAlchemy · Airflow 2 · pytest · Cursor · GitHub Actions",
        size=12,
        bold=True,
        color=PRIMARY,
    )


def slide_architecture(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 6, total, "04  Architecture", "End‑to‑end flow của hệ thống")

    # Boxes
    box_h = Inches(1.0)
    box_w = Inches(2.4)
    y = Inches(2.2)

    coords = [
        (Inches(0.5), "Java ETL\n(legacy)", PRIMARY),
        (Inches(3.3), "Parser / AST\n+ Specs", PRIMARY),
        (Inches(6.1), "Cursor Agent\n(Translation)", ACCENT),
        (Inches(8.9), "Python ETL\nAirflow + Polars", PRIMARY),
        (Inches(10.9), None, None),  # placeholder spacing
    ]

    # First row of 4 boxes
    boxes = [
        (Inches(0.5), "Java ETL\n(legacy)", PRIMARY),
        (Inches(3.3), "Parser / AST\n+ Specs", PRIMARY),
        (Inches(6.1), "Cursor Agent\n(Translation)", ACCENT),
        (Inches(8.9), "Python ETL\nAirflow + Polars", PRIMARY),
    ]
    for x, label, color in boxes:
        b = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y, box_w, box_h)
        _set_fill(b, color)
        tf = b.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = label
        r.font.name = FONT_TITLE
        r.font.size = Pt(14)
        r.font.bold = True
        r.font.color.rgb = WHITE

    # arrows between row1 boxes
    for i in range(3):
        x = boxes[i][0] + box_w
        gap = boxes[i + 1][0] - x
        a = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, x, y + box_h / 2 - Inches(0.15),
                                   gap, Inches(0.3))
        _set_fill(a, ACCENT)

    # Validation + Production row
    y2 = Inches(4.4)
    second = [
        (Inches(6.1), "Validation Harness\n(Diff vs Java)", ACCENT),
        (Inches(8.9), "Production\n(Airflow / K8s)", PRIMARY),
    ]
    for x, label, color in second:
        b = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, y2, box_w, box_h)
        _set_fill(b, color)
        tf = b.text_frame
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = label
        r.font.name = FONT_TITLE
        r.font.size = Pt(14)
        r.font.bold = True
        r.font.color.rgb = WHITE

    # Down arrow Python ETL -> Validation
    da = slide.shapes.add_shape(MSO_SHAPE.DOWN_ARROW,
                                Inches(8.9) + box_w / 2 - Inches(0.15),
                                y + box_h,
                                Inches(0.3),
                                y2 - (y + box_h))
    _set_fill(da, ACCENT)
    da2 = slide.shapes.add_shape(MSO_SHAPE.LEFT_ARROW,
                                 Inches(6.1) + box_w,
                                 y2 + box_h / 2 - Inches(0.15),
                                 Inches(8.9) - (Inches(6.1) + box_w),
                                 Inches(0.3))
    _set_fill(da2, ACCENT)

    add_text(
        slide,
        Inches(0.5),
        Inches(1.15),
        Inches(12),
        Inches(0.5),
        "Mỗi mũi tên cam = bước được tăng tốc bằng Cursor (translation, test gen, refactor).",
        size=13,
        color=INK_SOFT,
    )

    # Side notes
    add_text(
        slide,
        Inches(0.5),
        Inches(5.9),
        Inches(12),
        Inches(0.4),
        "Vai trò của Cursor",
        size=14,
        bold=True,
        color=PRIMARY,
    )
    add_bullets(
        slide,
        Inches(0.5),
        Inches(6.3),
        Inches(12.4),
        Inches(0.9),
        [
            ("Hiểu code legacy:", "giải thích Spring Batch reader/processor/writer trong vài giây."),
            ("Sinh code idiomatic:", "không dịch 1‑1, refactor sang Polars/Airflow đúng best‑practice."),
        ],
        size=12,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.15,
    )


def slide_cursor_demo(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 7, total, "05  Cursor Usage", "Demo end‑to‑end (5–7 phút, synthetic data)")

    # Left: timeline
    add_text(
        slide,
        Inches(0.5),
        Inches(1.15),
        Inches(6),
        Inches(0.5),
        "Kịch bản demo",
        size=16,
        bold=True,
        color=PRIMARY,
    )

    timeline = [
        ("0:30", "Mở repo Java mẫu CustomerEnrichmentJob.java (~400 LOC, 3 step)."),
        ("1:30", "Cursor Agent prompt: ‘Convert sang Airflow DAG dùng Polars, giữ logic, thêm tests.’"),
        ("2:00", "Multi‑file edit: tạo dags/, tasks/, tests/. Cursor giải thích từng bước."),
        ("1:00", "Chạy pytest → pass với golden dataset."),
        ("1:00", "Validation harness: Java vs Python → 0 mismatch trên 1.2M rows."),
        ("0:30", "Show metrics: LOC ‑38%, runtime ‑15%, coverage 0% → 86%."),
    ]

    tb = slide.shapes.add_textbox(Inches(0.5), Inches(1.65), Inches(6.3), Inches(5.0))
    tf = tb.text_frame
    tf.word_wrap = True
    for i, (t, d) in enumerate(timeline):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.line_spacing = 1.2
        p.space_after = Pt(8)
        badge = p.add_run()
        badge.text = f"  {t}  "
        badge.font.name = FONT_TITLE
        badge.font.bold = True
        badge.font.size = Pt(13)
        badge.font.color.rgb = WHITE
        # We can't set run background easily; emulate by surrounding chars
        badge.font.color.rgb = ACCENT
        sep = p.add_run()
        sep.text = "  "
        body = p.add_run()
        body.text = d
        body.font.name = FONT_BODY
        body.font.size = Pt(13)
        body.font.color.rgb = INK

    # Right: "How Cursor helped" card
    add_rect(slide, Inches(7.1), Inches(1.3), Inches(5.7), Inches(5.5), WHITE)
    add_rect(slide, Inches(7.1), Inches(1.3), Inches(5.7), Inches(0.5), PRIMARY)
    add_text(
        slide,
        Inches(7.3),
        Inches(1.34),
        Inches(5.4),
        Inches(0.45),
        "Cursor đã giúp như thế nào",
        size=14,
        bold=True,
        color=WHITE,
        font=FONT_TITLE,
    )

    add_bullets(
        slide,
        Inches(7.3),
        Inches(1.95),
        Inches(5.4),
        Inches(4.7),
        [
            ("Hiểu legacy nhanh:", "explain code Java + Spring Batch trong vài giây."),
            ("Translation thông minh:", "không dịch 1‑1, sinh code Pythonic, idiomatic."),
            ("Test tự động:", "pytest + fixture sinh từ schema thật."),
            ("Refactor đa file:", "đổi đồng bộ cả module, không sót call‑site."),
            ("Background agents:", "migrate hàng loạt job qua đêm."),
            ("Code review hỗ trợ:", "phát hiện sai lệch ngữ nghĩa khi diff với bản Java."),
        ],
        size=13,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.25,
    )


def slide_results(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 8, total, "06  Results", "Số liệu trên repo demo (~5K LOC Java)")

    headers = ["Metric", "Trước (Java)", "Sau (Python + Cursor)", "Δ"]
    rows = [
        ["Lines of Code", "5,120", "3,180", "‑38%"],
        ["Avg job runtime", "412 s", "350 s", "‑15%"],
        ["Test coverage", "0%", "86%", "+86 pp"],
        ["Memory peak", "2.4 GB", "1.1 GB", "‑54%"],
        ["Migration effort", "~120h dev", "~9h with Cursor", "‑92%"],
        ["Output diff vs Java", "n/a", "0 mismatch / 1.2M rows", "✅"],
    ]
    add_table(slide, Inches(0.5), Inches(1.3), Inches(8.0), Inches(3.5), headers, rows)

    # KPI strip on the right
    add_kpi(slide, Inches(8.9), Inches(1.3), Inches(4.0), Inches(1.6), "10×", "Faster migration", color=ACCENT)
    add_kpi(slide, Inches(8.9), Inches(3.05), Inches(4.0), Inches(1.6), "0", "Output mismatches", color=PRIMARY)

    add_text(
        slide,
        Inches(0.5),
        Inches(5.0),
        Inches(12),
        Inches(0.5),
        "Kết quả định tính & bài học",
        size=16,
        bold=True,
        color=PRIMARY,
    )
    add_bullets(
        slide,
        Inches(0.5),
        Inches(5.5),
        Inches(12.4),
        Inches(1.7),
        [
            ("Code base hiện đại:", "type hints, docstring, dễ tích hợp dbt + MLflow."),
            ("Pattern lặp được:", "scale dễ ra toàn bộ ETL legacy của tổ chức."),
            ("Bài học:", "Cursor mạnh nhất khi có context (schema, sample data, business rule)."),
            ("Lưới an toàn:", "Validation harness là điều kiện bắt buộc để AI‑migration production‑ready."),
        ],
        size=13,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.25,
    )


def slide_conclusion(prs, total):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_page_chrome(slide, 9, total, "07  Conclusion", "Tổng kết & bước tiếp theo")

    add_text(
        slide,
        Inches(0.5),
        Inches(1.2),
        Inches(12),
        Inches(0.5),
        "Tóm tắt",
        size=18,
        bold=True,
        color=PRIMARY,
    )
    add_bullets(
        slide,
        Inches(0.5),
        Inches(1.7),
        Inches(12.4),
        Inches(1.6),
        [
            ("Khả thi:", "AI‑assisted migration Java → Python chạy được end‑to‑end."),
            ("Nhanh:", "10× tốc độ vs làm tay, chi phí thấp hơn rõ rệt."),
            ("An toàn:", "Validation harness + auto test ⇒ 0 mismatch so với Java."),
        ],
        size=15,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.3,
    )

    add_text(
        slide,
        Inches(0.5),
        Inches(3.6),
        Inches(12),
        Inches(0.5),
        "Roadmap",
        size=18,
        bold=True,
        color=PRIMARY,
    )
    add_bullets(
        slide,
        Inches(0.5),
        Inches(4.1),
        Inches(12.4),
        Inches(2.2),
        [
            ("CLI open‑source:", "đóng gói pipeline thành tool etl-modernize."),
            ("Mở rộng nguồn:", "hỗ trợ Scala, PL/SQL, Informatica → Python."),
            ("Background agents:", "migrate hàng loạt job qua đêm, có review report."),
            ("AI reviewer:", "tự động kiểm tra semantic equivalence trước khi merge."),
        ],
        size=15,
        color=INK,
        bullet_color=ACCENT,
        line_spacing=1.3,
    )

    # CTA banner
    add_rect(slide, Inches(0.5), Inches(6.45), Inches(12.3), Inches(0.55), PRIMARY)
    add_text(
        slide,
        Inches(0.7),
        Inches(6.5),
        Inches(12),
        Inches(0.45),
        "Cho phép team Data ‘tốt nghiệp’ khỏi ETL legacy — với Cursor làm bạn đồng hành.",
        size=14,
        bold=True,
        color=WHITE,
        anchor=MSO_ANCHOR.MIDDLE,
    )


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def build():
    prs = Presentation()
    prs.slide_width = SLIDE_W
    prs.slide_height = SLIDE_H

    total = 9
    slide_title(prs)
    slide_agenda(prs, total)
    slide_problem(prs, total)
    slide_impact(prs, total)
    slide_solution(prs, total)
    slide_architecture(prs, total)
    slide_cursor_demo(prs, total)
    slide_results(prs, total)
    slide_conclusion(prs, total)

    out = "AI_Hackathon_ETL_Java_to_Python.pptx"
    prs.save(out)
    print(f"Saved: {out}")


if __name__ == "__main__":
    build()
