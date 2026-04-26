# Hikari-Tweaks

現在バージョン: **v1.0.0**

Minecraft 1.18.2 向けの **Fabric クライアントサイド Mod** です。  
Hikari 環境で使う便利機能をまとめています。

- スコアボードHUDのカスタム表示（HikariScoreBoard 連携）
- 耐久値 1% 警告
- ホットバー自動補充
- 不死のトーテム自動補充
- MiniHUD フリーカメラ時のビーコン範囲補正

---

## 1. 主な機能

### 1.1 MiniHUD 補正（Freecam Beacon Fix）

- MiniHUD のフリーカメラ中、ビーコン範囲表示の基準をカメラではなくプレイヤー位置に補正
- ON/OFF 切り替え可能（ホットキー対応）

### 1.2 耐久値 1% 警告

- 耐久アイテムが「残り1%以下」になったとき、チャット通知 + 効果音で警告
- 同一状態での連続通知を抑制
- ON/OFF 切り替え可能（ホットキー対応）

### 1.3 ホットバー自動補充

- コンテナを開いたとき、指定リストのアイテムをホットバーへ自動補充
- ON/OFF 切り替え可能（ホットキー対応）

補足:

- プレイヤーインベントリ画面では動作しません
- エンダーチェストを開いたときは動作しません
- 補充後は画面を自動で閉じます

### 1.4 不死のトーテム自動補充

- トーテム発動時に、発動前に持っていたスロットへトーテムを補充
- 通常のスロット切り替えでは補充が発動しないように調整済み
- ON/OFF 切り替え可能（ホットキー対応）

### 1.5 カスタムスコアボードHUD（HikariScoreBoard 連携）

サーバー側 `HikariScoreBoard` から受信したランキングを、クライアントHUDとして表示します。

- バニラスコアボードの非表示切替
- ページサイズ変更（1〜50）
- ページ送り / 戻し / リセット
- HUD位置（X/Y %）とスケール（0.5x〜3.0x）調整
- ヘッダー / 本文 / 文字色 / スコア色 / 自己強調色を ARGB で調整
- サーバー合計値（Total）表示切替
- プレイヤー管理タブ（表示ブロック切替）

---

## 2. 動作環境

- Minecraft `1.18.2`
- Fabric Loader `>= 0.14.0`
- Fabric API
- malilib

推奨（任意）:

- Mod Menu（設定画面を開きやすくする）
- MiniHUD（ビーコン補正機能を使う場合）
- HikariScoreBoard（カスタムスコアボード連携を使う場合）

---

## 3. 導入手順

1. `build/libs/hikari-tweaks-<version>.jar` をクライアント側 `mods/` に配置
2. 依存 Mod（Fabric API / malilib）も同様に配置
3. ゲーム起動
4. 初回起動後、`config/hikari-tweaks.json` が生成されます

---

## 4. 使い方

### 4.1 設定画面を開く

- デフォルトホットキー: `RIGHT_SHIFT`
- または Mod Menu 経由で `Hikari-Tweaks` の設定画面を開く

### 4.2 設定タブ

- `Tweaks`: 各機能の ON/OFF
- `Lists`: ホットバー自動補充対象アイテムIDリスト
- `Hotkeys`: 機能トグルや設定画面オープンのキー設定
- `Scoreboard`: スコアボード連携・表示設定・プレイヤー管理

---

## 5. 主要設定（デフォルト値）

| 設定キー | 既定値 | 説明 |
|---|---:|---|
| `fixBeaconRangeFreeCam` | `true` | MiniHUDのビーコン範囲補正 |
| `durabilityWarningEnabled` | `true` | 耐久1%警告 |
| `autoRestockHotbar` | `false` | ホットバー自動補充 |
| `totemRestock` | `false` | トーテム自動補充 |
| `hotbarRestockList` | `minecraft:firework_rocket`, `minecraft:golden_carrot` | 自動補充対象リスト |
| `openConfigHotkey` | `RIGHT_SHIFT` | 設定画面を開くキー |
| `scoreboardCustomHud` | `true` | カスタムHUD表示 |
| `scoreboardHideVanilla` | `true` | バニラ右側スコアボードを隠す |
| `scoreboardPageSize` | `10` | 1ページの表示件数（1〜50） |
| `scoreboardPositionX` | `100` | HUD基準X（0〜100%） |
| `scoreboardPositionY` | `50` | HUD基準Y（0〜100%） |
| `scoreboardScale` | `1.0` | HUDスケール（0.5〜3.0） |
| `scoreboardHeaderColor` | `0x66000000` | ヘッダー背景色（ARGB） |
| `scoreboardBodyColor` | `0x4D000000` | 本文背景色（ARGB） |
| `scoreboardTextColor` | `0xFFFFFFFF` | 文字色（ARGB） |
| `scoreboardScoreColor` | `0xFFFF5555` | スコア色（ARGB） |
| `scoreboardSelfColor` | `0xFFFFFF55` | 自己行強調色（ARGB） |
| `scoreboardShowServerTotal` | `true` | サーバー合計表示 |

---

## 6. ホットキー

- `Open Config`: 既定 `RIGHT_SHIFT`
- `fixBeaconRangeFreeCam`: 初期未割当（任意で設定）
- `durabilityWarningEnabled`: 初期未割当（任意で設定）
- `autoRestockHotbar`: 初期未割当（任意で設定）
- `totemRestock`: 初期未割当（任意で設定）

---

## 7. HikariScoreBoard 連携仕様

`Hikari-Tweaks` は以下チャネルで `HikariScoreBoard` と通信します。

受信:

- `hikariscoreboard:ranking_data`
- `hikariscoreboard:player_list_response`

送信:

- `hikariscoreboard:player_list_request`
- `hikariscoreboard:block_toggle`

---

## 8. 設定ファイル

配置先: `config/hikari-tweaks.json`

- JSON形式で保存
- 起動時に不足項目を補完
- 設定画面やホットキー変更時に自動保存

---

## 9. ビルド

```bash
./gradlew build
```

生成物:

- `build/libs/hikari-tweaks-<version>.jar`
- `build/libs/hikari-tweaks-<version>-sources.jar`

---

## 10. 既知の挙動・注意点

- 自動補充系はクライアント操作としてスロットクリックを行います
- ホットバー自動補充はコンテナを開いたタイミングで実行され、完了後に画面を閉じます
- F3デバッグ表示中はカスタムスコアボードHUDを描画しません

---

## 11. ライセンス

Apache-2.0

## 12. Update Checker (GitHub Releases)

Hikari-Tweaks には、GitHub Releases の新バージョンを確認してゲーム参加時に通知する機能があります。

設定ファイル: `config/hikari-tweaks.json`

主な項目:

- `updateCheckerEnabled`: 更新チェックを有効化
- `updateNotifyOnJoin`: 参加時に通知
- `updateCheckIntervalMinutes`: チェック間隔（分）
- `updateIncludePrerelease`: pre-release も対象にするか
- `updateGithubOwner`: GitHub owner（例: `Tamago0314`）
- `updateGithubRepo`: GitHub repository（例: `Hikari-Tweaks`）
- `updateReleaseUrlOverride`: 通知リンクを固定したい場合のURL（空ならGitHub Release URLを自動使用）

推奨:

- 通常運用: `updateIncludePrerelease = false`
- テスト配布も追いたい場合のみ `true`
