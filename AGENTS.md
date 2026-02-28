# AGENTS.md

このファイルは、SimpleDyphic の Android ネイティブ開発における共通運用ルールを定義する。
詳細実装手順は `skills/*/SKILL.md` に委譲し、本書は全体統制のみを扱う。

## 目的
- 開発判断を一貫させ、要求外の差分を防ぐ
- 品質を保ちながら実装速度を維持する
- 変更報告の形式を統一し、レビュー効率を上げる

## スコープ
- 対象: Kotlin + Jetpack Compose を用いた Android アプリ開発
- 非対象: クロスプラットフォーム向け実装、iOS 互換目的の抽象化追加

## 不変アーキテクチャ方針
- UI: Jetpack Compose (Material 3)
- 言語: Kotlin
- 非同期: Kotlin Coroutines + Flow
- 状態管理: `ViewModel` + `UiState`
- DI: Hilt（必須）
- 永続化: Room
- ナビゲーション: Navigation Compose
- 外部連携: Firebase, Health Connect

## 共通実装ガードレール
- 仕様変更は別タスク化し、同一タスクで混在させない
- 変更範囲は要求対象に限定し、無関係な改善を入れない
- 1ファイル肥大化を避け、責務ごとに分割する
- 文字列/色/寸法はリソースまたはテーマで管理する
- 例外は握りつぶさず、ユーザー向け表示とログを分離する
- 実装後は対象機能の検証（テストまたは手動確認）を必ず実施する
- Room スキーマ変更時は「DB version 更新 + Migration 実装 + Migration テスト追加」を同一タスクで完了させる
- Health Connect 指標の単位・権限・未取得ルールは `docs/HealthConnectMetricSpec.md` を基準に実装する

## SKILL 用途マトリクス
- UI 実装・改修: `android-compose-ui`
- ビジネスロジック実装・改修: `android-business-logic`
- Coroutines/Flow 設計・改修: `android-coroutines-flow`
- Firebase 連携実装・改修: `android-firebase`
- コードレビュー: `android-code-review`

## SKILL 適用ルール
- 本リポジトリでの SKILL 運用は `skills/<skill-name>/SKILL.md` を正とする
- 用途マトリクスに該当する作業では、対応する SKILL の手順・完了チェックを必ず満たす
- 複数領域に跨るタスクは、該当 SKILL を併用して不足観点を残さない

## 進捗管理ドキュメント標準
- 計画/進捗ボードは `docs/ActPlan.md` を使用する
- 完了報告ログは `docs/CompleteReport.md` を使用する
- タスク開始時に対象項目を `DOING` へ更新し、完了時に `DONE` と完了内容を反映する
- `CompleteReport.md` は項目を上書きせず、タスク単位で追記する

## 作業時の必須出力
- 変更ファイル一覧（プロジェクトルート基準）
- 実施内容（要点）
- 実行したテスト/確認結果
- 残課題・次アクション
- 出力更新タイミング:
  - 実装着手時: `ActPlan.md` の対象項目を更新
  - 実装完了時: 上記4項目を `CompleteReport.md` へ追記し、`ActPlan.md` を `DONE` へ更新

## 禁止事項
- クロスプラットフォーム向けコードの新規追加
- 手動 DI の新規導入
- 要求外の仕様追加や挙動変更
- 検証不能な UI 状態を残したままの提出（詳細は `android-compose-ui` を参照）

## 完了条件
- 主要導線でクラッシュや重大な表示崩れがない
- 実装内容に応じたテストまたは手動確認結果を提示できる
- 既存仕様からの無断逸脱がない
