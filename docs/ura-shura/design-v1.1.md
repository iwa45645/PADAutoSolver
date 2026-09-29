# PADAutoSolver
# 裏魔門の守護者〈裏修羅〉 完全自動安定クリア
## 開発設計書 v1.1

---

## 0. 文書情報

- 対象リポジトリ: `iwa45645/PADAutoSolver`
- 現行ベース: `v0.2.0`
- 現行main確認コミット: `d4f0a455483348e2c765c40b08d1b70c89aa66c2`
- Android: minSdk 26 / targetSdk 35
- Java: 17
- applicationId: `com.example.padautosolver`
- 想定端末: Motorola edge 60 pro / Android 15
- 想定作業ブランチ: `feature/ura-shura-full-auto`
- 最終目標: **利用者の実際のモンスターボックスを一度検査し、そこから導出した固定編成を用いて、裏魔門の守護者〈裏修羅〉を開始からクリアまで安定して自動攻略する**
- 安全原則: 認識・状態・操作結果に不確実性がある場合は操作を続けず、再取得、確認待ち、または安全停止へ移行する

本書の「完全自動」は、初回セットアップと編成確認が終わった後、アプリが以下を人手なしで処理する状態を指す。

1. ダンジョン開始画面の確認
2. 現在シーン、階層、敵分岐、フェーズ、ギミックの判定
3. スキル使用判断
4. スキルアイコン操作
5. 盤面認識
6. 攻略目的に合うルート探索
7. ドラッグ操作
8. 操作結果の検証
9. 次階層への遷移確認
10. 特殊パズル処理
11. ボス撃破確認
12. クリア画面確認
13. 想定外状態での停止・診断保存

規約違反の検知回避、root化、ゲーム改変、通信改ざん、メモリ改変、パケット操作、アンチチート回避は実装しない。通常の画面取得とAccessibilityジェスチャーだけを利用する。利用規約上のリスクはREADMEに明示する。

---

# 1. 最重要方針: 編成を先に決め打ちしない

## 1.1 必須の初回工程

Codexは、過去の会話で挙がったノーチラス、シルヴィ、禰豆子などをそのまま最終編成として固定してはならない。

開発の最初に、以下の「ボックス検査ゲート」を実施する。

```text
ボックス全体の一次走査
→ 候補モンスターの抽出
→ 候補だけ詳細検査
→ 裏修羅要件との照合
→ 複数編成候補の生成
→ 実現可能性シミュレーション
→ 利用者へ根拠付き提示
→ 固定編成を1つ選択
→ その編成専用の認識・戦略を構築
```

実際のボックスデータを取得していない段階で、特定モンスターを「所持している」「最適」と仮定しない。

## 1.2 ボックス検査完了の定義

次を満たした場合のみ、ボックス検査完了とする。

- ボックスの先頭から末尾まで走査した
- 重複ページとスクロール重なりを除去した
- 各所持モンスターを一意なインスタンスまたは所持数として記録した
- 認識信頼度が低い候補を利用者が確認した
- 進化前後、変身前後、アシスト進化を区別した
- 裏修羅編成候補に使うモンスターについて詳細画面を確認した
- スキルレベル、覚醒、超覚醒、潜在、アシスト、レベル、限界突破など、攻略に必要な状態を確認した
- 助っ人として利用可能な候補も確認した
- 取得できなかった情報を`unknown`として明示した
- `box_inventory.json`を生成した

スクリーンショットが一部しかない場合は「ボックス全体を検査済み」と報告しない。

---

# 2. 完成条件

## 2.1 機能上の完成条件

最終版は次を満たす。

- 初回に「ボックス検査」機能を実行できる
- 所持モンスターから裏修羅向け編成候補を生成できる
- 編成候補ごとに不足要件、必要育成、アシスト、潜在、超覚醒を示せる
- 固定したチームの6枠とアシストを登録できる
- 「裏修羅自動攻略」を開始すると、開始画面からクリア画面まで状態機械が継続する
- 各スキルの使用可否を画面から判定できる
- 現在階層、敵分岐、HPフェーズ、特殊指示を判定できる
- 未対応または低信頼の状態を通常盤面として誤処理しない
- 階層プロファイルに従ってスキルとパズル目標を決定する
- スキルタップ後、発動結果を確認してから次へ進む
- パズル後、敵HP、階層、盤面、ゲーム状態を再確認する
- 誤った階層推定や操作失敗を検知した場合は無制限に続行しない
- クリア画面を明確に検出し、成功記録を残して停止する
- 他アプリ、システムダイアログ、画面ロック中は操作しない
- 常駐通知、アプリ画面、オーバーレイから緊急停止できる

## 2.2 「安定」の定義

同一端末・同一解像度・同一チーム・同一ゲーム設定で以下を満たす。

- 20回以上の連続実機試験
- クリア率95%以上
- 誤ったアプリへのジェスチャー0回
- 認識不能状態での強行操作0回
- スキル誤タップ0回
- 違う階層戦略の実行0回
- 特殊パズルの誤目標実行0回
- 無限ループ0回
- 自動停止後に診断情報が残る割合100%
- 画面キャプチャ停止後のジェスチャー0回
- 画面回転・解像度変更後のジェスチャー0回

20回のうち失敗が1回ある場合、その原因を再現・修正し、追加10回を通すまで安定版扱いにしない。

## 2.3 実装済みと実機確認済みを区別する

最終報告では必ず以下を分ける。

- コード実装済み
- JVM単体試験済み
- 保存画像リプレイ試験済み
- Androidビルド済み
- 実機起動済み
- 実機ボックス走査済み
- 編成候補生成済み
- 実機Dry Run済み
- 実機操作済み
- 実機で1回クリア済み
- 実機で連続クリア基準達成

コードが存在するだけで「完全自動化完了」と報告してはならない。

---

# 3. ボックス検査サブシステム

## 3.1 目的

一度だけモンスターボックスを走査し、現在の所持状況から攻略編成を決める。

ボックス検査は2段階に分ける。

### 第1段階: 全体走査

目的:

- 所持モンスターの概略一覧を作る
- 同一モンスターの複数所持を把握する
- 裏修羅で使える可能性がある候補を広く抽出する

取得候補:

- モンスターIDまたはカタログ上の候補
- 表示名
- 進化形態
- 主属性・副属性
- レア度
- お気に入り状態
- 所持数
- 画像認識confidence
- 元スクリーンショット位置

### 第2段階: 候補の詳細検査

裏修羅編成に入り得る候補のみ詳細画面を確認する。

取得対象:

- 正確な進化形態
- レベル
- ＋値
- スキルレベル、残りターン
- 覚醒の解放数
- 超覚醒
- 潜在覚醒
- アシスト
- 限界突破
- 変身前後
- スキル内容
- リーダースキル
- タイプ、属性
- 代用可能性

## 3.2 ボックス走査モード

新しいモードを追加する。

```java
public enum AppMode {
    NORMAL_SOLVER,
    BOX_SCAN,
    TEAM_ANALYSIS,
    URA_SHURA_DRY_RUN,
    URA_SHURA_FULL_AUTO
}
```

`BOX_SCAN`の流れ:

1. MediaProjectionを開始
2. 利用者へモンスターボックスを開くよう案内
3. 表示形式、並び順、フィルタ状態を画面で確認
4. オーバーレイを最小化または一時非表示
5. 現在ページを取得
6. モンスターカードまたはアイコンのグリッドを検出
7. 各アイテムを切り出す
8. ページfingerprintを作る
9. Accessibilityで一定量スクロール
10. 動きが止まるまで待つ
11. 次ページを取得
12. 重なった行を照合
13. 末尾を検出するまで繰り返す
14. 一覧を利用者へ表示
15. 認識不確実な項目だけ確認
16. `box_inventory.json`を保存

## 3.3 前提を固定しすぎない

ゲームUIの更新に備え、以下を設定可能にする。

- グリッド列数
- カード矩形
- スクロール開始・終了座標
- スクロール距離
- 安定待機時間
- 末尾判定回数
- アイコン領域
- 名前・番号表示領域
- オーバーレイ退避位置

初回にキャリブレーション画面を用意する。

## 3.4 末尾検出

次の複数条件で判定する。

- スクロール前後でコンテンツ領域の差分が閾値未満
- 最下行のfingerprintが連続一致
- 同じページhashが規定回数出現
- スクロールジェスチャー後も座標対応が変わらない
- 末尾UIテンプレートがある場合は一致

1条件だけで確定しない。

## 3.5 重複除去

ページ間で行が重なるため、単純連結しない。

- アイコンのperceptual hash
- 行内順序
- 近隣アイコン列
- 表示名または番号候補
- スクロール方向
- 連続ページの最大共通部分列

を用いて重なりを除去する。

同一モンスターを複数所持している場合があるため、「画像が同じ」という理由だけで全て一つにまとめない。ページ位置と出現順を保持し、同一行の重複か別個体かを区別する。

## 3.6 モンスター同定

`MonsterCatalogProvider`を定義する。

```java
public interface MonsterCatalogProvider {
    CatalogVersion getVersion();
    List<MonsterCandidate> identify(IconCrop crop, Optional<String> ocrText);
    Optional<MonsterMetadata> getById(long monsterId);
    List<MonsterMetadata> search(String query);
}
```

提供方法:

- ライセンスと入手元を確認したローカルカタログ
- 利用者がインポートしたJSONカタログ
- 開発時だけ使う手動対応表
- 不明項目を利用者が選ぶ確認UI

外部データを採用する前に、Codexは以下を記録する。

- データ提供元
- 採用バージョンまたは取得日
- ライセンスまたは利用条件
- 含まれる項目
- 欠落項目
- 更新方法

架空のAPIや、取得できないデータソースを実装済みとして扱わない。信頼できるカタログがない場合は、認識候補＋利用者確認で完成させる。

## 3.7 認識confidence

候補ごとに以下を持つ。

```java
public final class InventoryRecognition {
    public final List<MonsterCandidate> candidates;
    public final float confidence;
    public final String evidence;
    public final boolean userConfirmed;
}
```

ルール:

- confidenceが高くても候補が競合する場合は確認対象
- 編成候補に使うモンスターは必ず詳細検査
- 未確認候補をチームへ自動採用しない
- 不明を近い別モンスターへ勝手に確定しない

## 3.8 データ保存

アプリ専用領域へ以下を保存する。

```text
files/
└─ inventory/
   ├─ box_inventory.json
   ├─ catalog_version.json
   ├─ scan_sessions/
   │  └─ <session-id>/
   │     ├─ summary.json
   │     ├─ pages/
   │     ├─ crops/
   │     └─ uncertain/
   └─ exports/
```

画面全体画像を長期保存する必要はない。診断・再解析に必要な範囲だけ保存し、利用者が削除できるようにする。外部送信しない。

## 3.9 再走査

最終編成決定後は毎回走査しない。

再走査条件:

- 利用者が明示的に実行
- 編成候補の所持状態が変わった
- カタログ更新後に再解析したい
- 選択チームが組めない
- アプリがチーム画面とTeamProfileの不一致を検出

---

# 4. 裏修羅用チーム分析

## 4.1 入力

- `box_inventory.json`
- 候補の詳細情報
- 助っ人候補
- 裏修羅要件プロファイル
- ソルバーが安定して作れる盤面形
- 操作時間
- 利用可能な育成素材に関する利用者設定
- アシスト付け替え可否
- 潜在変更可否
- 超覚醒変更可否

## 4.2 DungeonRequirement

裏修羅の要件をデータとして持つ。

```java
public final class DungeonRequirement {
    public final String id;
    public final RequirementSeverity severity;
    public final RequirementType type;
    public final int floor;
    public final String phaseId;
    public final Map<String, Object> parameters;
}
```

例:

- 変身までの実質スキブ
- スキル遅延への余裕
- 覚醒無効回復
- 消せない回復
- ダメージ吸収無効
- 属性吸収無効
- ダメージ無効対策
- ロック解除
- 盤面生成
- 軽減維持
- 回復力
- 固定追撃
- 全体攻撃
- 指定コンボ
- 特殊パズル
- ボスのフェーズ別対策

実際の階層データは最新情報を確認し、出典と確認日をプロファイルに記録する。攻略記事だけでなく、実画面リプレイで照合する。

## 4.3 ハード制約

以下を満たさない編成は候補から除外する。

- リーダーと助っ人の発動条件がソルバーで安定して作れる
- 必要HP・軽減・回復条件
- 封印耐性
- バインド対策
- 必須スキル用途が揃う
- 必須スキルが必要階層までに使用可能
- 遅延を考慮しても使用可能
- 変身が必要時点までに可能
- アシスト込みでもスキル順序が破綻しない
- ダメージ上限または必要火力を満たせる見込み
- 必須覚醒・潜在の実装が可能
- 同じ個体を二重使用していない
- 未所持モンスターを含まない
- 未確認個体を含まない
- 助っ人を実際に用意できる

## 4.4 ソフト評価

有効候補を以下で評価する。

1. 操作の単純さ
2. 盤面認識の安定性
3. 必要ドロップ数
4. スキル回転の余裕
5. スキル遅延への余裕
6. 回復安定性
7. 欠損時の代替手段
8. ボス分岐への対応範囲
9. ルーレットやロックへの耐性
10. アシスト・潜在変更コスト
11. 自動化で必要な特殊分岐数
12. リトライ可能性
13. 実機リプレイでの成功率

「理論最大火力」より「自動化で再現しやすい」を重視する。

## 4.5 スキルスケジュールシミュレーション

`SkillScheduleSimulator`を実装する。

入力:

- 各スキルの初期ターン
- 変身前後
- ヘイスト
- 遅延
- チャージ
- 使用予定階層
- アシストの溜まり
- 階層ごとの想定ターン数
- スキル溜め可能性

出力:

- 各階層での使用可否
- 溜まり過ぎによるアシスト化
- 遅延後の不足
- 必要な遅延耐性
- 必要なスキブ
- スキル溜めターン
- 破綻箇所
- 余裕ターン

編成候補は、盤面・耐久・スキルの三つをまとめて評価する。

## 4.6 候補の提示

最低3候補まで提示する。

各候補に以下を含める。

- リーダー
- サブ4体
- 助っ人
- アシスト
- 潜在
- 超覚醒
- 必要育成
- 役割
- 各階層の主要スキル表
- 長所
- 不安点
- 自動化難度
- 認識難度
- 不足している情報
- 推定成功率ではなく、検証前であることの明示

数値の成功率は実測前に捏造しない。

## 4.7 編成確定ゲート

利用者が候補を確認し、1編成を選択した後に`team_profile.json`を作る。

以後の完全自動ロジックはその固定編成を対象とする。

チーム画面を実機で再確認し、登録内容と一致しない場合は自動攻略を開始しない。

---

# 5. 現行v0.2.0の扱い

## 5.1 維持する機能

- MediaProjectionによるフレーム取得
- 盤面位置設定
- 6×5 / 7×6設定
- Beam Searchの基本構造
- 連続ストロークによるドラッグ
- 固定署名Release
- アプリ内更新
- GitHub Actionsによるtest / lint / APK生成
- キャプチャ世代管理
- 画面サイズ変更時の停止

## 5.2 回帰条件

通常モードはv0.2.0互換を維持する。

- フローティングボタンを押す
- 盤面を1回取得
- 最大コンボルートを探索
- 連続ドラッグを実行

裏修羅機能の追加で、通常モードの設定、ドラッグ、更新機能を壊さない。

---

# 6. 全体アーキテクチャ

```text
┌──────────────────────────────────────────────────────────┐
│ Setup                                                     │
│ BoxScanner → InventoryAnalyzer → TeamOptimizer            │
│            → TeamProfile → TeamCalibration                │
└────────────────────────────┬─────────────────────────────┘
                             │ fixed verified team
┌────────────────────────────▼─────────────────────────────┐
│ Android UI / Overlay                                      │
│ MainActivity / StatusOverlay / Calibration / Stop         │
└────────────────────────────┬─────────────────────────────┘
                             │ commands / status
┌────────────────────────────▼─────────────────────────────┐
│ AutomationOrchestrator                                    │
│ single source of truth / state machine / retries          │
└───────┬────────────┬────────────┬────────────┬────────────┘
        │            │            │            │
┌───────▼──────┐ ┌───▼────────┐ ┌▼──────────┐ ┌▼───────────┐
│CaptureEngine │ │SceneEngine │ │Strategy    │ │Action      │
│MediaProjection│ │classifier │ │Planner     │ │Executor    │
└───────┬──────┘ └───┬────────┘ └┬──────────┘ └┬───────────┘
        │             │           │             │
┌───────▼─────────────▼───────────▼─────────────▼───────────┐
│ Recognition Layer                                         │
│ board / floor / enemy / skill / dialog / result           │
└───────┬───────────────────────────────────────────────────┘
        │
┌───────▼───────────────────────────────────────────────────┐
│ Verification & Recovery                                   │
│ postconditions / stale-plan guard / retry / fail-closed   │
└───────┬───────────────────────────────────────────────────┘
        │
┌───────▼───────────────────────────────────────────────────┐
│ Diagnostics & Replay                                      │
│ event log / cropped frames / metrics / offline replay     │
└───────────────────────────────────────────────────────────┘
```

すべての操作は以下を守る。

```text
Observe
→ Classify
→ Plan
→ Validate against current observation
→ Execute exactly one atomic action
→ Wait
→ Observe again
→ Verify postcondition
→ Continue or stop
```

複数スキルをまとめてタップしてから確認しない。

---

# 7. 推奨パッケージ構成

```text
com.example.padautosolver
├─ MainActivity.java
├─ AutoPuzzleService.java
├─ PuzzleAccessibilityService.java
├─ UpdateManager.java
│
├─ inventory/
│  ├─ BoxScanController.java
│  ├─ BoxGridDetector.java
│  ├─ BoxScrollController.java
│  ├─ BoxPage.java
│  ├─ InventoryItem.java
│  ├─ InventoryRepository.java
│  ├─ InventoryDeduplicator.java
│  ├─ MonsterCatalogProvider.java
│  ├─ MonsterMetadata.java
│  ├─ MonsterCandidate.java
│  └─ CandidateDetailInspector.java
│
├─ teambuilder/
│  ├─ TeamOptimizer.java
│  ├─ TeamCandidate.java
│  ├─ DungeonRequirement.java
│  ├─ RequirementEvaluator.java
│  ├─ SkillScheduleSimulator.java
│  ├─ TeamProfile.java
│  └─ TeamProfileRepository.java
│
├─ automation/
│  ├─ AutomationOrchestrator.java
│  ├─ AutomationState.java
│  ├─ AutomationEvent.java
│  ├─ AutomationSession.java
│  ├─ RetryPolicy.java
│  └─ StopReason.java
│
├─ capture/
│  ├─ CaptureEngine.java
│  ├─ CapturedFrame.java
│  ├─ FrameLease.java
│  └─ DisplaySnapshot.java
│
├─ scene/
│  ├─ SceneClassifier.java
│  ├─ SceneType.java
│  ├─ SceneObservation.java
│  └─ SceneStabilityFilter.java
│
├─ board/
│  ├─ BoardGeometry.java
│  ├─ BoardRecognizer.java
│  ├─ OrbClassifier.java
│  ├─ OrbType.java
│  ├─ OrbState.java
│  ├─ CellRecognition.java
│  └─ BoardObservation.java
│
├─ dungeon/
│  ├─ DungeonProfile.java
│  ├─ FloorProfile.java
│  ├─ EnemyVariant.java
│  ├─ PhaseProfile.java
│  ├─ DungeonStateEstimator.java
│  └─ DungeonProfileRepository.java
│
├─ team/
│  ├─ SkillBarRecognizer.java
│  ├─ SkillReadiness.java
│  ├─ TeamCalibrator.java
│  └─ TeamVerifier.java
│
├─ strategy/
│  ├─ StrategyPlanner.java
│  ├─ StrategyRule.java
│  ├─ PlannedAction.java
│  ├─ Postcondition.java
│  └─ StrategyTrace.java
│
├─ solver/
│  ├─ MoveSearchEngine.java
│  ├─ MatchSimulator.java
│  ├─ PuzzleGoal.java
│  ├─ GoalEvaluator.java
│  ├─ GoalScore.java
│  ├─ SolverBudget.java
│  ├─ SolverPlan.java
│  └─ evaluators/
│
├─ action/
│  ├─ ActionExecutor.java
│  ├─ TapAction.java
│  ├─ DragAction.java
│  ├─ GestureResult.java
│  └─ GestureWatchdog.java
│
├─ verify/
│  ├─ PostconditionVerifier.java
│  ├─ FloorTransitionVerifier.java
│  ├─ SkillActivationVerifier.java
│  ├─ PuzzleResultVerifier.java
│  └─ ClearResultVerifier.java
│
├─ safety/
│  ├─ ForegroundAppGuard.java
│  ├─ StalePlanGuard.java
│  ├─ ConfidenceGuard.java
│  ├─ ScreenGuard.java
│  └─ EmergencyStopController.java
│
├─ overlay/
│  ├─ OverlayController.java
│  ├─ StatusOverlayView.java
│  ├─ BoardPreviewView.java
│  └─ CalibrationOverlayView.java
│
├─ diagnostics/
│  ├─ DiagnosticLogger.java
│  ├─ SessionSummary.java
│  ├─ FrameRecorder.java
│  └─ MetricsCollector.java
│
└─ replay/
   ├─ ReplayRunner.java
   ├─ ReplayScenario.java
   └─ GoldenExpectation.java
```

---

# 8. 完全自動攻略の状態機械

## 8.1 AutomationState

```java
public enum AutomationState {
    IDLE,
    PRECHECK,
    VERIFYING_TEAM,
    WAITING_FOR_TARGET_APP,
    WAITING_FOR_DUNGEON_START,
    CAPTURING,
    CLASSIFYING_SCENE,
    STABILIZING_SCENE,
    ESTIMATING_DUNGEON_STATE,
    PLANNING,
    VALIDATING_PLAN,
    EXECUTING_TAP,
    EXECUTING_DRAG,
    WAITING_FOR_ANIMATION,
    VERIFYING_POSTCONDITION,
    RETRYING_OBSERVATION,
    PAUSED_NEEDS_REVIEW,
    SUCCESS,
    STOPPING,
    STOPPED,
    ERROR
}
```

## 8.2 SceneType

```java
public enum SceneType {
    UNKNOWN,
    HOME,
    MONSTER_BOX,
    MONSTER_DETAIL,
    DUNGEON_ENTRY,
    TEAM_CONFIRMATION,
    LOADING,
    BATTLE,
    SKILL_TARGET_SELECTION,
    SPECIAL_PUZZLE_INSTRUCTION,
    RESULT_CLEAR,
    RESULT_GAME_OVER,
    CONNECTION_DIALOG,
    SYSTEM_DIALOG,
    APP_BACKGROUND
}
```

## 8.3 安定化

1フレームだけで重大な状態を確定しない。

- BATTLEは近接した複数フレームで一致
- 階層遷移は番号、敵領域、盤面領域の複数証拠
- クリア判定はテンプレートと画面レイアウトの両方
- スキル発動判定はスキルアイコン、盤面、演出の複数証拠
- ボックス末尾も複数証拠で判定

---

# 9. 盤面認識

## 9.1 OrbType

```java
public enum OrbType {
    FIRE,
    WATER,
    WOOD,
    LIGHT,
    DARK,
    HEART,
    POISON,
    DEADLY_POISON,
    JAMMER,
    BOMB,
    UNKNOWN
}
```

別フィールドとして以下を持つ。

- locked
- enhanced
- blinded
- superBlinded
- roulette
- confidence

## 9.2 UNKNOWN

入力を必ず通常6色のどれかへ分類してはならない。

以下はUNKNOWNまたは実行禁止。

- 低彩度
- 暗すぎる
- 白飛び
- 色相分散が大きい
- 候補差が小さい
- 未対応テクスチャ
- 超暗闇
- 解析不能なルーレット
- 盤面矩形不正
- 画面サイズ変化
- 特殊ドロップの誤認識疑い

## 9.3 実行可能条件

初期値:

- UNKNOWN数 = 0
- 最低confidence >= 0.68
- 平均confidence >= 0.78
- 盤面サイズ一致
- 前面パッケージ一致
- 計画作成後の画面世代一致

閾値は設定可能だが、安全側へclampする。

---

# 10. パズル目標

最低限以下を実装する。

```java
public enum PuzzleGoalType {
    MAX_COMBO,
    WOOD_ROW,
    WOOD_VDP,
    WOOD_MASS_ATTACK,
    CLEAR_POISON,
    ZERO_COMBO,
    CLEAR_REQUIRED_COLORS,
    HEART_PRIORITY,
    STALL
}
```

目標ごとにハード制約を先に判定し、有効解同士を辞書順スコアで比較する。

- `WOOD_ROW`: 木6個以上の横連結
- `WOOD_VDP`: 木3×3
- `WOOD_MASS_ATTACK`: 木5個以上連結
- `CLEAR_POISON`: 初期盤面の毒・猛毒をすべて消す
- `ZERO_COMBO`: 操作後総コンボ0
- `CLEAR_REQUIRED_COLORS`: 指定色を盤面からすべて消す
- `HEART_PRIORITY`: 回復消しを必須または優先
- `STALL`: 敵を倒さず必要条件だけ満たす

タイムアウト時に自動で制約を緩めない。

---

# 11. 階層・敵・フェーズ推定

`DungeonStateEstimator`は以下を統合する。

- 前回の確定階層
- BATTLE番号表示
- 敵テンプレート
- 敵数
- HPバー数
- 属性
- 特殊指示
- 直前行動
- 階層遷移演出
- 盤面の有無
- プロファイル上の遷移可能性

推定結果:

```java
public final class DungeonEstimate {
    public final int floor;
    public final String enemyVariantId;
    public final String phaseId;
    public final float confidence;
    public final List<FloorEvidence> evidence;
    public final boolean transitionConsistent;
}
```

初期実行条件:

- confidence >= 0.85
- 遷移整合
- 重大な競合候補なし

矛盾時は再取得し、それでも解消しなければ停止する。

---

# 12. スキル認識と操作

## 12.1 TeamProfile

ボックス分析で確定した実チームを登録する。

- 6枠
- アシスト
- 変身状態
- スキル用途
- ターン
- 座標
- アイコンテンプレート
- 使用前後の期待変化
- 代替スキル
- 使用禁止条件

## 12.2 原子的操作

```text
スキル1個を計画
→ 現画面を再確認
→ 1回タップ
→ 演出待ち
→ 新フレーム取得
→ 発動後条件を検証
→ 次の行動
```

同時に複数スキルを押さない。

## 12.3 誤タップ防止

- スキルバーの位置を毎回検査
- 座標だけでなくアイコン領域も一致
- スキル選択ダイアログを検出
- タップ後に期待変化がない場合、再タップしない
- 1回だけ再取得
- 不一致なら停止

---

# 13. StrategyPlanner

戦略はJavaへ階層ごとに直書きせず、JSONプロファイルから読み込む。

各ルール:

```text
条件:
- floor
- enemyVariant
- phase
- skill readiness
- board contents
- hp state if observable
- previous action
- retry count

行動:
- use skill slot
- wait
- solve puzzle goal
- re-observe
- stop

事後条件:
- skill changed
- board changed
- enemy phase changed
- next floor
- clear result
```

プロファイルの誤りで強行操作しない。

---

# 14. 操作結果検証

全アクションにpostconditionを付ける。

## 14.1 スキル

- アイコン状態変化
- 盤面変化
- 演出変化
- 指示ダイアログ出現
- スキル残ターン変化

## 14.2 パズル

- 盤面移動開始
- 敵HPまたはフェーズ変化
- 階層遷移
- ゲームオーバー
- 盤面再出現
- クリア画面

## 14.3 タイムアウト

callbackが来ない場合も`duration + 2000ms`のwatchdogで復帰する。

同じ操作を無条件再実行しない。

---

# 15. 安全停止

停止経路:

- アプリの停止ボタン
- 通知の停止
- オーバーレイの停止
- `ACTION_STOP`
- 前面アプリ不一致
- キャプチャ停止
- 解像度変更
- UNKNOWN連続
- 階層矛盾
- スキル発動不成立
- 予期しないダイアログ
- ゲームオーバー
- watchdog
- 利用者による音量キー等の緊急停止は任意追加

停止時に以下を解放する。

- MediaProjection
- VirtualDisplay
- ImageReader
- listener
- overlay
- solver task
- watchdog
- pending action
- pending plan

---

# 16. 診断とリプレイ

## 16.1 必須ログ

- sessionId
- state遷移
- frameId
- captureGeneration
- scene候補とconfidence
- 階層推定と証拠
- スキル状態
- 盤面認識
- 目標
- 探索結果
- 実行座標
- callback結果
- postcondition
- retry
- stopReason
- 処理時間

## 16.2 保存画像

常時フル画面を保存しない。

以下の失敗時だけ、必要ROIを保存できるようにする。

- scene UNKNOWN
- 階層競合
- 盤面UNKNOWN
- スキル認識失敗
- postcondition不成立
- ゲームオーバー
- クリア

## 16.3 ReplayRunner

保存画像列からAndroid端末なしで以下を再生する。

- scene分類
- 階層推定
- 盤面認識
- 戦略決定
- 期待行動
- postcondition

リプレイでは実ジェスチャーを送らない。

---

# 17. テスト

## 17.1 JVM単体試験

- 盤面マッチ
- 重力
- 連鎖
- 各PuzzleGoal
- 0コンボ
- 毒全消し
- 指定色全消し
- スキルスケジュール
- TeamOptimizerのハード制約
- 同一個体の二重使用防止
- ボックスページ重複除去
- 末尾判定
- 状態遷移
- retry上限
- stale plan拒否

## 17.2 保存画像試験

- ボックス先頭、中間、末尾
- 重複行
- 同一モンスター複数所持
- 認識困難な進化形態
- 通常盤面
- 毒
- 暗闇
- 特殊指示
- 各敵分岐
- スキル使用前後
- 階層遷移
- クリア
- ゲームオーバー
- 通信ダイアログ

## 17.3 実機試験

段階:

1. ボックスDry Run
2. ボックス自動スクロール
3. 在庫一覧の人手照合
4. 候補詳細検査
5. 編成候補生成
6. チーム画面照合
7. ダンジョンDry Run
8. 1操作ずつ確認
9. 1階層自動
10. 複数階層
11. 1回完走
12. 連続20回

---

# 18. バージョン別マイルストーン

## v0.3.0: 基盤・安全化

- Gradle Wrapper
- サービス停止
- watchdog
- ForegroundAppGuard
- 診断ログ
- 状態機械雛形
- 既存通常モード回帰

## v0.4.0: ボックス検査

- BOX_SCAN
- ページ取得
- 自動スクロール
- 重複除去
- カタログProvider
- 不明候補確認
- `box_inventory.json`
- 候補詳細検査

完成条件: 実際の利用者ボックスを末尾まで一度走査し、人手照合結果を記録する。

## v0.5.0: 編成導出

- 裏修羅要件プロファイル
- TeamOptimizer
- SkillScheduleSimulator
- 候補最大3件
- 育成・アシスト・潜在提案
- TeamProfile確定
- チーム画面検証

完成条件: 所持している個体だけで、検証可能な固定編成を1つ確定する。

## v0.6.0: 盤面高度化・半自動

- 特殊ドロップ
- UNKNOWN
- 複数PuzzleGoal
- プレビュー
- 手動修正
- Dry Run
- リプレイ

## v0.7.0: シーン・階層認識

- SceneClassifier
- DungeonStateEstimator
- 敵分岐
- 特殊指示
- クリア・ゲームオーバー
- スキルバー認識

## v0.8.0: スキル自動操作

- TeamProfile座標
- 1操作ごとの検証
- スキルschedule
- 失敗停止
- 階層単位自動化

## v0.9.0: 裏修羅完走候補

- 全階層戦略
- ボス分岐
- 異常復帰
- 1回完走
- 失敗データ修正

## v1.0.0: 安定版

- 20回以上
- 95%以上
- 誤操作0
- 診断100%
- v0.2.0から固定署名で更新可能
- READMEと引き継ぎ文書完成

---

# 19. GitHub運用

- `main`へ直接実験コードを入れない
- 作業ブランチを使う
- v0.2.0タグを移動しない
- 公開済みRelease assetを上書きしない
- versionCodeは必ず増加
- 固定署名を維持
- 署名鍵を出力・コミットしない
- 機能単位でコミット
- CIでtest / lint / debug APK
- Release前に固定証明書を検査
- 一回限りの`finalize-initial-release.yml`は削除候補
- ブランチ保護が未設定なら設定を推奨するが、Codexは勝手に管理設定を変更しない

---

# 20. Codexの実装順

1. リポジトリと現行テストを確認
2. 作業ブランチを作成
3. 既存通常モードの回帰基準を固定
4. 基盤安全化
5. ボックス走査機能
6. 実際のボックスデータ取得
7. 在庫の人手照合
8. 候補詳細検査
9. 編成最適化
10. 固定TeamProfile確定
11. 盤面高度化
12. シーン・階層認識
13. スキル操作
14. 階層ごとの自動化
15. 完走
16. 連続安定試験
17. Release

実ボックスを確認する前に、裏修羅用固定チームをコードへ埋め込まない。

---

# 21. Codexの報告形式

各作業完了時に短く以下を報告する。

```text
変更ファイル:
実装内容:
実行したテスト:
実機で確認した内容:
実機未確認:
取得したボックス範囲:
確認済みモンスター数:
不明モンスター数:
編成候補:
現在の停止要因:
次に必要な入力:
```

「認識候補が出た」と「所持確認済み」を区別する。

---

# 22. 最終受け入れチェックリスト

## ボックス・編成

- [ ] 先頭から末尾まで一度走査
- [ ] 重複除去
- [ ] 不明候補の確認
- [ ] 候補の詳細画面確認
- [ ] 所持個体だけで編成
- [ ] 助っ人確保
- [ ] スキルschedule成立
- [ ] 育成状態確認
- [ ] TeamProfileとゲーム画面一致

## 自動攻略

- [ ] 対象アプリ二重確認
- [ ] 画面世代確認
- [ ] 階層confidence
- [ ] スキル原子的実行
- [ ] postcondition
- [ ] 盤面UNKNOWNなし
- [ ] 経路制約成立
- [ ] watchdog
- [ ] 特殊パズル
- [ ] ボス分岐
- [ ] クリア検出
- [ ] 緊急停止
- [ ] 診断出力

## 安定性

- [ ] 20回以上
- [ ] 95%以上
- [ ] 誤アプリ操作0
- [ ] 誤スキル0
- [ ] 無限ループ0
- [ ] 不確実状態の強行0
- [ ] 失敗時診断100%

---

# 23. 重要な禁止事項

- 実際に見ていないモンスターを所持扱いしない
- 不明なアイコンを類似キャラへ勝手に確定しない
- 編成候補生成前に固定チームを決め打ちしない
- 画像数枚だけでボックス全体を確認済みとしない
- 実機未検証を実機確認済みと書かない
- テンプレートを置いただけで認識完成としない
- Adapterだけ作ってカタログ統合完了としない
- 1回クリアで安定版としない
- 失敗時に無制限リトライしない
- 対象アプリ不明時にジェスチャーを送らない
- 利用規約違反の検知回避を実装しない

---

以上。
