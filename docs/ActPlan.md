# SimpleDyphic Flutter → Jetpack Compose 段階移行 実装計画

## 1. 目的と前提
- 目的: `flutter_app/` の既存機能を Android 専用の Kotlin + Jetpack Compose に段階移行する。
- 方針: 機能同等性を優先し、仕様変更は移行完了後に別タスクで実施する。
- DI: Hilt を必須とし、手動DIは追加しない。
- カレンダー実装: `com.kizitonwose.calendar` を使用する（他ライブラリへ置換しない）。
- 参考実装: `./sampleCodeCalendar/CalendarScreen.kt` と `./sampleCodeCalendar/CalendarViewModel.kt` をベースに移植する。

## 2. 現状サマリ（棚卸し結果）
### Flutter側の主要機能
- アプリ初期化: Firebase初期化、Google Sign-In初期化、ローカルDB初期化、Health状態判定
- 画面構成: BottomNavigation（カレンダー / 設定）
- 記録管理:
  - 日付ID（`yyyyMMdd`）ベースの記録
  - カレンダー表示 + 日付選択
  - 記録編集（食事、体調、排便、メモ、歩数/kcal、RingFit）
- 設定:
  - Googleサインイン/サインアウト
  - Firestoreバックアップ/復元
  - バージョン表示

### 既存Android側（移行先）
- 導入済み: Compose / Lifecycle / Navigation / Room / Hilt
- 追加検討が必要: Firebase系、Health Connect系、Google Sign-In系

## 3. 移行戦略
- 垂直スライスで移行する（基盤 → データ層 → 画面）。
- 1フェーズごとに「動作確認可能な状態」を作る。
- 外部連携（Firebase, Health Connect）は、ローカル完結機能の移行後に接続する。

## 4. フェーズ計画

## Phase 0: 土台整備（ビルド可能な骨格を作る）
### 実施内容
- Composeアプリのエントリポイントを作成（`MainActivity` + `setContent`）。
- `Application` (`@HiltAndroidApp`) と Manifest の関連を確認。
- パッケージ構成を確定。
  - `core/`（共通）
  - `data/`（local, remote, repository）
  - `domain/`（必要最小限）
  - `feature/calendar`, `feature/record`, `feature/settings`
  - `di/`
- 例外/ログ方針を定義（少なくとも `Logcat` 出力の統一窓口を1つ用意）。

### 完了条件
- アプリがクラッシュなく起動し、空のホーム画面を表示できる。
- Hilt注入が1箇所以上で動作する。

## Phase 1: データ層移行（Isar → Room）
### 実施内容
- `Record` ドメインモデルを Kotlin で定義。
- `DyphicId`（`yyyyMMdd` 変換）を Kotlin 実装。
- Room Entity / DAO / Database を作成。
- LocalDataSource相当を Roomベースで実装。
- Repositoryインターフェース + 実装（ローカルのみ）を作成。
- Hilt ModuleでDB/DAO/Repositoryを注入。

### 完了条件
- 記録の `保存 / 単体取得 / 全件取得` が端末内で完結して動作する。
- DAOとRepositoryのユニットテストが通る。

## Phase 2: 画面骨格移行（ナビゲーション + タブ）
### 実施内容
- Navigation Composeでルート構成を作る。
- BottomNavigation（カレンダー / 設定）をComposeで実装。
- 画面単位の `UiState` と `ViewModel` 雛形を作成。
- ローディング/エラー共通UIを用意。

### 完了条件
- カレンダー画面と設定画面を相互遷移できる。
- 各画面で `ViewModel + StateFlow` が接続されている。

## Phase 3: カレンダー機能移行（ローカルデータ連携）
### 実施内容
- `com.kizitonwose.calendar` でカレンダー表示（マーカー含む）をCompose実装。
- `sampleCodeCalendar/` のサンプル構成（Screen + ViewModel）を取り込み、SimpleDyphicのデータモデルに合わせて調整。
- 日付選択で当日記録のサマリを表示。
- 日付タップから記録編集画面への遷移を実装。
- `refresh` 相当（編集後再読み込み）を実装。

### 完了条件
- Flutter同等のカレンダー閲覧・選択体験を再現できる。
- ローカルDBの内容がカレンダーに正しく反映される。

## Phase 4: 記録編集機能移行（ローカル保存）
### 実施内容
- 記録編集UIをComposeで実装。
  - 食事入力（朝/昼/夜）
  - 体調選択
  - 体調メモ
  - 排便チェック
  - RingFit入力（kcal, km）
- 「未保存で戻る」確認ダイアログを実装。
- 保存処理と保存後の戻り値（更新有無）を実装。

### 完了条件
- 記録の編集・保存・再表示が一連で動作する。
- 主要入力の状態遷移テストが通る。

## Phase 5: 設定機能移行（ローカル完結部分）
### 実施内容
- 設定画面UIをComposeで実装。
- バージョン表示、ライセンス表示を実装。
- サインイン状態に応じたUI切替の骨格を実装（実認証は次Phase）。

### 完了条件
- Flutter同等の設定画面構造を再現できる。
- サインイン状態フラグで表示分岐できる。

## Phase 6: Firebase連携移行（Auth + Firestore）
### 実施内容
- Firebase BOMを採用し、以下を導入。
  - Firebase Auth
  - Cloud Firestore
- Google Sign-In（Credential Manager 連携を優先検討）を導入。
- 認証サービス、Firestoreデータソース、Repository連携を実装。
- 設定画面から `サインイン / サインアウト / バックアップ / 復元` を接続。

### 完了条件
- Googleサインイン/サインアウトが実機で成功する。
- バックアップ/復元がFirestoreで動作する。

## Phase 7: Health Connect連携移行
### 実施内容
- Health Connect SDK（AndroidX）を導入。
- 権限確認・権限リクエスト・歩数/消費カロリー取得を実装。
- 記録画面に反映し、既存値との上書き判定を実装。
- FlutterのMethodChannel依存は使わず、Androidネイティブ実装に統一。

### 完了条件
- 対応端末で歩数/消費kcalを取得し、記録画面に反映できる。
- 非対応時のエラーハンドリングをUIで案内できる。

## Phase 8: 仕上げ（品質・置換完了）
### 実施内容
- 主要導線のUIテスト追加（カレンダー→記録→保存、設定→バックアップ等）。
- リグレッション確認（手動テストシナリオ固定化）。
- 不要になったFlutter資産の段階整理（削除は最後に実施）。
- リリースビルド確認。

### 完了条件
- 主要導線でクラッシュなし。
- ユニット/UIテストが安定して通る。
- Flutter側を更新せずAndroid実装のみで開発継続可能。

## 5. 実装順序（最小失敗ルート）
1. Phase 0
2. Phase 1
3. Phase 2
4. Phase 3
5. Phase 4
6. Phase 5
7. Phase 6
8. Phase 7
9. Phase 8

## 6. リスクと対策
- Firebase/Google認証仕様差分:
  - 対策: 認証はPhase 6で単独検証し、UI層より先にサービス層を実機確認する。
- Health Connect端末差異:
  - 対策: 非対応・未インストール・権限拒否の3状態を先に実装する。
- Isar→Room差分によるデータ整合性:
  - 対策: `Record` 変換テストを先に作成し、`id/date` 変換の回帰を防ぐ。
- 一括移行による複合バグ:
  - 対策: 各Phaseで必ず「完了条件」を満たしてから次へ進む。

## 7. 直近アクション（着手タスク）
- `MainActivity` と Compose `App()` を作成する。
- `Record` / `DyphicId` の Kotlin実装を追加する。
- Roomスキーマ（Entity/Dao/Database）を先に実装する。
- Hilt ModuleでRoomとRepositoryを注入可能にする。
