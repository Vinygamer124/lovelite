# Ngày Yêu Lite

Ứng dụng đếm ngày yêu siêu nhẹ cho Android — **không quảng cáo, không quyền truy cập nào, không mạng**.

## Tính năng (v1.1)
- Mặc định **tiếng Việt** (đổi sang English trong *Tuỳ chọn → Ngôn ngữ*), không phụ thuộc ngôn ngữ hệ thống
- **Ảnh đại diện** cho cả hai người (chạm vào ảnh ở màn hình chính hoặc trong *Sửa thông tin*), tự cắt vuông và thu nhỏ
- Hiển thị **tên, giới tính, tuổi, cung hoàng đạo** của hai người
- **Lời chúc từ app**: chào theo giờ trong ngày, lời nhắn đổi mỗi ngày, chúc mừng sinh nhật / kỷ niệm tháng, năm / mốc 100 ngày
- Mốc sắp tới gồm tròn trăm ngày, kỷ niệm năm và **sinh nhật hai người**
- 2 widget (4x2, 2x2): ảnh đại diện, số ngày, tuổi + cung, lời chúc; nền gradient hoặc ảnh nền (bật/tắt được), tự vẽ lại khi đổi kích thước
- Nhật ký ngắn, khoá PIN, sao lưu/khôi phục JSON (kèm ảnh)
- Không thư viện ngoài (chỉ Android framework + Kotlin), bật R8 + shrinkResources

## Build
GitHub Actions tự build khi push lên `main` (tab **Actions → Build APK → Artifacts**).
Hoặc cục bộ: `gradle :app:assembleRelease` (JDK 17, Android SDK 34). APK release đang ký bằng khoá debug —
thay bằng keystore riêng trong `app/build.gradle.kts` khi phát hành.
