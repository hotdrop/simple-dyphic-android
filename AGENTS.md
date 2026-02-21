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

## SKILL 用途マトリクス
- UI 実装・改修: `android-compose-ui`
- ビジネスロジック実装・改修: `android-business-logic`
- Coroutines/Flow 設計・改修: `android-coroutines-flow`
- Firebase 連携実装・改修: `android-firebase`
- コードレビュー: `android-code-review`

## 作業時の必須出力
- 変更ファイル一覧（プロジェクトルート基準）
- 実施内容（要点）
- 実行したテスト/確認結果
- 残課題・次アクション

## 禁止事項
- クロスプラットフォーム向けコードの新規追加
- 手動 DI の新規導入
- 要求外の仕様追加や挙動変更
- 検証不能な UI 状態を残したままの提出（詳細は `android-compose-ui` を参照）

## 完了条件
- 主要導線でクラッシュや重大な表示崩れがない
- 実装内容に応じたテストまたは手動確認結果を提示できる
- 既存仕様からの無断逸脱がない
