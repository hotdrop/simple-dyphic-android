カレンダー機能(`CalendarScreen.kt`)に関するコードレビューを行いたいです。

Spawn one subagent per role, do not answer directly in the main agent.
必ずSubagentsを使用してください。各観点ごとに1つのagentをspawnし、並列で実行してください。
すべてのagentの完了を待ってから統合してください。

以下のagentを使用してください：

- pr_explorer：実行フローと対象ファイルの特定のみ（修正禁止）
- reviewer：バグ、セキュリティ、テスト観点のみ
- performance_reviewer：パフォーマンス観点のみ

まずagentをspawnし、その後に結果を統合してください。

## 対象範囲
カレンダー機能に関係するViewレイヤーの実装一式です。レビュー対象は、少なくとも以下を含む実行フローです。

- UIイベントの起点
  - `CalendarScreen.kt`
- ViewModel
  - `CalendarViewModel.kt`
  - `CalendarUiState.kt`
