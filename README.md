# Digital Wellbeing

Ứng dụng Android phân tích thói quen sử dụng điện thoại theo hướng local-first, kết hợp các insight hành vi và lớp giải thích AI để giúp người dùng hiểu rõ hơn về screen time của mình.

## Mục tiêu

Digital Wellbeing không chỉ hiển thị tổng thời lượng sử dụng. Ứng dụng tái cấu trúc dữ liệu UsageStats thành session, xu hướng, chuyển đổi giữa app và insight hành vi để người dùng nhìn thấy các pattern như dùng máy khuya, chuyển app liên tục hoặc phụ thuộc vào một nhóm ứng dụng.

Ứng dụng ưu tiên kiến trúc local-first: dữ liệu usage được xử lý và lưu cục bộ bằng Room, vẫn hoạt động khi offline. AI cloud enhancement là tùy chọn và có fallback về insight local.

## Tính năng chính

- Dashboard tổng quan screen time, top apps, biểu đồ theo giờ và insight nổi bật.
- Daily Overview cho dữ liệu theo ngày, so sánh với ngày trước đó.
- Weekly Overview cho tổng quan tuần, biểu đồ 7 ngày và top apps.
- Session Timeline tái tạo các phiên sử dụng theo dòng thời gian.
- App Categories phân nhóm ứng dụng và cho phép chỉnh category thủ công.
- App Transition Graph phân tích chuyển đổi nhanh giữa các ứng dụng.
- Usage Pattern phân tích độ dài session, số lần mở app và switch count.
- Late Night Analysis tập trung vào hành vi sử dụng từ tối muộn đến rạng sáng.
- Preferences cho ngôn ngữ, dark mode, threshold phân tích, notification và cloud enhancement.
- Privacy & Data cho export, reset phân tích, xóa dữ liệu và cloud backup toggle.

## Kiến trúc

Dự án dùng Clean Architecture + MVVM:

```text
Presentation Layer
Activity / Fragment / Adapter / ViewModel / UiState
        ↓
Domain Layer
UseCase / Service / Model / Repository Interface
        ↓
Data Layer
Repository Impl / DataSource / Room / DataStore / Retrofit / Worker
```

Pipeline dữ liệu usage:

```text
Android UsageStats
        ↓
UsageStatsDataSource
        ↓
RefreshUsageDataUseCase
        ↓
SessionBuilder / SessionEnricher
        ↓
UsageAggregator / UsageFeatureExtractor
        ↓
InsightEngine
        ↓
Room Database
        ↓
Presentation ViewModels
```

## Công nghệ

- Kotlin
- XML Views
- Coroutines & Flow
- Dagger Hilt
- Room
- DataStore Preferences
- WorkManager
- Retrofit + OkHttp
- Gemini API cho cloud insight tùy chọn
- MPAndroidChart

## Các module chính

- `presentation`: UI, ViewModel, adapter, mapper và state rendering.
- `domain`: business logic, use case, model, service và repository contract.
- `data`: Room, DataStore, Android UsageStats, Retrofit, WorkManager và notification.
- `di`: Hilt modules cho repository, service, database và network.

## Background Work

Ứng dụng lên lịch `UsageSyncWorker` mỗi 4 giờ để đồng bộ dữ liệu usage vào Room. Notification worker được bật/tắt theo preferences:

- `InsightNotificationWorker`: insight định kỳ.
- `WeeklyReportNotificationWorker`: báo cáo tuần.

## AI-Assisted Insights

Insight có hai tầng:

- Local insight: sinh từ rule/domain service, hoạt động offline.
- Cloud enhancement: nếu người dùng bật tùy chọn, có mạng và có Gemini API key, app gửi grounded context đã được rút gọn sang Gemini để tạo lời giải thích tự nhiên hơn.

Nếu cloud không khả dụng, app tự fallback về `LocalInsightNarrator`.

## Chạy dự án

1. Mở project bằng Android Studio.
2. Đảm bảo Android Gradle Plugin và Gradle wrapper được sync thành công.
3. Cấu hình Gemini API key nếu muốn dùng cloud insight.
4. Build hoặc chạy:

```bash
./gradlew assembleDebug
```

Trên Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Ghi chú

Project phục vụ mục đích học tập, nghiên cứu và portfolio Android. Điểm trọng tâm của dự án là pipeline phân tích hành vi local-first, không chỉ là giao diện thống kê screen time đơn giản.
