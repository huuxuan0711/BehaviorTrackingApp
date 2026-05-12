# Digital Wellbeing

Dự án Android: **Digital Wellbeing kết hợp AI-Assisted Behavioral Insights**

Ứng dụng không chỉ đóng vai trò là một "app quản lý thời gian", mà còn là một trợ lý phân tích thói quen sử dụng điện thoại, biến những con số thời lượng thô thành những hiểu biết (insights) hữu ích thông qua AI để giúp người dùng cải thiện sức khoẻ kỹ thuật số thực sự.

## 🎯 Project Philosophy

Ứng dụng này tập trung vào việc chuyển đổi dữ liệu thời gian sử dụng màn hình (screen-time data) thô thành nhận thức sâu sắc về hành vi (behavioral insights), đồng thời duy trì trải nghiệm người dùng tối giản, thân thiện, không gây quá tải thông tin.

Bên cạnh đó, dự án tuân thủ kiến trúc **local-first** để bảo vệ quyền riêng tư, giảm độ phức tạp về hạ tầng mạng, đảm bảo ứng dụng luôn hoạt động ổn định kể cả khi không có kết nối internet (offline-first reliability).

## 📱 Screenshots

> `[Dashboard]`
> `[Timeline]` `[Analysis]` `[Categories]` `[App Detail]`

## 🌟 Tính năng nổi bật & Chiều sâu sản phẩm

- **Quản lý & Theo dõi sử dụng (Usage Tracking):** Dễ dàng theo dõi thời lượng sử dụng chi tiết theo ngày, tuần và đặc biệt là phân tích theo phiên sử dụng (sessions).
- **Phân tích hành vi (Behavioral Analysis):** Nhấn mạnh vào xu hướng và thói quen thay vì chỉ hiện con số:
  - **Late Night:** Đánh giá thói quen thức khuya sử dụng điện thoại.
  - **Transitions:** Phân tích mô hình chuyển đổi qua lại giữa các ứng dụng gây xao nhãng.
  - **Categories:** Phân loại và tổng kết thời gian dành cho giải trí, công việc, mạng xã hội, v.v.
- **AI-Assisted Behavioral Insights:** Ứng dụng tích hợp cấu trúc LLM-enhanced explanation layer (kết nối với Gemini 2.5 Flash), đóng vai trò phân tích các pattern thống kê, từ đó đưa ra lời khuyên cá nhân hóa một cách tự nhiên.
- **Trực quan hoá thân thiện:** Bản đồ xu hướng hằng ngày và hằng tuần được mô tả dễ nhìn bằng biểu đồ sắc nét (MPAndroidChart), giúp insight tiếp cận người dùng dễ dàng hơn.

## 🏛 Architecture Diagram

Sự phân chia luồng dữ liệu tuân thủ Clean Architecture:

```text
       UsageStats (Raw OS Events)
                  ↓
          Tracking Layer (Worker)
                  ↓
       Room Database (Local-first)
                  ↓
        Insight Engine (Domain Logic)
                  ↓
  Presentation Layer (Clean UI/Flow/MVVM)
                  ↓
  AI Explanation Layer (LLM Enhancement)
```

## 🛠️ Kiến trúc và Công nghệ (Tech Stack)

Dự án sử dụng Kotlin kết hợp kiến trúc **Clean Architecture** và các tiêu chuẩn tốt nhất trong Android:

*   **Kiến trúc:** Clean Architecture + MVVM
*   **Asynchronous:** Kotlin Coroutines & Flow
*   **Dependency Injection:** Dagger Hilt
*   **Network / AI Layer:** Retrofit2, OkHttp3 giao tiếp với Gemini API
*   **Persistence:** Room Database, DataStore Preferences
*   **Background processing:** WorkManager

## 🧠 Core Modules

Thay vì liệt kê package kỹ thuật đơn thuần, hệ thống được cấu trúc dựa trên các Business Modules chính:

- **Usage Tracking Engine:** Module thu thập, làm sạch và định cấu trúc luồng sự kiện từ OS.
- **Session Analysis:** Trích xuất các phiên sử dụng liền mạch, tính số lần mở app.
- **Insight Engine:** Phân tích quy luật, độ dài thời gian, phân loại theo category, cảnh báo ranh giới.
- **AI Explanation Layer:** Xử lý format dữ liệu, truyền ngữ cảnh cho LLM để kết xuất thành thông điệp khuyên dùng tự nhiên.

## ⚡ Engineering Challenges

Quá trình phát triển gặp phải những bài toán thử thách đã được xử lý để mang lại trải nghiệm hoàn thiện:
- **Session reconstruction:** Tái tạo lại chuỗi phiên sử dụng từ luồng sự kiện UsageStats phân mảnh của hệ điều hành.
- **Behavioral transition analysis:** Xây dựng logic phân tích hành vi chuyển đổi nhanh chóng qua lại giữa các ứng dụng (context switching/doomscrolling).
- **Balancing insight richness with clean UI/UX:** Cân bằng giữa lượng thông tin Insights khổng lồ và một giao diện trực quan không làm choáng ngợp người dùng.
- **Designing a scalable local-first architecture:** Thiết kế DB nội bộ đủ khả năng thực hiện truy vấn phức tạp đồng thời hạn chế các tác vụ ngốn tài nguyên khi offline.

## 🚀 Cài đặt và Chạy thử nghiệm

Để build và chạy dự án này trên môi trường phát triển của bạn:

1. **Clone dự án:**
   ```bash
   git clone <repository_url>
   ```
2. **Mở dự án** thông qua Android Studio (Hỗ trợ AGP 8.12.3).
3. **Cấu hình AI Key:** Mặc định đã cấu hình sẵn trong `app/build.gradle.kts` (GEMINI_API_KEY). Có thể thay đổi nếu cần.
4. **Build & Run:**
   - Đảm bảo thiết bị Android hỗ trợ SDK 24 đến 36.
   - Bấm **Run** hoặc dùng `./gradlew assembleDebug`.

## 🔮 Future Improvements

Tương lai dự án có thể mở rộng với:
- Cloud synchronization
- Cross-device wellbeing tracking (Quản lý đa thiết bị)
- Adaptive onboarding (Chào mừng cá nhân hoá)
- Personalized recommendation system (Hệ thống gợi ý thông minh dựa trên ngữ cảnh thực)
- Advanced on-device ML insights (Đưa mô hình ML nhỏ hoàn toàn offline)

## 📝 Giấy phép (License)

Dự án này phục vụ với mục đích giáo dục, nghiên cứu cá nhân và làm Portfolio chứng minh năng lực phát triển phần mềm. 
Được phân phối dưới giấy phép **MIT License**. Chi tiết xem tệp "LICENSE" để biết thêm.

