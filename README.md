# PADAutoSolver

パズドラの画面認識、ルート探索、AccessibilityServiceによる連続ドラッグを行うAndroidアプリです。

現在は **0.4.0-dev77 / versionCode 80** の検証用開発版です。前回の開発検証では更新・停止・再開を挟みB1〜B22の実出現分岐とCLEAR・報酬を確認しました。**同じ最終APKでの新規潜入全階通し、別出現分岐、安定周回は未達です。**

10/7の開発再開で、B1の最初の交換欠落、強化/コンボ/毒の表示差、使用後の名前読取を修正し、実Battle3まで到達しました。本体でB2の毒・0コンボ・全消しとB3の回復・全闇更新を実行。dev77はCD4→3を確認した後に存在しない戻るボタンを待つ不具合と、突破済み階層の引き継ぎ時の画像再読取を修正。JVM256件・署名Release・両lint成功。最終版の実機結果とCIは[10/7検証記録](docs/ura-shura/stability-validation-2026-10-07.md)を参照。新規試行3回、無中断クリア0回、同一APK20試行基準は未達です。

## 現在の開発状況

- [現在の実装・検証状況](docs/ura-shura/implementation-status.md)
- [開発再評価と修正の優先順位（2026-10-05）](docs/ura-shura/development-review-2026-10-05.md)
- [次の開発者向けの入口](docs/ura-shura/handoff-prompt.md)

2026-10-05の続行指示を受け、B20ヨウユウ、B21メノア、B22闇メノアを本体で攻略しました。クリア後は結果確認専用の状態に移り、TIPSを本体で閉じ、獲得コイン5,739,050・獲得EXP1,390,782の報酬画面を確認しました。再挑戦せず停止します。各版・日時・証跡は実機記録を参照してください。

## モード

| モード・機能 | 現状と制約 |
| --- | --- |
| 通常パズル・周回 | 通常6色、6×5/7×6、盤面探索とドラッグ。裏魔門の攻略とは別の処理。最終版での回帰試験が必要 |
| 売却 | 専用UIと周回から分離したモード。現在の利用者条件は「合計MP30なら売却、それ以外は停止」。最終版での通し回帰試験が必要 |
| BOX_SCAN | ページと詳細画像の実機取得、再開、診断保存。全個体の確定同定・育成情報の構造化は未完了 |
| 裏魔門の守護者 | 利用者が選んだ固定編成と実測端末に合わせた攻略。今回のB1〜B22を更新・再開を挟んで個別検証。B22は闇分岐のみ対応 |
| ルート確認 | 盤面と目的に対する探索・検算。全階の戦闘リプレイを保証するものではない |

モードを切り替えるときは、実行中の処理を停止してから設定します。画面認識と自動操作はPAD Auto Solver本体で行い、USBは開発・インストール・観察に使用します。

## 裏魔門で使用する固定編成

エスペル（No.14094）、セッカ（No.7333）、青オーディン（No.3391）、ユキネ（No.10042）、花嫁ルカ（No.2955）、助っ人ミオン（No.9411）。詳細は保存済みのTeamProfileと実機検証記録を参照してください。

利用者の指定により、潜入前は助っ人のミオンだけを確認します。自分の5枠の再照合やBOX再走査を潜入条件に戻しません。戦闘中のスキル・盤面・再開地点の確認は引き続き必要です。

## 利用に必要な許可

アプリ内の案内に従ってオーバーレイとユーザー補助を許可し、画面キャプチャを開始します。画面共有はAndroidの同意画面を伴います。APKの更新などで共有が終了した場合は再開が必要です。認識不明・操作条件不一致・利用者の停止指定では停止し、原因と保存記録を確認します。

## ビルドと検証

Java 17、Gradle Wrapper 8.9、compileSdk/targetSdk 35、minSdk 26を使用します。AndroidX設定は`gradle.properties`に保持しています。

```ini
android.useAndroidX=true
android.enableJetifier=true
```

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

dev52のローカルJVMは215件成功、失敗・エラー0件。署名Releaseビルドとdebug/release lintも成功しています。前段のB22攻略実装コミット`0661bd03fe46dcf1ae143c125299492e316641c7`の[Actions Run 37249907382](https://github.com/iwa45645/PADAutoSolver/actions/runs/37249907382)もdebug検証・固定署名previewが成功しています。最終コミットのCI結果は配布build-infoを参照してください。ロジック検証と最終APKだけの全階通し試験は別です。

## 署名・配布

- Repository: `iwa45645/PADAutoSolver`
- applicationId: `com.example.padautosolver`
- 開発ブランチ: `feature/ura-shura-full-auto`
- 固定署名を維持し、同一packageNameで上書き更新します。
- keystoreとパスワードはコード・リポジトリへ保存しません。
- Actions Secrets: `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。
- ローカルReleaseは既存の`ANDROID_KEYSTORE_PATH`と署名用環境変数を使います。新しい鍵は生成しません。

[通常ビルドworkflow](.github/workflows/build-apk.yml)と[Release workflow](.github/workflows/release-apk.yml)を使用します。開発用previewと安定版Releaseは区別してください。アプリ内の更新参照先はこの専用リポジトリです。更新チェックの24時間制限は起動時処理であり、常駐して定刻実行する機能ではありません。

## 設計と検証記録

- [原設計書 v1.1](docs/ura-shura/design-v1.1.md) / [原マイルストーン](docs/ura-shura/milestones.yaml)
- [2026-10-02：B1](docs/ura-shura/runtime-testing-2026-10-02.md)
- [2026-10-03：B2](docs/ura-shura/runtime-testing-2026-10-03.md)
- [2026-10-04：B3〜B5](docs/ura-shura/runtime-testing-2026-10-04.md)
- [2026-10-05：B6〜B22、クリア・報酬確認](docs/ura-shura/runtime-testing-2026-10-05.md)
- [整理前のREADME](docs/ura-shura/history/readme-before-review-2026-10-05.md) / [過去の進捗全文](docs/ura-shura/history/implementation-status-before-review-2026-10-05.md)

原設計と過去の記録は履歴として保持しています。現行の利用者条件・到達点は「現在の実装・検証状況」を優先して参照してください。
