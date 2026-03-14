# Comfortable Health MVP 完了報告

## 運用ルール
- 各IR完了時に追記
- 各セクションは次の4項目を必ず記載する
  - 変更ファイル一覧
  - 実施内容（要点）
  - 実行したテスト/確認結果
  - 残課題・次アクション

## IR-00
- 変更ファイル一覧
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - 実行ボードと完了報告テンプレートを初期化。
- 実行したテスト/確認結果
  - ドキュメント作成のみ（テスト対象なし）。
- 残課題・次アクション
  - IR-01へ着手。

## IR-01
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/HealthMetricType.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/MetricAvailability.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/HealthMetricValue.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/DailyHealthMetrics.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/WeeklyGoal.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/WeeklyGoalProgress.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/WeekRange.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/test/java/jp/hotdrop/simpledyphic/model/WeekRangeCalculatorTest.kt`
- 実施内容（要点）
  - 5指標対応の共通ドメイン型を追加。
  - 月曜起点の週範囲計算ユーティリティを追加。
  - 週次表示向けの文字列IDを追加。
- 実行したテスト/確認結果
  - `./gradlew :app:testDebugUnitTest` 成功（`WeekRangeCalculatorTest` を含む）。
- 残課題・次アクション
  - IR-02/03へ着手。

## IR-02
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/WeeklyGoalEntity.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/WeeklyGoalDao.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabaseMigrations.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/RoomGoalLocalDataSource.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/GoalRepository.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabase.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/di/DatabaseModule.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/data/local/db/WeeklyGoalDaoTest.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabaseMigrationTest.kt`
- 実施内容（要点）
  - `weekly_goals` テーブル/DAO/Repository を新設（ローカル限定）。
  - Room を v2 に更新し、`MIGRATION_1_2` を実装。
  - 週目標の初期値を Repository 側で補完する設計に統一。
- 実行したテスト/確認結果
  - `./gradlew :app:testDebugUnitTest` 成功（`WeeklyGoalDaoTest`, `AppDatabaseMigrationTest` を含む）。
- 残課題・次アクション
  - IR-03のHealth Connect拡張へ移行。

## IR-03
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/HealthConnectRepository.kt`
  - `app/src/main/AndroidManifest.xml`
- 実施内容（要点）
  - Health Connect の取得対象を歩数/活動kcal/運動時間/距離/階段へ拡張。
  - `readRangeMetrics(start, end)` と `getGrantedMetricTypes()` を追加。
  - 指標ごとの `AVAILABLE / PERMISSION_MISSING / SOURCE_UNAVAILABLE` を返す実装に変更。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
  - `./gradlew :app:testDebugUnitTest` 成功（既存テスト非回帰）。
- 残課題・次アクション
  - G1統合ゲート実施。

## G1
- 変更ファイル一覧
  - なし（統合確認のみ）
- 実施内容（要点）
  - IR-02/03統合後のビルド/テストゲートを実施。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
  - `./gradlew :app:testDebugUnitTest` 成功。
- 残課題・次アクション
  - IR-04/05/06へ着手。

## IR-04
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/WeeklyGoalSettingsUiState.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/WeeklyGoalSettingsViewModel.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/WeeklyGoalSettingsScreen.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsScreen.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/MainNavigation.kt`
  - `app/src/main/res/values/strings.xml`
- 実施内容（要点）
  - Settings配下に週間目標設定画面を新設。
  - Navigation子ルート `settings/weekly-goals` を追加。
  - 5指標の目標編集/保存をローカルDBへ接続。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
- 残課題・次アクション
  - IR-05/06のロジック統合。

## IR-05
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/domain/usecase/WeeklyGoalProgressUseCase.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/domain/usecase/WeeklyGoalProgressCalculator.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/WeeklyGoalSummary.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarUiState.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/domain/usecase/WeeklyGoalProgressCalculatorTest.kt`
- 実施内容（要点）
  - 週間達成率UseCaseを追加し、月曜起点7日集計・100%超過達成表示を実装。
  - `CalendarViewModel` に週次状態を追加して達成率を反映。
  - 目標更新時に再計算されるよう、Goalの監視を追加。
- 実行したテスト/確認結果
  - `./gradlew :app:testDebugUnitTest` 成功（`WeeklyGoalProgressCalculatorTest` を含む）。
- 残課題・次アクション
  - IR-06のインサイトロジック統合。

## IR-06
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/InsightRepository.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/ConditionWindowCalculator.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/WeeklyConditionActivityInsight.kt`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/RecordRepository.kt`
  - `app/src/test/java/jp/hotdrop/simpledyphic/data/repository/ConditionWindowCalculatorTest.kt`
- 実施内容（要点）
  - BAD日の前後2日窓で集計する体調×活動インサイトを実装。
  - 先週比/平均との差の差分計算とルールベースコメント生成を追加。
  - 境界日（月跨ぎ）を扱うウィンドウ計算ロジックを分離。
- 実行したテスト/確認結果
  - `./gradlew :app:testDebugUnitTest` 成功（`ConditionWindowCalculatorTest` を含む）。
- 残課題・次アクション
  - G2統合ゲートへ移行。

## G2
- 変更ファイル一覧
  - なし（統合確認のみ）
- 実施内容（要点）
  - IR-04/05/06統合後のゲート確認を実施。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
  - `./gradlew :app:testDebugUnitTest` 成功。
- 残課題・次アクション
  - IR-07 UI反映へ移行。

## IR-07
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt`
  - `app/src/main/res/values/strings.xml`
- 実施内容（要点）
  - Calendar画面に「週間目標達成率」「体調×活動インサイト」カードを追加。
  - 未取得（権限未許可）と0実績を表示上で区別。
  - 既存カレンダー導線（選択/編集遷移）は維持。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
- 残課題・次アクション
  - IR-08で回帰確認と文書締め。

## IR-08
- 変更ファイル一覧
  - `app/src/androidTest/java/jp/hotdrop/simpledyphic/phase8/Phase8UserFlowUiTest.kt`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - 既存UIテストを新シグネチャに追従。
  - AndroidTest Kotlinコンパイルを追加で実施。
  - 実装進捗文書を最終更新。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
  - `./gradlew :app:testDebugUnitTest` 成功。
  - `./gradlew :app:compileDebugAndroidTestKotlin` 成功。
- 残課題・次アクション
  - MVP(A〜C)としては完了。次は中期機能（D/E）への展開検討。

## CH-2026-03-14
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - 未使用になった Health Connect 指標を型・画面・Repository・Manifest から削除し、4指標構成に統一。
  - Health Connect 指標仕様ドキュメントと進捗ログを実装内容に合わせて更新。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
- 残課題・次アクション
  - 必要に応じて実機またはエミュレータで Calendar 画面の表示順と欠落項目を目視確認する。

## CH-2026-03-14-PREVIEW
- 変更ファイル一覧
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt`
  - `docs/ActPlan.md`
  - `docs/CompleteReport.md`
- 実施内容（要点）
  - `WeeklyDashboardCard` を単体確認できる Preview を追加。
  - 通常表示に加えて `Loading` と `Error` の Preview も用意し、状態ごとの差分を確認しやすくした。
  - Preview 用の `CalendarUiState` サンプルデータを追加し、権限未許可・取得不可の行も再現した。
- 実行したテスト/確認結果
  - `./gradlew :app:compileDebugKotlin` 成功。
- 残課題・次アクション
  - Android Studio の Compose Preview 上でカードの改行量と情報密度を目視確認する。
