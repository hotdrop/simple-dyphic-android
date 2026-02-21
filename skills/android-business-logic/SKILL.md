---
name: android-business-logic
description: このアプリの ViewModel・UseCase・Repository を実装/改修するスキル。状態遷移、入力検証、AppResult/AppCompletable、Hilt DI、Room/Firebase/Health Connect をまたぐ業務ロジックを追加するときに使う。
---

# Android Business Logic Skill

## 設計方針
- UI・状態管理・データアクセスを分離する。
- 画面状態を `data class` または `sealed interface/class` で明示する。
- ViewModel から Repository を呼び、Repository から DataSource を呼ぶ層構造を維持する。
- 新規 DI は Hilt を必須にし、手動 DI を追加しない。

## ViewModel 実装ルール
- `BaseViewModel` を継承し、非同期開始は `launch {}` を使う。
- 状態更新は `_uiState.update { ... }` で一元化する。
- 1回限りイベントは `Channel` + `receiveAsFlow()` を使う。
- 入力検証失敗は `errorMessageResId` などの UI 状態に反映する。
- 例外は握りつぶさず、ユーザー表示用状態とログ出力を分離する。

## Repository 実装ルール
- 返り値は既存方針どおり `AppResult` / `AppCompletable` を使う。
- 例外を上位へ投げず、失敗型に変換して返す。
- 複数データソースを跨ぐ処理は Repository に集約する。
- I/O の詳細は DataSource に閉じ、Repository では業務判断に専念する。

## テストルール
- ViewModel の状態遷移と入力検証をユニットテストする。
- Repository の主要分岐（成功/失敗/未ログイン分岐など）をユニットテストする。
- 仕様変更を伴う場合は同等性を壊していないか手動確認手順も残す。

## 完了チェック
- 層分離が壊れていない。
- Hilt 構成に統一されている。
- 失敗ケースが UI 状態とログに適切反映される。
- 主要分岐のテストが追加または更新されている。
