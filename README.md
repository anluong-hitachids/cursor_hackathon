# AI Hackathon — Slide deck

**Đề tài:** Convert một ETL viết bằng **Java** sang **Python** với sự hỗ trợ của **Cursor AI**.

Repo này chứa:

| File | Mô tả |
|---|---|
| `slides_content.md` | Nội dung lý thuyết cho từng slide (Problem, Impact, Solution, Results…). Sửa ở đây nếu bạn muốn đổi câu chữ. |
| `build_slides.py` | Script Python sinh file `.pptx` 9 slide (16:9) với theme tím‑cam khớp với event. |
| `requirements.txt` | Phụ thuộc Python (`python-pptx`). |
| `AI_Hackathon_ETL_Java_to_Python.pptx` | File slide đã build sẵn — mở trực tiếp bằng PowerPoint / Keynote / Google Slides. |

## Cấu trúc 9 slide

1. **Title** — tiêu đề bài thi.
2. **Agenda** — mục lục.
3. **Problem** — pain points của ETL Java legacy.
4. **Impact** — KPI + bảng so sánh trước/sau.
5. **Solution** — pipeline 5 bước có Cursor làm co‑pilot.
6. **Architecture** — sơ đồ end‑to‑end + vai trò của Cursor.
7. **Cursor Usage / Demo** — kịch bản demo 5–7 phút + cách Cursor hỗ trợ.
8. **Results** — bảng metrics + KPI chính.
9. **Conclusion & Next Steps** — tóm tắt, roadmap, CTA.

Mapping với 5 tiêu chí chấm điểm:

| Tiêu chí | Slide chính |
|---|---|
| Innovation | 5 (Solution), 6 (Architecture) |
| Impact | 4 (Impact), 8 (Results) |
| Feasibility | 5, 6, 8 |
| Cursor Usage | 7 (Demo), 5, 6 |
| Demo Quality | 7 (Demo) |

## Build lại file slide

```bash
pip install -r requirements.txt
python build_slides.py
```

Đầu ra: `AI_Hackathon_ETL_Java_to_Python.pptx` (16:9, ~9 slides).

## Tuỳ biến

- **Đổi tên / team / event:** sửa hàm `slide_title()` trong `build_slides.py`.
- **Đổi màu / theme:** chỉnh các hằng `PRIMARY`, `ACCENT`, `BG`, `INK`… ở đầu file.
- **Đổi nội dung slide:** sửa text trong từng hàm `slide_*()` (hoặc cập nhật `slides_content.md` rồi viết lại các hàm tương ứng).
- **Thêm slide:** viết một hàm `slide_xxx(prs, total)` mới và gọi trong `build()`. Nhớ tăng biến `total`.

## Gợi ý khi present

- Slide **Problem** → kể 1 câu chuyện thực tế (đã từng debug job 3 ngày, deploy lại JAR…).
- Slide **Solution** → nhấn vào *Cursor Agent + custom rules + background agents* (đây là điểm “smart automation”).
- Slide **Demo** → nên chuẩn bị video screen‑record dự phòng phòng khi mạng chậm.
- Slide **Results** → đọc to các con số đậm (cam) — đây là điểm “measurable benefit”.
