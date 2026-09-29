# BOX_SCAN 開発版 0.4.0-dev03

固定編成は未決定です。まず通常のモンスターBOXを読み取り、所持個体と不明項目を記録します。裏修羅自動攻略はまだ有効化していません。

1. 「BOX_SCAN」を選択。
2. 「所持一覧・BOXキャリブレーション」で列数、所持数、グリッド範囲を設定し、先頭・ALL・検索空欄・フィルタなしを確認。
3. 画面全体を共有し、通常のモンスターBOXへ戻って「新規」。売却画面には対応しません。
4. 走査中は画面を触らず待ちます。検出不能、重なりの曖昧さ、個体数の不一致は停止理由として保存します。
5. 所持一覧で不明個体の番号・名前を詳細画面と照合。候補の詳細タブを「詳細」で保存。
6. 停止位置が変わっていなければ「再開」で保存済みページと照合して続行できます。所持一覧の「保存済みページから再集計」は保存画像から未確認一覧を作り直し、元セッションも保持します。
7. 診断ZIPは所持一覧の保存ボタンから端末Downloadへ出力できます。inventory JSONは明示的な共有操作で取り出せます。画像・JSONは端末のアプリ専用領域に保持し、自動送信しません。

カタログを導入していないため、画像を取り込んだだけでは名前・ID・育成状態は確定しません。`completeScan=false` と走査途中/未確認の状態を区別して表示します。末尾の静止だけでは全走査を完了にしません。表示枠数（素材スタックを含む）と画面の所持数を別に記録し、件数照合・個体同定まで未完了を維持します。

`MonsterCatalogProvider` のJSONインポートは `source`, `license`, `version`, `monsters` が必須です。各レコードの `monsterId` と任意の `iconHash`（64bit 16進文字列）を使い、候補を生成します。候補を所持確認済みに自動昇格させません。

自動操作はゲームの利用規約に抵触する可能性があります。root、ゲーム改変、通信・メモリ改変、検知回避は使用しません。

実装済みと実機確認済みの範囲は [実装状況](docs/ura-shura/implementation-status.md) を参照してください。通常・周回・売却の既存機能は以下に記録されています。

---

# PAD Auto Solver 0.2.1

パズル＆ドラゴンズの通常盤面を、Android端末上で「画面キャプチャ → 盤面色認識 → ルート探索 → 自動ドラッグ」するサイドロード用Androidアプリです。

## v0.2.0 の追加点

- GitHub Actionsでdebug APKを自動ビルド
- 固定の署名鍵をGitHub Secretsから読み込み、更新可能なrelease APKを作成
- `v0.2.0` のようなタグをpushするとGitHub ReleaseへAPKを添付
- アプリ内の「アプリの更新を確認」からGitHub Releasesの最新版を確認
- 新版があればAPKをダウンロードし、Androidのインストール画面を開く
- 消去後の重力による追加コンボまで評価し、コンボ数→消去数→短い手数の順でルートを選択
- ドラッグは1本の連続ストローク。未知の新規ドロップによる落ちコンは予測しない
- 更新APKのパッケージ名・署名・versionCodeをインストール画面の前に検査
- キャプチャ停止・画面サイズ変更後の古いルート実行を抑止

> 自動更新チェックはGitHub APIへ匿名アクセスするため、リポジトリを非公開にすると利用できません。非公開運用の場合はActions Artifact/Releaseから手動でAPKを取得してください。

## 対応範囲

- Android 8.0+（minSdk 26）
- 6×5通常盤面
- 7×6盤面（設定で列数を7に変更）
- 通常6色（火・水・木・光・闇・回復）
- Beam Searchによるルート探索
- キャプチャ開始後、パズドラを表示すると盤面安定を確認して自動で操作を継続
- 赤い「停止」でキャプチャと自動操作を終了。緑のボタンで連続操作を一時停止・再開
- 「◎」は手動で1回だけ操作する補助ボタン
- 「◎」長押しで認識中の盤面グリッドを約1.6秒表示
- 盤面下端余白・左右余白・探索手数・探索幅・操作時間を調整可能

初版系の色認識は、毒・猛毒・お邪魔・爆弾・ルーレット・特殊なドロップスキンを保証していません。

## アプリ更新で最も重要なこと

Androidは、既にインストール済みのアプリを上書き更新する際に**同じapplicationIdと同じ署名鍵**を要求します。
このプロジェクトは `com.example.padautosolver` を維持し、ReleaseビルドではGitHub Secretsの固定署名鍵を使います。

署名鍵は紛失しないでください。鍵が変わると、旧版をアンインストールしない限り更新できなくなります。

## GitHub Secrets

リポジトリの `Settings → Secrets and variables → Actions` に以下4個を登録します。

- `ANDROID_KEYSTORE_BASE64`: `.jks` ファイルをbase64化した文字列
- `ANDROID_KEYSTORE_PASSWORD`: keystoreのパスワード
- `ANDROID_KEY_ALIAS`: キーのalias
- `ANDROID_KEY_PASSWORD`: キーのパスワード

既存の署名バックアップを使ってください。更新用に鍵を新規作成しないでください。

このプロジェクトの固定署名証明書SHA-256（秘密鍵ではありません）:

`8F0319A7674AC4C94CAE72F7F53C887D7D909FC997E0852F674FAD41A3994FFC`

初回に別プロジェクト用の署名鍵を作る場合の参考:

```bash
keytool -genkeypair -v -keystore pad-release.jks -alias padautosolver -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 pad-release.jks
```

Windows PowerShellでbase64化する場合:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("pad-release.jks"))
```

`.jks` 自体をGitリポジトリへコミットしないでください。

## GitHub Actions

### debug APK

`.github/workflows/build-apk.yml`

main/masterへのpush、Pull Request、または手動実行で `PADAutoSolver-debug` Artifactを作ります。

### 更新用release APK

`.github/workflows/release-apk.yml`

署名Secretsを設定したあと、例えば以下のタグを作ります。

```bash
git tag v0.2.0
git push origin v0.2.0
```

Actionsが `PADAutoSolver-release.apk` をビルドし、GitHub Releaseへ添付します。

次回は `app/build.gradle` の `versionCode` を必ず増やし、`versionName` も `0.2.1` 等へ更新したうえで `v0.2.1` タグを作成してください。

## アプリ内更新

メイン画面の「アプリの更新を確認」を押すと、固定の `iwa45645/PADAutoSolver` の最新Releaseを確認します。別リポジトリでビルドしても更新先は変わりません。

新版がある場合はAPKをアプリ専用領域へダウンロードし、Androidのパッケージインストーラを開きます。初回のみ「この提供元のアプリを許可」が必要です。

自動チェックは起動時に前回から24時間以上経過している場合に行います。終了中のバックグラウンド定期実行ではありません。

旧 `PADAutoSolver-v0.2.0-debug.apk` はAndroid Debug署名で、固定Release署名とは異なります。旧debug版からの上書き更新はできません。固定Release版v0.2.0以降は同一署名を継続します。

テストとLint: `gradle :app:testDebugUnitTest :app:lintDebug`

上書き検証用の非公開ローカルAPKは、同じ署名環境変数を設定して `gradle :app:assembleRelease -PappVersionName=0.2.1 -PappVersionCode=3` で生成できます。この検証APKをv0.2.0 Releaseへ添付しないでください。

GitHubのリポジトリがprivateの場合、アクセストークンをアプリへ埋め込むのは危険なので、この自動取得は使わない設計です。

## 使い方

1. APKをインストールして起動します。
2. 「① オーバーレイ権限を開く」から、他のアプリの上に表示する権限を許可します。
3. Android 13以降で「制限付き設定」の警告が出る場合は、`設定 → アプリ → PAD Auto Solver → 右上の︙ → 制限付き設定を許可` を先に実行します。
4. 「② ユーザー補助サービスを開く」から `PAD Auto Solver` を有効にします。
5. 「③ 画面キャプチャを開始」を押します。
6. Androidの画面共有ダイアログでは、パズドラを読み取れるよう **画面全体** の共有を選びます。
7. パズドラに切り替え、パズル開始後に右上の「◎」をタップします。

## おすすめ初期設定

- 列数: 6
- 左右余白: 0 dp
- 下端余白: 0 dp
- 探索手数: 30
- 探索幅: 1200
- 自動スワイプ: 4000 ms

## 注意

ゲーム側の利用規約・不正対策に抵触する可能性があります。利用する場合は規約を確認してください。AccessibilityServiceをゲーム自動化目的でGoogle Playへ公開する用途は想定していません。
