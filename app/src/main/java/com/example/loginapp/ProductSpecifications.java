package com.example.loginapp;

public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static String description(String name) {
        if (name.contains("iPhone"))
            return "Thiết kế gọn, thuận tiện sử dụng hằng ngày. Phù hợp liên lạc, chụp ảnh, học tập"
                    + " và giải trí.";
        if (name.contains("Galaxy"))
            return "Điện thoại gọn, màn hình dễ quan sát. Phù hợp làm việc di động, chụp ảnh và"
                    + " giải trí hằng ngày.";
        if (name.contains("Dell"))
            return "Laptop nhỏ gọn, dễ mang theo. Phù hợp học tập, soạn thảo tài liệu, làm việc văn"
                    + " phòng và di chuyển thường xuyên.";
        if (name.contains("Sony"))
            return "Thiết kế chụp tai, hỗ trợ giảm tiếng ồn xung quanh. Phù hợp nghe nhạc, họp trực"
                    + " tuyến và tập trung khi học tập hoặc làm việc.";
        if (name.contains("MX Master"))
            return "Thiết kế ôm tay, thuận tiện thao tác trong thời gian dài. Phù hợp công việc văn"
                    + " phòng, xử lý bảng tính và sử dụng nhiều ứng dụng.";
        if (name.contains("Keychron"))
            return "Bàn phím gọn giúp tiết kiệm diện tích bàn. Phù hợp gõ văn bản, lập trình, học"
                    + " tập và làm việc hằng ngày.";
        if (name.contains("LG"))
            return "Không gian hiển thị rộng, thuận tiện theo dõi tài liệu và làm việc nhiều cửa"
                    + " sổ. Phù hợp học tập, văn phòng và xem nội dung giải trí.";
        return "";
    }

    public static String details(String name) {
        if (name.contains("iPhone 15"))
            return "Màn hình: OLED Super Retina XDR 6,1 inch; 2556 × 1179\n"
                    + "Chip: A16 Bionic\n"
                    + "Camera chính: 48 MP; góc siêu rộng 12 MP\n"
                    + "Kết nối: 5G, USB-C\n"
                    + "Dung lượng có trên dòng máy: 128 / 256 / 512 GB\n"
                    + "Khối lượng: 171 g";
        if (name.contains("WH-1000XM5"))
            return "Kiểu: Tai nghe chụp tai, chống ồn\n"
                    + "Điều khiển: Cảm ứng\n"
                    + "Pin nghe nhạc tối đa: 30 giờ bật chống ồn; 40 giờ tắt chống ồn\n"
                    + "Sạc đầy tham khảo: khoảng 3,5 giờ\n"
                    + "Codec: SBC, AAC, LDAC\n"
                    + "Có chế độ âm thanh môi trường\n"
                    + "Thời lượng thực tế tùy điều kiện sử dụng";
        if (name.contains("Galaxy S24"))
            return "Dòng máy: Galaxy S24 (không phải S24+ hoặc Ultra)\n"
                    + "Thông số bộ nhớ và bộ xử lý thay đổi theo thị trường, phiên bản.";
        if (name.contains("Keychron K2"))
            return "Phiên bản tham khảo: Keychron K2 V2\n"
                    + "Bố cục: 75%, gọn hơn bàn phím đầy đủ\n"
                    + "Kết nối: Bluetooth hoặc cáp USB-C\n"
                    + "Pin: 4.000 mAh\n"
                    + "Hỗ trợ: macOS và Windows\n"
                    + "Switch, đèn và khả năng hot-swap tùy biến thể.";
        if (name.contains("Dell"))
            return "Model tham khảo: Dell XPS 13 9310\n"
                    + "Bộ xử lý: Intel Core i5-1135G7 thế hệ 11\n"
                    + "CPU: 4 nhân, 8 luồng, tối đa 4,2 GHz\n"
                    + "Màn hình tham khảo: 13,4 inch FHD+ 1920 × 1200, 60 Hz\n"
                    + "RAM/SSD: tùy cấu hình đặt hàng; đối chiếu cấu hình cửa hàng\n"
                    + "Thiết kế gọn phục vụ học tập và làm việc di động.";
        if (name.contains("MX Master"))
            return "Model tham khảo: Logitech MX Master 3S\n"
                    + "Cảm biến: tối đa 8.000 DPI\n"
                    + "Khối lượng: khoảng 141 g\n"
                    + "Thiết kế: công thái học cho tay phải\n"
                    + "Kết nối và phụ kiện đi kèm tùy phiên bản Bluetooth/Business.";
        if (name.contains("LG"))
            return "Model tham khảo: LG 27MP400-B\n"
                    + "Kích thước: 27 inch\n"
                    + "Tấm nền: IPS\n"
                    + "Độ phân giải: Full HD 1920 × 1080\n"
                    + "Tần số quét: tối đa 75 Hz\n"
                    + "Thiết kế: viền mỏng ba cạnh\n"
                    + "Phù hợp học tập, văn phòng và giải trí.";
        return "";
    }
}
