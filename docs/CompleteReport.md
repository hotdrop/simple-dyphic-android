# CH-2026-03-20-CODEX-AGENTS
- 変更ファイル一覧
  - `.codex/agents/pr_explorer.toml`
  - `.codex/agents/reviewer.toml`
  - `.codex/agents/performance_reviewer.toml`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - `.codex/agents/` を新設し、並列コードレビュー向けの custom agents を3種追加。
  - `pr_explorer` はコードフロー把握専用、`reviewer` はバグ・セキュリティ・テスト不足専用、`performance_reviewer` は性能観点専用として責務を明文化。
  - 各 agent に「扱う観点」と「扱わない観点」を明示し、役割の越境を防ぐプロンプト構成にした。
- 実行したテスト/確認結果
  - TOML は構文が単純なキー/文字列のみで作成し、目視でフィールド整合性を確認。
- 残課題・次アクション
  - Codex 上で各 custom agent を実際に呼び出し、期待どおりに責務分離された出力になるかを初回運用で確認する。

# CH-2026-03-20-CODEX-RULE-UPDATE
- 変更ファイル一覧
  - `AGENTS.md`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
  - `docs/feedback.md`
- 実施内容（要点）
  - `AGENTS.md` に、設計書・ADR・プロポーザルなどの仕様書を `design/` 配下へ集約するルールを追加。
  - `AGENTS.md` に、タスク完了後は `docs/feedback.md` へフィードバックを書き出し、ルール更新はユーザー承認後に行うフィードバックループを追加。
  - 今回の作業記録を `ActPlan.md` / `CompleteReport.md` / `feedback.md` に反映。
- 実行したテスト/確認結果
  - ドキュメント更新のみのため自動テストは未実施。
  - `design/` ディレクトリ実在、および更新後の記述整合性を目視確認。
- 残課題・次アクション
  - 今後のタスクでは、完了時に `docs/feedback.md` 追記と必要時の承認依頼を標準フローとして運用する。

# CH-2026-03-21-CODEX-CALENDAR-REVIEW-FIX
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarDependencies.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModelTest.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/ui/calendar/MainDispatcherRule.kt`
  - `app/src/androidTest/java/jp/hotdrop/simpledyphic/phase8/Phase8UserFlowUiTest.kt`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
  - `docs/feedback.md`
- 実施内容（要点）
  - `CalendarViewModel` の週次ロード責務を分離し、`onRetry()` で週次目標監視を再購読するよう修正。
  - 週次目標監視の初回成功では再ロードをスキップし、初期二重ロードと goal 変更時の insight 再計算を抑止。
  - 週次ロード失敗時に `weeklyStartDate` / `weeklyEndDate` / `weeklyGoalProgresses` / `weeklyInsights` / `hasBadConditionDaysInWeek` をクリアするよう統一。
  - `CalendarScreen` に `calendar_edit_selected_date_button` と日セル test tag を付与し、既存 UI テストの破損を修正。
  - `CalendarViewModelTest` で初期二重ロード抑止・retry 後の監視復旧・stale state クリアを追加し、`Phase8UserFlowUiTest` で日付選択導線を追加。
- 実行したテスト/確認結果
  - `./gradlew :app:testDebugUnitTest` 成功
  - `./gradlew :app:compileDebugAndroidTestKotlin` 成功
- 残課題・次アクション
  - `ReviewResult.md` の P3 指摘である `CalendarContent` の eager compose / 一括 state 読みは今回スコープ外のため未対応。必要なら別タスクで Lazy レイアウト化や section 分割を進める。

# CH-2026-03-21-CODEX-CALENDAR-WARNING-FIX
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarDependencies.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - `CalendarDependencyModule` と `@Binds` メソッドに `@Suppress("unused")` を付与し、Hilt 参照専用定義に対する IDE 未使用警告を解消。
  - `CalendarViewModel.observeGoals()` の初回成功スキップ判定を整理し、未読代入警告を解消。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功
  - `./gradlew :app:testDebugUnitTest` 成功
- 残課題・次アクション
  - 追加の警告は今回報告された範囲では未確認。
