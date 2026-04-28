# AI Hackathon — Slide deck (built on the official Cursor Hitachi template)

**Topic:** Convert a Java ETL project to Python with the help of Cursor AI.

This deck reuses **`Cursor Hitachi Sprint Day Results .pptx`** so the master/theme,
fonts (Helvetica Neue), Cursor wordmark and color scheme are kept identical to
what the event expects. The first two slides match the template’s required
structure (cover + 4-quadrant project overview); slides 3–9 are the additional
content slides for **Problem / Impact / Solution / Architecture / Demo / Results
/ Conclusion**.

## Files

| File | Purpose |
|---|---|
| `Cursor Hitachi Sprint Day Results .pptx` | The original event template. **Do not delete.** `build_slides.py` opens it as a base. |
| `build_slides.py` | `python-pptx` script that fills the template’s slides 1–2 and appends 7 new content slides in matching style. |
| `cursor_logo_dark.png` | The dark Cursor wordmark, extracted from the template, used in the page chrome of the appended slides. |
| `slides_content.md` | Plain-text source of the slide copy (Problem / Impact / Solution / Results …). Edit here when you want to change wording. |
| `requirements.txt` | Python dependencies (`python-pptx`). |
| `AI_Hackathon_ETL_Java_to_Python.pptx` | Pre-built deck — open in PowerPoint / Keynote / Google Slides. |

## Deck structure (9 slides)

1. **Cover** *(template TITLE_2)* — “Java ETL → Python — AI-Assisted Migration with Cursor.”
2. **Cursor Hackathon Project** *(template 4-quadrant)* — Name/Dept, Project Description, Project Outcomes, Key Learnings.
3. **01 Problem** — six pain-point cards + closing question.
4. **02 Impact** — four KPI tiles + before/after table + stakeholder bullets.
5. **03 Solution** — five-step pipeline (Inventory → AI Translation → Auto Test Gen → Validation Harness → Human Review) + Cursor capabilities.
6. **04 Architecture** — boxes-and-arrows end-to-end flow + Cursor’s role.
7. **05 Cursor Usage / Demo** — 5–7 minute demo timeline + “How Cursor accelerated build & quality.”
8. **06 Results** — metrics table + KPI tiles + qualitative wins.
9. **07 Conclusion** — summary, roadmap, CTA strip.

## Mapping to the 5 judging criteria

| Criterion | Where it’s shown |
|---|---|
| Innovation | Slides 5, 6 (pipeline + validation harness pattern) |
| Impact | Slides 4, 8 (KPIs + measurable benefit) |
| Feasibility | Slides 5, 6, 8 (architecture, end-to-end metrics) |
| Cursor Usage | Slides 7, 5, 6 (Agent, custom rules, background agents, multi-file edit) |
| Demo Quality | Slide 7 (timeline, end-to-end with synthetic data) |

## Rebuild

```bash
pip install -r requirements.txt
python build_slides.py
# -> AI_Hackathon_ETL_Java_to_Python.pptx
```

## Customising

- **Name / Department / Team:** edit `fill_overview()` and `fill_cover()` in `build_slides.py`.
- **Theme colors:** the constants at the top of `build_slides.py` (`INK`, `ORANGE`, `SURFACE`, …) come straight from the template’s `theme1.xml`. Change them only if you want to deviate from the official theme.
- **Wording:** edit either the slide functions in `build_slides.py` or the master copy in `slides_content.md`.
- **Add a slide:** write a new `slide_xxx(prs, layout, page, total)` function and call it from `build()`. Bump `total` accordingly.

## Presenting tips

- Slide 3 (Problem): tell a real war story (e.g. spent 3 days debugging a Spring Batch chunk).
- Slide 5 (Solution): emphasise *Cursor Agent + custom rules + background agents* — that is the “smart automation” story.
- Slide 7 (Demo): keep a screen recording as a fallback in case the live demo stalls.
- Slide 8 (Results): read the bold numbers aloud — they map to the “measurable benefit” judging criterion.
