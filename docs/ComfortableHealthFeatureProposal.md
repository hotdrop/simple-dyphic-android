# 単独ユーザー向け 体調管理機能 Proposal

## 1. この文書の目的
- 対象アプリ: `SimpleDyphic`（Android / Kotlin + Jetpack Compose）
- 目的: ユーザー1名運用を前提に、体調管理に実際に効く機能を提案する
- 本文書のスコープ: **機能提案と優先度整理**
- 本文書の対象外: 実装着手、設計詳細、コード変更

## 2. 前提条件（確定）
- ユーザーは1名
- 優先価値は「継続しやすさ」ではなく「健康管理への有益性」
- Quick記録は今回の提案対象外
- 週間目標達成率は採用
- Health Connect連携は既存（歩数・kcal）から拡張する
- 週間集計の起点は**月曜**
- 目標値は**手動設定**

## 3. 現状機能と課題
### 現状（実装済み）
- 日次記録（食事、体調、排便、メモ、歩数、消費kcal、RingFit）
- カレンダーでの閲覧
- Health Connectから歩数・kcalの取り込み
- Firestoreバックアップ/リストア

### 課題
- 健康行動の「目標」と「実績」の差が分かりづらい
- 指標が分散しており、週単位の意思決定がしづらい
- 体調変化と行動データの相関が見えない
- Health Connect連携の活用幅が限定的

## 4. 提案方針
- 「入力を楽にする」よりも、「体調判断に使える情報」を優先する
- 日次より週次で振り返れる構成を中心にする
- 既存データ構造と画面導線（Calendar / Record / Settings）を大きく崩さない

## 5. 提案機能（優先度付き）

## 5.1 短期（MVP）
### A. 週間目標達成率ダッシュボード（最優先）
- 目的: 健康行動の達成状況を週単位で一目化する
- 対象指標:
  - 歩数
  - 活動消費kcal
  - 運動時間（分）
  - 移動距離（km）
  - 階段上昇（階）
- 仕様案:
  - 指標ごとに「目標値」「実績値」「達成率（%）」を表示
  - 100%超過達成も表示（上限で丸めない）
  - 集計は月曜開始の7日間
- 期待効果:
  - 週の途中で調整ができる
  - 週末の振り返りが容易になる

### B. Health Connect拡張取得（運動・距離・階段）
- 目的: 行動量の粒度を上げ、体調との関連分析を可能にする
- 追加取得候補（例）:
  - 運動時間
  - 移動距離
  - 階段上昇
- 仕様案:
  - 権限未許可時は取得可能な指標のみ反映
  - 未取得と0実績を区別して表示
- 期待効果:
  - 歩数だけでは見えない活動傾向を把握できる

### C. 体調×活動インサイト（週次）
- 目的: 体調不良が起きる前後の行動変化を見える化する
- 仕様案:
  - 体調が「悪い」日の前後2〜3日を自動集計
  - 歩数、kcal、運動時間、距離、階段の差分を表示
  - 「先週比」「平均との差」でコメント生成
- 期待効果:
  - 自分専用の悪化トリガーを把握しやすくなる

## 5.2 中期
### D. 未達/過負荷アラート
- 目的: 悪化の予兆を早めに検知する
- 仕様案:
  - 週間目標の連続未達を通知
  - 活動量の急増（過負荷）も通知対象にする
- 期待効果:
  - 無理と不足の両方を管理できる

### E. 週次レビュー自動生成
- 目的: 翌週の行動修正に直結するサマリーを残す
- 仕様案:
  - 達成率ランキング
  - 体調が良かった日の共通行動
  - 次週の調整提案（例: 距離目標を+10%）
- 期待効果:
  - 振り返りが習慣化しやすくなる

## 5.3 長期
### F. 睡眠・心拍・体重の統合管理
- 目的: 体調変化の因子をさらに拡張して捉える
- 仕様案:
  - Health Connectから睡眠/心拍/体重を段階追加
  - 既存の週間目標/インサイト表示に統合
- 期待効果:
  - 行動指標に加えて生体指標でも判断できる

## 6. 優先度マトリクス

| 機能 | 健康管理への有益性 | 実装コスト | 優先度 |
|---|---|---|---|
| 週間目標達成率ダッシュボード | 高 | 中 | S |
| Health Connect拡張（運動・距離・階段） | 高 | 中 | S |
| 体調×活動インサイト | 高 | 中 | A |
| 未達/過負荷アラート | 中 | 中 | A |
| 週次レビュー自動生成 | 中 | 低〜中 | B |
| 睡眠・心拍・体重統合 | 高 | 高 | C |

## 7. API / データ構造の追加案
※ 実装時の候補。ここでは仕様決めのたたき台として提示。

```kotlin
enum class HealthMetricType {
    STEP_COUNT,
    ACTIVE_KCAL,
    EXERCISE_MINUTES,
    DISTANCE_KM,
    FLOORS_CLIMBED
}

data class DailyHealthMetrics(
    val dateId: Int,
    val stepCount: Int?,
    val activeKcal: Double?,
    val exerciseMinutes: Int?,
    val distanceKm: Double?,
    val floorsClimbed: Int?
)

data class WeeklyGoal(
    val metricType: HealthMetricType,
    val targetValue: Double,
    val weekStartsOnMonday: Boolean = true,
    val enabled: Boolean = true
)

data class WeeklyGoalProgress(
    val metricType: HealthMetricType,
    val targetValue: Double,
    val actualValue: Double,
    val achievementRate: Double
)
```

### Repository追加/拡張案
- `HealthConnectRepository`
  - `readDailyMetrics(date: LocalDate): AppResult<DailyHealthMetrics>`
  - `readRangeMetrics(start: LocalDate, end: LocalDate): AppResult<List<DailyHealthMetrics>>`
- `GoalRepository`（新設）
  - `getWeeklyGoals()`
  - `saveWeeklyGoal(goal: WeeklyGoal)`
- `InsightRepository`（新設）
  - `buildConditionActivityInsight(week: YearWeek)`

## 8. KPI（評価指標）
- 週間目標の設定継続率（毎週更新/見直しを行えた割合）
- 週間目標の平均達成率
- 体調「悪い」日の発生頻度（週あたり）
- 週次レビュー閲覧率

## 9. テスト観点（提案段階）
1. 目標値変更時に達成率が即時再計算される
2. 一部権限拒否時でも取得可能指標のみ表示される
3. データ未取得と0実績が正しく表示上区別される
4. 週次レビュー値と日次データ集計が一致する
5. 体調不良日の相関抽出ロジックが境界日で破綻しない

## 10. 実施順（推奨）
1. 週間目標達成率ダッシュボード
2. Health Connect拡張（運動・距離・階段）
3. 体調×活動インサイト
4. 未達/過負荷アラート
5. 週次レビュー自動生成
6. 睡眠・心拍・体重統合

## 11. 補足
- 本文書は提案書であり、現時点でアプリ本体コードの実装は含まない。
- 実装に進む場合は、別タスクで画面単位（UI + ViewModel + Repository + テスト）に分割して進行する。
