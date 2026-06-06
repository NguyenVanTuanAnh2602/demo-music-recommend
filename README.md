# Music Recommendation System — DAA Project

## 1. Tổng quan

Project xây dựng hệ thống gợi ý bài hát dựa trên hành vi nghe nhạc của người dùng (collaborative filtering). Trọng tâm **không phải** là độ chính xác của mô hình gợi ý, mà là **thiết kế và tối ưu thuật toán** theo tư duy DAA:

- Giảm số phép tính
- Giảm độ phức tạp thuật toán
- Tránh tính lặp
- Tăng khả năng mở rộng khi dữ liệu lớn

---

## 2. Bài toán

### Dữ liệu đầu vào

File CSV với định dạng:

```
user_id | track_id | track_name | artist_id | artist_name | play_count | play_count_log
```

Mỗi dòng biểu diễn: user X nghe bài Y với Z lần.

### Mô hình hóa

Mỗi user được biểu diễn như một **sparse vector** bài hát:

```
User A = { song_25: 5.0, song_40: 3.0, ... }
User B = { song_25: 4.0, song_12: 2.0, ... }
```

Độ giống nhau giữa hai user → **cosine similarity** trên sparse vector.

Pipeline tổng thể:

```
CSV → DataLoader → User/Song/Interaction → Recommender → Similarity + Scoring → Result → Exporter
```

### Vấn đề hiệu năng

Nếu thực hiện trực tiếp (so sánh mọi cặp user, duyệt toàn bộ bài hát):

```
O(U² × I)
```

Với U = số user, I = số bài hát. Khi dữ liệu lớn, chi phí tăng rất nhanh → cần các phương pháp tối ưu.

---

## 3. Các thuật toán được triển khai

| Class | Phương pháp | Ý tưởng chính |
|---|---|---|
| `BruteForceRecommender` | Vét cạn | Tính similarity với toàn bộ user, duyệt toàn bộ bài hát. Dùng làm baseline. |
| `TopKRecommender` | Greedy / Top-K | Chỉ giữ lại K user có similarity cao nhất, giảm lượng dữ liệu xử lý. |
| `CachingRecommender` | Dynamic Programming / Memoization | Lưu kết quả `similarity(u, v)` vào cache, tránh tính lặp. |
| `PruningRecommender` | Branch and Bound | Dừng sớm phép tính cosine nếu kết quả không thể vượt top-K hiện tại. |
| `HeuristicRecommender` | Heuristic | Chỉ xét user có bài hát chung, bỏ qua các cặp user không có điểm giao. |

Tất cả implement chung interface `Recommender`:

```java
public interface Recommender {
    List<Song> recommend(User user, int k);
}
```

---

## 4. Cấu trúc project

```
src/
├── algorithm/
│   ├── Recommender.java            # Interface chung
│   ├── BruteForceRecommender.java  # [todo] Baseline vét cạn
│   ├── TopKRecommender.java        # [todo] Greedy top-K neighbors
│   ├── CachingRecommender.java     # [todo] Memoization similarity
│   ├── PruningRecommender.java     # [todo] Branch & bound pruning
│   └── HeuristicRecommender.java   # [todo] Heuristic candidate filter
│
├── data/
│   ├── DataLoader.java             # [done] Đọc CSV, tạo sparse structure
│   ├── User.java                   # [done] Entity user
│   ├── Song.java                   # [done] Entity bài hát
│   └── Interaction.java            # [done] Lưu (user, song, playCount)
│
├── util/
│   ├── Similarity.java             # [todo] Cosine similarity (sparse)
│   └── Scoring.java                # [todo] Tính điểm recommendation
│
├── benchmark/
│   ├── BenchmarkRunner.java        # [done] Chạy & đo thời gian
│   └── BenchmarkResult.java        # [done] Lưu kết quả benchmark
│
├── exporter/
│   ├── BenchmarkExporter.java      # [done] Xuất benchmark ra CSV
│   └── RecommendationExporter.java # [done] Xuất recommendation ra CSV
│
└── Test.java                       # [done] Main: load → recommend → benchmark → export
```

---

## 5. Các tối ưu kỹ thuật quan trọng

### Sparse vector
Mỗi user chỉ nghe một phần nhỏ tổng số bài hát → dùng `Map<Song, Interaction>` thay vì mảng đầy đủ. Khi tính cosine similarity, chỉ duyệt **phần giao** giữa hai user thay vì toàn bộ không gian bài hát.

### Object caching trong DataLoader
`userCache` và `songCache` đảm bảo không tạo object trùng khi load dữ liệu. Interaction trùng được **merge trực tiếp** thay vì tạo mới.

### Warm-up trong BenchmarkRunner
Chạy 2 lần warm-up trước khi đo để loại bỏ ảnh hưởng của JIT compilation, sau đó lấy trung bình 5 lần đo.

---

## 6. Tiêu chí đánh giá thuật toán (DAA)

| Tiêu chí | Nội dung |
|---|---|
| Độ phức tạp thời gian | Phân tích best/average/worst case |
| Độ phức tạp không gian | Bộ nhớ phụ, cache, recursion stack |
| Tính đúng đắn | Luôn cho nghiệm đúng? Có thể bỏ sót? |
| Tính tối ưu | Đảm bảo tối ưu tuyệt đối hay xấp xỉ? |
| Độ phức tạp cài đặt | Dễ code, dễ debug, dễ mở rộng? |
| Khả năng mở rộng | Khi U, I tăng lớn thì thuật toán còn khả thi không? |

---

## 7. Output

| File | Nội dung |
|---|---|
| `output/benchmark.csv` | `Algorithm \| AvgTime(ns) \| K` |
| `output/recommendations.csv` | `User \| Artist \| Track` |

---

## 8. Mục tiêu cuối cùng

Chứng minh rằng cùng một bài toán recommendation, các phương pháp thiết kế thuật toán khác nhau tạo ra sự khác biệt rõ rệt về **thời gian chạy** và **khả năng mở rộng** — đây là trọng tâm chính của project theo định hướng DAA.

## 9. Kết quả

- Brute Force đạt độ chính xác tham chiếu (Recovery = 100%) nhưng có độ phức tạp cao, không phù hợp với hệ thống quy mô lớn.
- Caching chỉ mang lại cải thiện hiệu năng nhỏ do đặc thù benchmark không có các truy vấn lặp lại.
- Greedy cho kết quả tốt nhất về cân bằng giữa tốc độ và độ chính xác:
  - GreedyNeighbour: tăng tốc 7.51×
  - GreedySong: tăng tốc 7.62×
  - Recovery: 100%
- Heuristic + Inverted Index đạt tốc độ cao nhất:
  - Tăng tốc 13.92×
  - Recovery: 89.2%
Phù hợp cho các hệ thống ưu tiên hiệu năng.
- Pruning (Branch and Bound) tăng tốc 4.03× và vẫn duy trì Recovery 100%, phù hợp khi yêu cầu tính tối ưu tuyệt đối.
