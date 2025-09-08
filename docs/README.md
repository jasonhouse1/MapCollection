```markdown
# Map Collection App

[![HackMD同步狀態](https://img.shields.io/badge/HackMD-自動同步-blue)](https://hackmd.io/SkYMpwQvex)

這是一個專為地圖愛好者設計的 Android 應用程式，讓使用者可以輕鬆建立、收藏與分享個人化的地圖與行程。本文檔透過 GitHub Actions 自動同步至 HackMD。

## ✨ 功能簡介

- **🗺️ 個人地圖建立與儲存**：輕鬆建立並管理屬於自己的地圖收藏。
- **📍 景點卡片化呈現**：以直觀的卡片形式管理與瀏覽各個景點資訊。
- **👥 社群推薦與標籤分類**：透過標籤系統發現社群推薦的熱門地圖與路徑。
- **🧭 Google Map 整合與路線規劃**：深度整合 Google Maps API，提供流暢的路線規劃功能。

## 🚀 詳細功能

- **🔍 地圖搜尋**：使用者可透過標籤或名稱搜尋已建立的地圖。
- **🛣️ 自訂路徑規劃**：可在地圖中新增景點與行程，並以路徑卡片方式保存。
- **❤️ 推薦與收藏**：提供推薦頁面，快速找到熱門或適合的路徑與地圖。
- **👤 個人檔案編輯**：使用者可修改暱稱、標籤與大頭貼，建立專屬的地圖身份。
- **📝 貼文分享**：使用者可將地圖、路徑以卡片方式呈現，支援刪除與瀏覽。

---

## 📱 畫面預覽

| 主畫面 | 搜尋 | 路徑規劃 | 個人檔案 |
|:---:|:---:|:---:|:---:|
| (待補充) | (待補充) | (待補充) | (待補充) |

> *請將應用截圖放置於 `docs/screenshots/` 目錄並更新連結*

---

## 📁 專案結構


MapCollection/
├── app/
│   └── src/main/
│       ├── java/com/example/mapcollection/
│       │   ├── MainActivity.kt              # 應用主入口
│       │   ├── EditProfileActivity.kt       # 個人檔案編輯
│       │   ├── MapsActivity.kt              # 地圖新增與管理
│       │   ├── MapsActivity2.kt             # 行程規劃
│       │   ├── NewPointActivity.kt          # 新增景點
│       │   ├── InformationActivity.kt       # 地點資訊顯示
│       │   ├── SearchActivity.kt            # 搜尋地圖
│       │   ├── RecommendActivity.kt         # 推薦清單
│       │   ├── PathActivity.kt              # 路徑清單
│       │   ├── adapter/
│       │   │   ├── PostAdapter.kt           # 地圖貼文 RecyclerView adapter
│       │   │   └── PathPostAdapter.kt       # 路徑卡片 RecyclerView adapter
│       │   └── model/
│       │       ├── Post.kt                  # 地圖貼文資料模型
│       │       └── PathPost.kt              # 路徑貼文資料模型
│       ├── res/
│       │   ├── layout/                      # XML UI 版面佈局
│       │   └── drawable/                    # 圖片與圖形資源
│       └── AndroidManifest.xml              # Android 應用清單
└── README.md                                # 專案說明文件
```

---

## 🛠️ 技術堆疊

- **開發語言**：Kotlin
- **架構**：MVC (Model-View-Controller)
- **地圖服務**：Google Maps SDK for Android
- **UI 框架**：Android Jetpack (AndroidX)
- **版面佈局**：ConstraintLayout, RecyclerView
- **設計風格**：Material Design

---

## 🔮 未來規劃

- [ ] **使用者系統**：實現帳號註冊、登入與雲端同步功能。
- [ ] **資料持久化**：整合雲端資料庫（如 Firebase Firestore）儲存地圖資料。
- [ ] **社群功能**：加入好友系統、地圖協作編輯與即時互動。
- [ ] **效能優化**：提升地圖渲染與大量標記處理的流暢度。

---

## 📄 授權

本專案僅供學術與個人研究使用。如需用於商業用途，請事先與作者聯繫。

---

*本文檔最後同步時間：2025年9月8日*
```
