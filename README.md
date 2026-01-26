# Future Log - Monthly Maintenance App

## 目的 / Purpose
「実績ログを起点に、定期的な生活メンテナンスを月次Future Logとして自然に可視化する」Androidアプリです。

## 思想 / Philosophy
このアプリは「バレットジャーナル」の **Future Log** を思想のベースとしていますが、以下の点を最重要視しています：

*   **日付や期限で管理しない**: 厳格なスケジュール管理は行いません。
*   **ToDo管理をしない**: タスクを完了させることへのプレッシャーを排除します。
*   **ユーザーを責めない**: 未実施の項目があっても、それは「失敗」ではありません。
*   **月単位で未来をゆるく見せる**: 日々の詳細よりも、長期的な見通しを重視します。
*   **管理されている感覚を与えない**: 自然と生活を振り返り、先を見通せる体験を提供します。

## 目指す体験 / What
*   **実績の可視化**: 過去の行動ログから、自然に未来の予定やメンテナンス項目が浮かび上がる体験。
*   **ゆるやかな見通し**: 「今月はこれをしようかな」「来月はこれがあるな」という、プレッシャーのない未来の把握。
*   **月次まとめ**: 月に一度、自分の生活をメンテナンスするような心地よい振り返りの時間。

## やらないこと / Non-Goals
*   **日付指定のスケジュール管理**: カレンダーアプリではありません。
*   **期限・アラート・ToDo化**: リマインダーや期限切れ通知でユーザーを急かしません。
*   **未実施＝失敗**: 未達成項目を赤字で表示したり、警告を出したりしません。
*   **詳細なタスク管理**: プロジェクト管理ツールや高度なToDoリストの機能は持ちません。

## 技術スタック / Tech Stack
*   **Platform**: Android
*   **Language**: Kotlin
*   **UI**: Jetpack Compose
*   **Architecture**: Logic-light, clean structure
*   **Persistence**: (Planned) Room / DataStore

## Build Instructions
1.  Open this project in **Android Studio**.
2.  Android Studio should automatically detect the project structure and setup the necessary Gradle wrappers.
3.  Sync implementation with Gradle.
4.  Run on Emulator or Physical Device.
