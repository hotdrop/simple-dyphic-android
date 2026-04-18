# Codex App を使ったFlutterアプリからの移行
手順や作業メモを記録しておきます。

## 依頼手順
1. ルールファイル`AGENTS.md`, `SKILL.md`作成
   1. プロジェクト内の`flutter_app/`ディレクトリに既存のFlutterアプリのDartコードを入れる
   2. 作成依頼をする
2. ライブラリ追加（手動）
   1. Android StudioでCompose, Lifecycle, Navigation, Room, Hiltを追加しビルドが通るように実施
   2. ここは依存関係の解消や定義のやり取りをすると時間がかかるので自分で作業した
3. 移行計画策定
   1. 実装計画を立ててもらい`ActPlan.md`を作成
4. 計画に沿って実行
   1. 一旦全て実行してもらう。ただ、途中Previewが一切作られないことがわかったのでそこだけ指摘
5. 成果物をレビュー
   1. カレンダーUI

## 作業メモ
どちらが良いか？
1. 質問。どちらが良いか？
   1. このままこのThreadで作業を依頼してく
   2. 移行計画のPhaseを1ずつ別のThreadで作業依頼していく
2. Codexの回答
   1. このThreadを「統括Thread」にする
   2. 実装は「Phaseごとに別Thread」で実行する
   3. 各Phase開始時に必ず読むファイルを固定する
      1. AGENTS.md, SKILL.md, ActPlan.md
   4. 各Phase完了時に統括Threadへ結果を戻す
      1. 変更ファイル一覧
      2. テスト結果
      3. 次Phaseへの申し送り

## 依頼テンプレート

```
[ActPlan.md](docs/ActPlan.md) と [CompleteReport.md](docs/CompleteReport.md) を熟読し、以下の作業を行なってください。
- `Phase 8`を完遂させてください。その他のPhaseに記載されている作業は禁止です。
- 完了時に以下の報告を CompleteReport.md に記載してください。古いPhaseの完了報告の内容は削除し、新しい報告で更新してください。
  1. 変更ファイル一覧（プロジェクトルートからのパス）
  2. 実施内容（要点）
  3. 実行したテスト/確認結果
  4. 残課題・次Phaseへの申し送り
```

### 作業状況
- [x] Phase 0: 土台整備（ビルド可能な骨格を作る） 39kトークン使用 15%Used
- [x] Phase 1: データ層移行（Isar → Room）     50kトークン使用 19%Used
- [x] Phase 2: 画面骨格移行（ナビゲーション + タブ） 39kトークン使用 15%Used
- [x] Phase 3: カレンダー機能移行（ローカルデータ連携）
- [x] Phase 4: 記録編集機能移行（ローカル保存）
- [x] Phase 5: 設定機能移行（ローカル完結部分）
- [x] Phase 6: Firebase連携移行（Auth + Firestore）
- [x] Phase 7: Health Connect連携移行
- [x] Phase 8: 仕上げ（品質・置換完了）

# 修正依頼
- UIがFlutterのものと違っていたので見直し（7割くらいあっていたが・・）
- 全体的に設計がおかしかったため見直し。（ViewModelのScopeがUIに入り込んでいたり、Repositoryの作りやパッケージがおかしい）
- 色々と余計な仕様を実装していたので見直し（余計なダイアログ表示やUIへのオペレーション結果表示など）
- 文字列がコード直埋め or strings.xml定義、だったり日本語と英語が混在していたのでstrings.xmlに統一して日本語へ変換
