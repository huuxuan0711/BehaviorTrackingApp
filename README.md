# Project2DigitalWellbeing

Dự án Android: **Digital Wellbeing (Sức khỏe Kỹ thuật số) kết hợp AI** 

Đây là một ứng dụng Android giúp người dùng theo dõi và quản lý thời gian sử dụng điện thoại thông minh, đồng thời cung cấp những phân tích sâu sắc về thói quen sử dụng thông qua trí tuệ nhân tạo (Gemini AI), từ đó đưa ra lời khuyên để cải thiện sức khoẻ kỹ thuật số (Digital Wellbeing).

## 🌟 Tính năng nổi bật

- **Quản lý & Theo dõi sử dụng (Usage Tracking):** Dễ dàng theo dõi thời lượng sử dụng chi tiết theo từng ứng dụng (daily, weekly, sessions).
- **Phân tích hành vi (Analysis & Insights):** Phân tích các mô hình/chu kỳ sử dụng điện thoại như sử dụng đêm khuya (Late Night), thói quen chuyển đổi ứng dụng (Transitions) hay phân tích theo danh mục ứng dụng (App Categories).
- **Phân tích bằng AI (AI Reasoning):** Tích hợp AI (Gemini 2.5 Flash) để đưa ra các đánh giá, cảnh báo và lời khuyên mang tính cá nhân hóa dựa trên dữ liệu sử dụng thông qua kiến trúc suy luận thông minh.
- **Biểu đồ trực quan:** Các xu hướng và hạn mức sử dụng hằng ngày/tuần được trực quan hóa rõ ràng bằng thống kê và biểu đồ (MPAndroidChart).
- **Tuỳ chỉnh & Nhắc nhở:** Hỗ trợ cảnh báo sử dụng vượt định mức, giúp người dùng tập trung hơn với các cài đặt linh hoạt (Permissions, WorkManager cho background tasks).

## 🛠️ Kiến trúc và Công nghệ (Tech Stack)

Dự án sử dụng Kotlin và được xây dựng dựa trên **Clean Architecture** (với các tầng Data, Domain, Presentation), đồng thời tuân thủ các chuẩn mực thiết kế hiện đại của Android:

*   **Ngôn ngữ:** Kotlin 2.0.21
*   **Trình quản lý giao diện:** XML & ViewBinding
*   **Kiến trúc:** Clean Architecture (Data - Domain - Presentation) + MVVM
*   **Dependency Injection:** Dagger Hilt
*   **Xử lý bất đồng bộ:** Kotlin Coroutines & Flow
*   **Mạng & API:** Retrofit2, OkHttp3, Gson (Dùng để giao tiếp với mạng và Gemini API)
*   **Cơ sở dữ liệu:** Room Database (Lưu trữ cục bộ), DataStore Preferences (Cài đặt)
*   **WorkManager:** Xử lý các tác vụ nền đáng tin cậy.
*   **Thư viện UI/UX:** 
    *   MPAndroidChart (Vẽ biểu đồ)
    *   Glide (Tải ảnh nghệ thuật/App Icon)
    *   ViewPager2 + DotsIndicator (Onboarding, chuyển đổi màn hình)
    *   SDP/SSP (Hỗ trợ co giãn UI linh hoạt trên nhiều kích thước màn hình)

## 📁 Cấu trúc thư mục (Packages)

- `data`: Xử lý dữ liệu cục bộ (Room), remote (AI API) và thao tác preferences, backup, tracking, notifications.
- `domain`: Chứa Business Logic (các mô hình, UseCases cho AI, Usage, Preferences, Notification, Reasoning).
- `presentation`:
  - `onboarding`: Splash screen, Xin quyền (Permissions) và Intro.
  - `dashboard`: Các màn hình thống kê sử dụng hằng ngày, hàng tuần, session.
  - `analysis`: Phân tích chuyên sâu (sử dụng đêm khuya, thói quen, danh mục ứng dụng).
  - `settings`: Quản lý cài đặt, chính sách bảo mật, hỗ trợ người dùng.
- `di`: Cấu trúc Dagger Hilt (cung cấp Dependency Injection cho toàn app).

## 🚀 Cài đặt và Chạy thử nghiệm

Để build và chạy dự án này trên môi trường phát triển của bạn:

1. **Clone dự án:**
   ```bash
   git clone <repository_url>
   ```
2. **Mở dự án:**
   Mở dự án thông qua Android Studio (Bản mới nhất hỗ trợ Android Gradle Plugin 8.12.3).
3. **Cấu hình Gemini AI (Mặc định đã có cấu hình mẫu):**
   Trong file `app/build.gradle.kts`, `GEMINI_API_KEY` đã được thiết lập để kết nối với Google Gemini.
   Bạn cũng có thể thay thế bằng API Key của bạn nếu cần thiết.
4. **Build & Run:**
   - Đảm bảo thiết bị chạy Android (API từ 24 đến 36).
   - Bấm nút **Run** trong Android Studio hoặc dùng lệnh:
     ```bash
     ./gradlew assembleDebug
     ```

## 📝 Giấy phép (License)
Dự án được xem xét sử dụng với mục đích cá nhân và nghiên cứu về các khía cạnh quản lý thời gian trên nền tảng số.

