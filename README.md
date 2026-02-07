# このアプリについて
Codex Appを使ってFlutterアプリをAndroidのComposeで移植してもらった。

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