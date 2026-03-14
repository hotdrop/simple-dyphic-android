# Health Connect 指標仕様（SimpleDyphic）

本書は、Health Connect 連携で扱う指標の単位・権限・表示ルールを定義する。

## 指標一覧
| MetricType | 表示名 | 単位 | Health Connect Record | 必須権限 |
|---|---|---|---|---|
| `STEP_COUNT` | 歩数 | 歩 | `StepsRecord` | `android.permission.health.READ_STEPS` |
| `ACTIVE_KCAL` | 活動消費kcal | kcal | `TotalCaloriesBurnedRecord` | `android.permission.health.READ_TOTAL_CALORIES_BURNED` |
| `EXERCISE_MINUTES` | 運動時間 | 分 | `ExerciseSessionRecord` | `android.permission.health.READ_EXERCISE` |
| `DISTANCE_KM` | 移動距離 | km | `DistanceRecord` | `android.permission.health.READ_DISTANCE` |

## availability 定義
- `AVAILABLE`: データ取得可能（値が `0` の場合も含む）
- `PERMISSION_MISSING`: 指標に必要な権限が未許可
- `SOURCE_UNAVAILABLE`: 連携不可、SDK未使用可能、読み取り失敗などで値を確定できない

## 表示ルール
- `AVAILABLE` かつ値 `0` は「0」と表示する（未取得扱いにしない）
- `PERMISSION_MISSING` は「未取得（権限未許可）」
- `SOURCE_UNAVAILABLE` は「未取得（連携元データなし）」

## 実装ルール
- 複数日集計時は指標ごとの availability を保持したまま上位へ渡す
- 週間達成率計算は `AVAILABLE` の値のみを集計対象とする
- UI は availability と値の両方を使って表示を決定する
