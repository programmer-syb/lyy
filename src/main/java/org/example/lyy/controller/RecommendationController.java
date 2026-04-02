package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.entity.MovieRating;
import org.example.lyy.entity.User;
import org.example.lyy.service.MovieRatingService;
import org.example.lyy.service.MovieService;
import org.example.lyy.service.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    @Resource
    private MovieService movieService;

    @Resource
    private MovieRatingService ratingService;

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 移除硬编码的偏好/屏蔽常量，改为动态从用户表获取
    private static final Map<Long, Map<Long, Double>> ratingMatrix = new HashMap<>();
    private static final Map<Long, List<Long>> svdRecommendCache = new HashMap<>();
    private static final Map<Long, List<Long>> alsRecommendCache = new HashMap<>();
    // 无偏好时的默认推荐类型（兜底策略，非写死用户偏好）
    private static final List<String> DEFAULT_RECOMMEND_GENRES = Arrays.asList("科幻", "动作", "剧情");

    @GetMapping("/hot")
    public Result<List<Movie>> getHotRecommendations(@RequestParam(defaultValue = "10") Integer limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Movie::getHeat)
                .orderByDesc(Movie::getRatingCount)
                .last("LIMIT " + limit);
        List<Movie> hotMovies = movieService.list(wrapper);
        return Result.success(hotMovies);
    }

    @GetMapping("/svd")
    public Result<List<Movie>> getSvdRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        // 获取当前用户的个性化偏好
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        buildRatingMatrix();

        List<Long> recMovieIds = svdRecommendCache.getOrDefault(userId, new ArrayList<>());
        if (recMovieIds.isEmpty()) {
            // 传入用户对象，动态适配偏好
            recMovieIds = betterSvdRecommend(userId, currentUser);
            svdRecommendCache.put(userId, recMovieIds);
        }

        List<Movie> movies = movieService.listByIds(recMovieIds);
        // 传入用户对象，动态过滤（基于用户偏好）
        List<Movie> filtered = filterPreferredMovies(movies, currentUser);
        return Result.success(filtered);
    }

    @GetMapping("/als")
    public Result<List<Movie>> getAlsRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        buildRatingMatrix();

        List<Long> recMovieIds = alsRecommendCache.getOrDefault(userId, new ArrayList<>());
        if (recMovieIds.isEmpty()) {
            recMovieIds = betterAlsRecommend(userId, currentUser);
            alsRecommendCache.put(userId, recMovieIds);
        }

        List<Movie> movies = movieService.listByIds(recMovieIds);
        List<Movie> filtered = filterPreferredMovies(movies, currentUser);
        return Result.success(filtered);
    }

    @GetMapping("/online/similar")
    public Result<List<Movie>> getOnlineSimilarMovies(
            @RequestParam Long targetMovieId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        Movie targetMovie = movieService.getById(targetMovieId);
        if (targetMovie == null || !StringUtils.hasText(targetMovie.getGenres())) {
            return Result.success(Collections.emptyList());
        }

        LambdaQueryWrapper<MovieRating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(MovieRating::getUserId, userId).select(MovieRating::getMovieId);
        List<Long> watchedMovieIds = ratingService.list(ratingWrapper).stream()
                .map(MovieRating::getMovieId)
                .collect(Collectors.toList());

        String[] genres = targetMovie.getGenres().split(",");
        LambdaQueryWrapper<Movie> similarWrapper = new LambdaQueryWrapper<>();
        similarWrapper.and(w -> {
            for (int i = 0; i < genres.length; i++) {
                if (i == 0) w.like(Movie::getGenres, genres[i].trim());
                else w.or().like(Movie::getGenres, genres[i].trim());
            }
        });

        watchedMovieIds.add(targetMovieId);
        similarWrapper.notIn(Movie::getId, watchedMovieIds)
                .orderByDesc(Movie::getGlobalRating)
                .last("LIMIT 8");

        List<Movie> similarMovies = movieService.list(similarWrapper);
        // 基于当前用户偏好过滤相似推荐
        List<Movie> filtered = filterPreferredMovies(similarMovies, currentUser);
        return Result.success(filtered);
    }

    @GetMapping("/cold-start")
    public Result<List<Movie>> getColdStartRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);

        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        // 动态解析用户偏好（无硬编码）
        extracted(currentUser, wrapper);
        wrapper.orderByDesc(Movie::getHeat).last("LIMIT 10");
        List<Movie> list = movieService.list(wrapper);

        // 无偏好结果时，用默认类型兜底（非写死用户偏好）
        if (list.isEmpty()) {
            LambdaQueryWrapper<Movie> defaultWrapper = new LambdaQueryWrapper<>();
            defaultWrapper.and(w -> {
                for (int i = 0; i < DEFAULT_RECOMMEND_GENRES.size(); i++) {
                    if (i == 0) w.like(Movie::getGenres, DEFAULT_RECOMMEND_GENRES.get(i));
                    else w.or().like(Movie::getGenres, DEFAULT_RECOMMEND_GENRES.get(i));
                }
            });
            defaultWrapper.orderByDesc(Movie::getHeat).last("LIMIT 10");
            list = movieService.list(defaultWrapper);
        }
        return Result.success(list);
    }

    // ==================== 核心算法重构（动态适配用户偏好） ====================
    private void buildRatingMatrix() {
        if (!ratingMatrix.isEmpty()) return;
        List<MovieRating> allRatings = ratingService.list();
        for (MovieRating r : allRatings) {
            BigDecimal bd = r.getRating();
            double score = (bd == null) ? 0.0 : bd.doubleValue();
            ratingMatrix.computeIfAbsent(r.getUserId(), k -> new HashMap<>()).put(r.getMovieId(), score);
        }
    }

    /**
     * 重构SVD推荐：传入用户对象，动态计算偏好权重
     */
    private List<Long> betterSvdRecommend(Long userId, User currentUser) {
        Map<Long, Double> userRatings = ratingMatrix.getOrDefault(userId, new HashMap<>());
        Set<Long> watched = userRatings.keySet();
        Map<Long, Double> predict = new HashMap<>();
        List<Movie> all = movieService.list();

        for (Movie m : all) {
            if (watched.contains(m.getId())) continue;

            double base = 0.0;
            int cnt = 0;
            for (Map.Entry<Long, Double> entry : userRatings.entrySet()) {
                Movie wm = movieService.getById(entry.getKey());
                if (wm == null) continue;
                // 动态判断类型相似度（无硬编码）
                if (isSimilarGenres(m.getGenres(), wm.getGenres())) {
                    base += entry.getValue();
                    cnt++;
                }
            }

            double score = cnt > 0 ? base / cnt : 0;
            // 动态计算用户偏好权重（替代硬编码的PREFERRED_GENRES）
            score += calcGenreWeight(m.getGenres(), currentUser);
            predict.put(m.getId(), score);
        }

        return predict.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(12)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 重构ALS推荐：传入用户对象，动态计算偏好权重
     */
    private List<Long> betterAlsRecommend(Long userId, User currentUser) {
        Map<Long, Double> userRatings = ratingMatrix.getOrDefault(userId, new HashMap<>());
        Set<Long> watched = userRatings.keySet();
        Map<Long, Double> predict = new HashMap<>();
        List<Movie> all = movieService.list();

        double userVec = calcUserVector(userId);

        for (Movie m : all) {
            if (watched.contains(m.getId())) continue;
            double movieVec = calcMovieVector(m.getId());
            double score = userVec * movieVec * 1.5;
            // 动态计算用户偏好权重
            score += calcGenreWeight(m.getGenres(), currentUser);
            predict.put(m.getId(), score);
        }

        return predict.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(12)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 重构：动态计算用户偏好权重（从用户表preferences获取，无硬编码）
     * @param genres 电影类型
     * @param user 当前用户（含个性化偏好）
     * @return 动态权重
     */
    private double calcGenreWeight(String genres, User user) {
        if (!StringUtils.hasText(genres)) return 0;
        double weight = 0;

        // 1. 动态获取用户偏好类型（逗号分隔，如"科幻,动作"）
        List<String> userPreferredGenres = getUserPreferredGenres(user);
        // 2. 匹配用户偏好则加分（权重可配置，非写死）
        for (String g : userPreferredGenres) {
            if (genres.contains(g.trim())) {
                weight += 1.8; // 偏好匹配加分，值可抽成配置
            }
        }

        // 【可选扩展】如果需要屏蔽类型，可在user表加blocked_genres字段，此处动态获取
        // List<String> userBlockedGenres = getUserBlockedGenres(user);
        // for (String g : userBlockedGenres) {
        //     if (genres.contains(g.trim())) weight -= 100;
        // }

        return weight;
    }

    /**
     * 解析用户偏好类型（适配user表preferences字段，逗号分隔）
     */
    private List<String> getUserPreferredGenres(User user) {
        List<String> preferred = new ArrayList<>();
        if (user == null || !StringUtils.hasText(user.getPreferences())) {
            // 无偏好时返回默认兜底类型（非写死用户偏好，仅兜底）
            return DEFAULT_RECOMMEND_GENRES;
        }
        // 拆分用户偏好字符串（如"科幻,动作" → ["科幻","动作"]）
        String[] prefs = user.getPreferences().split(",");
        for (String p : prefs) {
            if (StringUtils.hasText(p)) {
                preferred.add(p.trim());
            }
        }
        return preferred.isEmpty() ? DEFAULT_RECOMMEND_GENRES : preferred;
    }

    /**
     * 【可选扩展】解析用户屏蔽类型（需在user表新增blocked_genres字段）
     */
    // private List<String> getUserBlockedGenres(User user) {
    //     List<String> blocked = new ArrayList<>();
    //     if (user == null || !StringUtils.hasText(user.getBlockedGenres())) {
    //         return blocked;
    //     }
    //     String[] blocks = user.getBlockedGenres().split(",");
    //     for (String b : blocks) {
    //         if (StringUtils.hasText(b)) {
    //             blocked.add(b.trim());
    //         }
    //     }
    //     return blocked;
    // }

    private boolean isSimilarGenres(String g1, String g2) {
        if (!StringUtils.hasText(g1) || !StringUtils.hasText(g2)) return false;
        for (String t : g1.split(",")) {
            if (g2.contains(t.trim())) return true;
        }
        return false;
    }

    /**
     * 重构过滤逻辑：基于当前用户偏好过滤（无硬编码屏蔽）
     */
    private List<Movie> filterPreferredMovies(List<Movie> movies, User user) {
        if (movies.isEmpty() || user == null) return movies;

        List<String> userPreferredGenres = getUserPreferredGenres(user);
        return movies.stream()
                .filter(m -> {
                    String g = m.getGenres();
                    if (!StringUtils.hasText(g)) return true;

                    // 【可选】仅保留匹配用户偏好的电影（按需选择）
                    // return userPreferredGenres.stream().anyMatch(pre -> g.contains(pre));

                    // 基础逻辑：不屏蔽任何类型，仅按偏好权重排序
                    return true;
                })
                .sorted((a, b) -> Double.compare(
                        // 动态计算评分+偏好权重
                        b.getGlobalRating().doubleValue() + calcGenreWeight(b.getGenres(), user),
                        a.getGlobalRating().doubleValue() + calcGenreWeight(a.getGenres(), user)
                ))
                .limit(10)
                .collect(Collectors.toList());
    }

    private double calcUserVector(Long userId) {
        List<MovieRating> list = ratingService.list(new LambdaQueryWrapper<MovieRating>().eq(MovieRating::getUserId, userId));
        if (list.isEmpty()) return 0.6;
        double sum = 0;
        int c = 0;
        for (MovieRating r : list) {
            if (r.getRating() != null) {
                sum += r.getRating().doubleValue();
                c++;
            }
        }
        return c == 0 ? 0.6 : (sum / c) / 5.0;
    }

    private double calcMovieVector(Long movieId) {
        Movie m = movieService.getById(movieId);
        return (m == null || m.getGlobalRating() == null) ? 0.6 : m.getGlobalRating().doubleValue() / 5.0;
    }

    /**
     * 动态解析用户偏好，拼接查询条件（无硬编码）
     */
    private static void extracted(User user, LambdaQueryWrapper<Movie> wrapper) {
        if (user != null && StringUtils.hasText(user.getPreferences())) {
            String[] prefs = user.getPreferences().split(",");
            wrapper.and(w -> {
                for (int i = 0; i < prefs.length; i++) {
                    String pref = prefs[i].trim();
                    if (i == 0) {
                        w.like(Movie::getGenres, pref);
                    } else {
                        w.or().like(Movie::getGenres, pref);
                    }
                }
            });
        }
    }
}