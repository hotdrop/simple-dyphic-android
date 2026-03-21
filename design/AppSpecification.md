# SimpleDyphic 現在仕様書（設計寄り）

## 1. 文書目的・対象読者・非対象
### 1.1 文書目的
- 本書は、SimpleDyphic の「現時点で提供している機能」を設計観点で整理することを目的とする。
- 実装変更の提案ではなく、現行コードの仕様を把握しやすくすることを主眼とする。

### 1.2 対象読者
- プロダクトオーナー
- 設計レビュー担当
- 次フェーズで実装/テストを行う開発者

### 1.3 非対象
- 詳細実装手順
- 将来機能の仕様確定

仕様確度: コード根拠あり

## 2. システム全体像
### 2.1 対象プラットフォーム
- Android 専用アプリ（Kotlin / Jetpack Compose）
- 最小 SDK: 34
- 目標 SDK: 36

### 2.2 アーキテクチャ概要
- UI: Compose + Navigation Compose
- 状態管理: `ViewModel` + `StateFlow`
- 非同期: Coroutines + Flow
- DI: Hilt
- ローカル保存: Room
- 外部連携: Firebase Auth / Firestore / Health Connect

### 2.3 起動構成
- `App` (`@HiltAndroidApp`) でアプリ初期化
- `MainActivity` -> `MainNavigation` を起動
- 画面の主軸は `calendar` と `settings`

仕様確度: コード根拠あり

## 3. 画面構成と遷移仕様
### 3.1 ルート構成
- Top-level ルート:
  - `calendar`
  - `settings`
- 詳細ルート:
  - `record/{recordId}`

### 3.2 ボトムナビゲーション
- `calendar` / `settings` 画面で表示。
- `record/{recordId}` では非表示。

### 3.3 画面間データ受け渡し
- 記録編集画面の戻り時に `recordUpdated: Boolean` を返却。
- `calendar` 側は `recordUpdated` を受け取ったら消費し、エラー表示状態を解除する。

仕様確度: コード根拠あり

## 4. 機能仕様
## 4.1 Calendar
### できること
- 月移動（前月/翌月）
- 日付選択
- 同じ日付を再タップで記録編集画面へ遷移
- 日付ごとのマーカー表示
- 選択日のサマリー表示

### 条件
- 表示範囲は `現在月 ±2年`
- データは `RecordRepository.observeAll()` を継続購読

### 成功時挙動
- レコード更新が入るとカレンダー表示とマーカーが自動更新される
- サマリーカードは選択日を表示し、主に体調メモを表示する

### 失敗時挙動
- レコード購読に失敗した場合、エラーメッセージとリトライUIを表示

### マーカー条件
- 排便記録あり
- 体調記録あり
- RingFit kcal または km が 0 より大きい
- 歩数が 7000 以上

仕様確度: コード根拠あり / テスト根拠あり（一部）

## 4.2 Record Edit
### できること
- 食事（朝/昼/夜）入力
- 体調（悪い/普通/良い）選択
- 体調メモ入力
- 排便チェック
- Health Connect 日次値（歩数・kcal）取り込み
- RingFit 値（kcal/km）入力
- 保存

### 条件
- `recordId` で対象日を特定
- 既存レコードがない場合は空レコードとして編集開始
- 画面初回表示時に Health Connect 同期処理を試行

### 成功時挙動
- 保存成功時:
  - 変更があれば `onBack(true)` で戻る
  - 変更がなければ `onBack(false)` で戻る
- Health Connect 同期成功時:
  - 歩数/kcal を画面状態へ反映

### 失敗時挙動
- RingFit 入力が数値でない場合はエラーメッセージ表示
- 保存失敗時はエラーメッセージ表示
- Health Connect 非対応/権限拒否/取得失敗時はメッセージ表示

### 追加制御
- 未保存変更ありで戻る操作を行うと破棄確認ダイアログを表示
- 保存中は保存ボタンを無効化
- Health 同期中は同期トリガーUIを無効化

仕様確度: コード根拠あり / テスト根拠あり（一部）

## 4.3 Settings
### できること
- Google サインイン
- Google サインアウト
- Firestore バックアップ
- Firestore リストア
- バージョン/ライセンス表示

### 条件
- バックアップ/リストア項目はサインイン中のみ表示
- バックアップ/リストア実行は確認ダイアログ経由
- 操作中は `isLoading` により多重操作抑止

### 成功時挙動
- サインイン成功でアカウント情報をUI反映
- サインアウト成功でアカウント情報をクリア
- バックアップ/リストア成功で待機状態へ戻る

### 失敗時挙動
- サインイン/サインアウト/バックアップ/リストア失敗時はログ出力
- 画面上の専用エラートースト/履歴表示は現状なし

仕様確度: コード根拠あり / テスト根拠あり（一部）

## 5. データ仕様
## 5.1 主要ドメインモデル
### `Record`
- `id: Int` (`yyyyMMdd`)
- `breakfast: String?`
- `lunch: String?`
- `dinner: String?`
- `isToilet: Boolean`
- `condition: ConditionType?`
- `conditionMemo: String?`
- `stepCount: Int?`
- `healthKcal: Double?`
- `ringfitKcal: Double?`
- `ringfitKm: Double?`

### `DyphicId`
- `LocalDate <-> yyyyMMdd(Int)` を相互変換

### `ConditionType`
- `BAD`, `NORMAL`, `GOOD`
- 永続化時の文字列変換:
  - `悪い`, `普通`, `良い`

## 5.2 画面状態モデル
- `CalendarUiState`
- `RecordEditUiState`
- `SettingsUiState`

仕様確度: コード根拠あり

## 6. 外部連携仕様
## 6.1 Room
- DB名: `simpledyphic.db`
- テーブル: `records`
- 主キー: `id`
- upsert: `OnConflictStrategy.REPLACE`
- `replaceAll()` は `DELETE -> INSERT` のトランザクション

## 6.2 Firebase Auth
- Credential Manager + Google ID Token を使用
- Firebase Auth へサインインして `UserAccount` を構築

## 6.3 Firestore
- 保存先:
  - `dyphic/{userId}/records/{recordId}`
- 保存方式:
  - `SetOptions.merge()`
- リストア方式:
  - Firestore 全件取得 -> ローカル `replaceAll`

## 6.4 Health Connect
- 取得対象（現状）:
  - 歩数 (`StepsRecord.COUNT_TOTAL`)
  - 総消費カロリー (`TotalCaloriesBurnedRecord`)
- ステータス:
  - 利用可 / 要アップデート / 未インストール
- 必須権限:
  - `READ_STEPS`
  - `READ_TOTAL_CALORIES_BURNED`

仕様確度: コード根拠あり

## 7. 状態管理・エラー/ローディング方針
### 7.1 状態管理
- ViewModel 基盤は `BaseViewModel` を継承
- 非同期起動は `launch {}` 統一
- I/O は `dispatcherIO` を利用

### 7.2 エラー表現
- Repository 層は `AppResult` / `AppCompletable` で成功/失敗を返却
- UI は `errorMessageResId` などの状態に応じて表示切替

### 7.3 共通UI
- 読み込み: `LoadingContent`
- 失敗: `ErrorContent`（必要時リトライ導線あり）

仕様確度: コード根拠あり

## 8. 権限・セキュリティ・バックアップ方針
### 8.1 権限
- `INTERNET`
- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_TOTAL_CALORIES_BURNED`

### 8.2 セキュリティ/運用
- `allowBackup=false`
- `data_extraction_rules` を使用
- Firebase/Health Connect を利用するためネットワークおよび連携アプリ状態に依存

### 8.3 バックアップ運用
- サインイン済み時のみ実行可能
- 未サインイン時は backup/restore ともに実質 no-op 完了

仕様確度: コード根拠あり

## 9. 既知の制約・未実装/未保証事項
- 単一ユーザー利用を前提にしたUI設計（複数ユーザー切替UIなし）
- Health Connect 連携は歩数・総消費kcalのみ（睡眠/心拍/距離など未対応）
- Calendar サマリーは主に体調メモ中心で、食事/運動の詳細要約は未提供
- Settings は操作失敗時のユーザー向け詳細メッセージが限定的
- Firestore 保存は `merge` 方式のため、欠損項目削除の厳密同期は未保証
- 文言は日本語中心だが一部英語混在（例: `Sign In with Google`）
- UI テストのカバレッジは主要導線の一部に限定

仕様確度: コード根拠あり / 暫定（運用時挙動確認不足を含む）

## 10. テスト実施範囲（現状）
### 10.1 実装済みテスト
- Unit Test:
  - `RecordDaoTest`
  - 保存/単体取得
  - ID順取得
- UI Test:
  - `calendar -> record -> save` 導線（画面コンポーネントベース）
  - `settings -> backup confirm` 導線（画面コンポーネントベース）

### 10.2 未整備/限定的な範囲
- ViewModel の状態遷移ユニットテスト
- Firebase/Health Connect 連携の統合テスト
- 実機での包括的リグレッション自動化

仕様確度: テスト根拠あり / 暫定（未カバー領域あり）

## 11. 用語集
- **DyphicId**: 日付を `yyyyMMdd` の整数で表すID形式
- **Record**: 1日分の体調・食事・活動記録
- **recordUpdated**: 記録編集後にカレンダーへ返す更新有無フラグ
- **Health Connect**: 歩数・総消費kcalの取得元となるAndroid健康データ基盤
- **Backup/Restore**: ローカルRoomとFirestore間の同期操作

仕様確度: コード根拠あり

## 別資料参照（将来提案）
- 将来機能の提案は本書に含めず、以下を参照:
  - `/Users/hotdrop/Desktop/MyWorkSpace/android/simpledyphic/docs/ComfortableHealthFeatureProposal.md`

仕様確度: コード根拠あり
