# Comfortable Health MVP 実行計画（A〜C）

## ボード運用
- カラム: `IR-ID / 担当 / 依存 / ステータス / PR or commit / ブロッカー / 次アクション`
- ステータスは `TODO / DOING / BLOCKED / REVIEW / DONE` のみを使用
- 期間: 2026-02-28

## 実装ボード（完了）
| IR-ID | 担当 | 依存 | ステータス | PR or commit | ブロッカー | 次アクション |
|---|---|---|---|---|---|---|
| IR-00 | PL | なし | DONE | local | なし | IR-01へ移行 |
| IR-01 | データ基盤 | IR-00 | DONE | local | なし | IR-02/03へ移行 |
| IR-02 | データ基盤 | IR-01 | DONE | local | なし | G1へ移行 |
| IR-03 | HC/分析 | IR-01 | DONE | local | なし | G1へ移行 |
| G1 | 全員 | IR-02, IR-03 | DONE | local | なし | IR-04/05/06へ移行 |
| IR-04 | UI | IR-02 | DONE | local | なし | G2へ移行 |
| IR-05 | HC/分析 | IR-02, IR-03 | DONE | local | なし | G2へ移行 |
| IR-06 | HC/分析 | IR-03 | DONE | local | なし | G2へ移行 |
| G2 | 全員 | IR-04, IR-05, IR-06 | DONE | local | なし | IR-07へ移行 |
| IR-07 | UI | IR-05, IR-06 | DONE | local | なし | IR-08へ移行 |
| IR-08 | PL+QA | IR-07 | DONE | local | なし | 提出 |
| CH-2026-03-14 | UI | IR-08 | DONE | local | なし | Health Connect 指標を4項目構成へ整理済み |
| CH-2026-03-14-PREVIEW | UI | CH-2026-03-14 | DONE | local | なし | WeeklyDashboardCard の単体 Preview を追加済み |
| CH-2026-03-14-DASHBOARD-UI | UI | CH-2026-03-14-PREVIEW | DONE | local | なし | WeeklyDashboardCard を達成感のある UI に改修済み |
