**Findings**

- P2: `onRetry()` が週次目標の監視を再開しません。[CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L54) は `observeRecords()` と単発の `loadWeeklyData()` しか呼ばず、[CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L120) の `observeGoals()` を再購読しません。`observeWeeklyGoals()` が `Failure` を返して終了した後は、再試行しても以後の週次目標変更が画面に反映されず、`ViewModel` 再生成まで回復しません。

- P2: 週次ロード失敗時に古い週次データが state に残り、UI でも一部表示され続けます。[CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L149) と [CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L159) は `weeklyErrorMessageResId` だけ更新し、`weeklyStartDate` / `weeklyEndDate` / `weeklyGoalProgresses` / `weeklyInsights` をクリアしません。そのため [CalendarScreen.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt#L474) の期間表示が失敗後も前回値を出し続け、誤った週の文脈を見せます。

- P2: 初期表示時に週次データ取得が二重実行されます。[CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L42) で `loadWeeklyData()` を呼んだ直後、[CalendarViewModel.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt#L120) の `observeGoals()` 初回成功でも再度 `loadWeeklyData()` を呼びます。起動時に Health Connect 系集計と insight 生成が重複し、しかも goals 変更で insight まで毎回再計算されます。

- P3: `CalendarContent` が `CalendarUiState` 全体を一括で読み、全セクションを `Column(verticalScroll())` 配下で eager compose しています。[CalendarScreen.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt#L147) から [CalendarScreen.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt#L307) までの構成だと、週次ロード状態の変化でも月グリッドや日セルまで巻き込んで再 compose されやすく、表示項目数が増えたときの無駄な測定コストも残ります。

- P3: テストが重要経路を保護しておらず、既存 UI テストも現状実装とズレています。[Phase8UserFlowUiTest.kt](simpledyphic/app/src/androidTest/java/jp/hotdrop/simpledyphic/phase8/Phase8UserFlowUiTest.kt#L83) は `calendar_edit_selected_date_button` を前提にしていますが、[CalendarScreen.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt#L963) の `SummaryCard` にはその test tag がありません。加えて、日付選択、retry 後の回復、週次エラー遷移を検証する `ViewModel` / UI テストが不足しています。

**Security**

- 対象 view layer では、今回の範囲内に重大なセキュリティ指摘はありませんでした。

**Execution Flow**

- 入口は [MainNavigation.kt](simpledyphic/app/src/main/java/jp/hotdrop/simpledyphic/ui/MainNavigation.kt) の `CalendarRoute()` です。
- `CalendarScreen.kt` の月移動、日付タップ、サマリーカードタップが `CalendarRoute` 経由で `CalendarViewModel` とナビゲーションに接続されています。
- `CalendarViewModel.kt` では `observeRecords()`、`observeGoals()`、`loadWeeklyData()` が主要フローで、`CalendarUiState.kt` が表示 state の集約点です。

**Assumptions / Notes**

- `android-code-review` はリポジトリ方針で示されていますが、このセッションでは利用可能 skill 一覧に存在しなかったため、指定どおり subagent ベースで統合レビューしました。
- 残留リスクは、月変更と日付選択の副作用、週次エラー回復、初期二重ロードの回帰がテスト未整備な点です。