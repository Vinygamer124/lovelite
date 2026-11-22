# Ngày Yêu Lite

Ứng dụng đếm ngày yêu siêu nhẹ cho Android — **không quảng cáo, không quyền truy cập nào, không mạng**.

- Đếm ngày bên nhau, ảnh nền tuỳ chọn (tự thu nhỏ ≤1080px)
- Mốc sắp tới: 100, 200… 1000 ngày, mỗi 500 ngày, kỷ niệm theo năm
- Nhật ký ngắn, khoá PIN, sao lưu/khôi phục JSON
- 2 widget (4x2, 2x2) chỉ cập nhật khi qua ngày mới → gần như không tốn pin
- Vi / En, không thư viện ngoài (chỉ Android framework + Kotlin), bật R8 + shrinkResources

## Build
GitHub Actions tự build khi push lên `main` (tab **Actions → Build APK → Artifacts**).
Hoặc cục bộ: `gradle :app:assembleRelease` (JDK 17, Android SDK 34). APK release đang ký bằng khoá debug —
thay bằng keystore riêng trong `app/build.gradle.kts` khi phát hành.
