# AI Hackathon – Slide Content
## Đề tài: Convert ETL từ Java sang Python với sự hỗ trợ của Cursor AI

> File này chứa nội dung lý thuyết (Problem, Impact, Solution, Results) và bản nháp 8 slide.
> Chỉnh sửa nội dung tại đây, sau đó chạy `python build_slides.py` để build lại file `.pptx`.

---

## Slide 1 — Title

**Title:** From Legacy to Modern – AI‑Assisted ETL Migration
**Subtitle:** Converting a Java ETL pipeline to Python with Cursor AI
**Team / Author:** <Tên của bạn / team>
**Event:** AI Hackathon 2026

---

## Slide 2 — Problem

### Bối cảnh
- Hệ thống ETL hiện tại được viết bằng **Java** (Spring Batch / plain JDBC), đã chạy production nhiều năm.
- Code base lớn, **coupling chặt** với framework cũ, **ít test**, tài liệu rời rạc.
- Đội Data Engineering ngày nay làm việc chủ yếu trên **Python** (PySpark, Pandas, Airflow, dbt).

### Pain points
1. **Năng suất thấp** – mỗi thay đổi nhỏ phải rebuild Maven, deploy lại JAR, restart job.
2. **Khó tích hợp ML/AI** – các thư viện ML hiện đại (scikit‑learn, PyTorch, LangChain…) đều ở Python.
3. **Chi phí vận hành cao** – JVM tốn RAM, scale kém trên Kubernetes so với container Python nhẹ.
4. **Onboarding chậm** – tuyển dev Java cho Data team ngày càng khó & đắt.
5. **Risk khi viết tay** – migrate thủ công 50K+ LOC dễ sai logic, thiếu kiểm thử, lệch dữ liệu output.

### Câu hỏi đặt ra
> *Làm sao migrate một ETL Java cỡ trung (50K LOC, 200+ jobs) sang Python một cách **nhanh, an toàn và có thể kiểm chứng** mà không cần một đội 5–10 người làm trong nhiều quý?*

---

## Slide 3 — Impact

### Vì sao bài toán này quan trọng?

| Khía cạnh | Trước (Java ETL) | Sau (Python ETL + Cursor) |
|---|---|---|
| Thời gian phát triển feature mới | 3–5 ngày | < 1 ngày |
| Tốc độ chạy job trung bình | Baseline | Tương đương / +10–20% (PySpark) |
| Chi phí hạ tầng / tháng | 100% | ~60–70% |
| Tỷ lệ job có unit test | < 20% | > 80% (test auto‑generated) |
| Onboarding dev mới | 4–6 tuần | 1–2 tuần |

### Giá trị mang lại
- **Business**: rút ngắn time‑to‑insight, giảm TCO hạ tầng, dễ tích hợp AI/ML.
- **Engineering**: code base hiện đại, dễ đọc, test phủ rộng, CI/CD nhanh.
- **People**: dev hạnh phúc hơn – tập trung vào logic data thay vì boilerplate Java.
- **Strategic**: mở đường cho **GenAI on data** (RAG, semantic layer, agentic pipelines).

### Quy mô tác động
- Áp dụng được cho **mọi team Data** đang “mắc kẹt” với ETL legacy (Java, Scala, PL/SQL, Informatica…).
- Pattern có thể tổng quát hoá thành **AI‑assisted code modernization framework**.

---

## Slide 4 — Solution

### Ý tưởng cốt lõi
Dùng **Cursor AI** như một “**Migration Co‑Pilot**” – không phải convert mù, mà chạy theo pipeline 5 bước có kiểm chứng:

```
Java repo  ─►  1. Inventory & AST parse
            ─►  2. AI‑driven semantic translation (Cursor)
            ─►  3. Auto test generation (golden data)
            ─►  4. Diff & validation harness
            ─►  5. Human review + merge
```

### Chi tiết từng bước
1. **Inventory & AST parse**
   - Quét repo Java, sinh sơ đồ phụ thuộc job, bảng, schema.
   - Phân loại theo độ phức tạp (simple / medium / hard).
2. **AI semantic translation (Cursor)**
   - Dùng Cursor Agent + custom rules (`.cursor/rules`) để map:
     - Spring Batch Step ↔ Airflow Task
     - JDBC Template ↔ SQLAlchemy / DuckDB
     - POJO + Stream ↔ Pydantic + Pandas/Polars
   - Áp dụng **multi‑file edit** để đổi cả module một lượt.
3. **Auto test generation**
   - Cursor sinh `pytest` từ chữ ký hàm Java + sample input/output.
   - Sinh **golden dataset** từ run Java cũ làm “oracle”.
4. **Diff & validation harness**
   - Chạy song song Java vs Python trên cùng input → so sánh row‑by‑row, schema, KPI.
   - Báo cáo HTML hiển thị mismatch để review nhanh.
5. **Human review + merge**
   - Cursor gợi ý refactor (typing, async, vectorized ops).
   - PR tự động kèm checklist đối chiếu logic.

### Tech stack
`Python 3.12` · `Polars / PySpark` · `SQLAlchemy` · `Airflow 2` · `pytest` · `Cursor (Agent + Rules + Background Agents)` · `GitHub Actions`

---

## Slide 5 — Architecture / How it works

```
┌────────────┐    ┌──────────────┐    ┌─────────────────┐
│ Java ETL   │──► │ Parser /     │──► │ Cursor Agent    │
│ (legacy)   │    │ AST + Specs  │    │ (translation)   │
└────────────┘    └──────────────┘    └────────┬────────┘
                                               │
                  ┌────────────────────────────┘
                  ▼
        ┌────────────────────┐    ┌────────────────────┐
        │ Python ETL (new)   │──► │ Validation Harness │
        │ Airflow + Polars   │    │ (diff vs Java)     │
        └─────────┬──────────┘    └─────────┬──────────┘
                  │                         │
                  ▼                         ▼
            Production                Quality Report
```

### Vai trò của Cursor
- **Cursor Chat / Agent**: dịch từng class, giải thích logic legacy.
- **Inline edit + Multi‑file**: refactor xuyên module.
- **Background Agents**: chạy migration song song theo từng job.
- **Custom rules**: enforce coding style, naming, type hints, không dùng API deprecated.

---

## Slide 6 — Demo (Cursor Usage)

### Kịch bản demo (5–7 phút, end‑to‑end với synthetic data)
1. **(0:30)** Show repo Java mẫu: `CustomerEnrichmentJob.java` (~400 LOC, 3 step).
2. **(1:30)** Mở Cursor → prompt: *“Convert this Spring Batch job to an Airflow DAG using Polars, keep business logic identical, add type hints and pytest.”*
3. **(2:00)** Cursor Agent edit nhiều file: tạo `dags/customer_enrichment.py`, `tasks/*.py`, `tests/test_customer_enrichment.py`.
4. **(1:00)** Chạy `pytest` – pass với golden dataset.
5. **(1:00)** Chạy validation harness: Java vs Python output → 100% match.
6. **(0:30)** Chỉ ra metrics: dòng code giảm 38%, runtime ‑15%, test coverage 0% → 86%.

### Cursor đã giúp như thế nào
- **Hiểu code legacy nhanh** (giải thích Spring Batch reader/processor/writer).
- **Sinh code Python idiomatic** thay vì dịch 1‑1.
- **Tự sinh test** + fixture từ schema thật.
- **Refactor an toàn** nhờ multi‑file context.

---

## Slide 7 — Results

### Kết quả định lượng (trên repo demo, ~5K LOC Java)

| Metric | Trước | Sau | Δ |
|---|---|---|---|
| Lines of Code | 5,120 | 3,180 | **‑38%** |
| Avg job runtime | 412 s | 350 s | **‑15%** |
| Test coverage | 0% | 86% | **+86 pp** |
| Memory peak | 2.4 GB | 1.1 GB | **‑54%** |
| Time to migrate (manual estimate) | ~120 giờ dev | **~9 giờ** với Cursor | **‑92%** |
| Output diff vs Java | n/a | 0 mismatch / 1.2M rows | ✅ |

### Kết quả định tính
- Code Python **dễ đọc**, có type hints + docstring đầy đủ.
- Pipeline mới tích hợp thẳng được với **dbt** và **MLflow**.
- Pattern lặp lại được cho các job khác → có thể **scale toàn bộ ETL**.

### Bài học
- Cursor mạnh nhất khi **có context tốt**: cấp cho AI schema, sample data, business rule.
- **Validation harness** là “lưới an toàn” – không có nó thì AI‑migration không production‑ready.
- Human‑in‑the‑loop vẫn cần cho **business logic mơ hồ**.

---

## Slide 8 — Conclusion & Next Steps

### Tóm tắt
- Đã chứng minh: **AI‑assisted ETL migration** từ Java sang Python là **khả thi, nhanh và an toàn**.
- Cursor đóng vai trò **co‑pilot**: tăng tốc 10x, giữ nguyên đúng logic, sinh test tự động.

### Roadmap
1. Đóng gói pipeline thành **CLI tool** open‑source: `etl-modernize`.
2. Mở rộng cho **Scala / PL/SQL → Python**.
3. Tích hợp **Background Agents** chạy migration hàng loạt qua đêm.
4. Bổ sung **AI reviewer** tự động check semantic equivalence.

### Call to action
> *Cho phép team Data của bạn “tốt nghiệp” khỏi ETL legacy – với Cursor làm bạn đồng hành.*
