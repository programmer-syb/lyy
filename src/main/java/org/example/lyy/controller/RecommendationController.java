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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * 1. 热门推荐 (RC-01) - 游客和登录用户均可看
     * 逻辑: 取 rating_count > 10 的电影，按 (global_rating * rating_count) 降序排，或者直接按 heat(热度) 降序
     */
    @GetMapping("/hot")
    public Result<List<Movie>> getHotRecommendations(@RequestParam(defaultValue = "10") Integer limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        // 假设 heat 字段是通过定时任务根据评分人数和平均分算好的，直接排序最快
        // 如果没有热度字段，也可以写原生 SQL 计算：ORDER BY (global_rating * rating_count) DESC
        wrapper.orderByDesc(Movie::getHeat)
                .orderByDesc(Movie::getRatingCount)
                .last("LIMIT " + limit);
        List<Movie> hotMovies = movieService.list(wrapper);
        return Result.success(hotMovies);
    }

    /**
     * 2. SVD 离线推荐 (RC-02) - 演戏方案：基于用户偏好的随机高分推荐 [cite: 6]
     */
    @GetMapping("/svd")
    public Result<List<Movie>> getSvdRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = userService.getById(userId);

        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        // 推荐底线：只推荐评分大于 4.0 的好电影
        wrapper.ge(Movie::getGlobalRating, 4.0);

        // 假设 user 表的 preferences 存的是逗号分隔的类型，如 "动作,科幻"
        extracted(user, wrapper);

        // 使用 MySQL 的 RAND() 随机抽取 10 条，制造出“每次推荐都不一样”的算法假象
        wrapper.last("ORDER BY RAND() LIMIT 10");
        List<Movie> list = movieService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 3. ALS 离线推荐 (RC-03) - 演戏方案：基于用户历史最高评分的相似推荐 [cite: 6]
     */
    @GetMapping("/als")
    public Result<List<Movie>> getAlsRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");

        // 1. 找到该用户评过最高分的一部电影
        LambdaQueryWrapper<MovieRating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(MovieRating::getUserId, userId)
                .orderByDesc(MovieRating::getRating)
                .last("LIMIT 1");
        MovieRating highestRating = ratingService.getOne(ratingWrapper);

        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (highestRating != null) {
            // 2. 找到这部最高分电影
            Movie targetMovie = movieService.getById(highestRating.getMovieId());
            if (targetMovie != null && StringUtils.hasText(targetMovie.getGenres())) {
                // 取它的第一个类型去匹配
                String firstGenre = targetMovie.getGenres().split(",")[0];
                wrapper.like(Movie::getGenres, firstGenre);
                wrapper.ne(Movie::getId, targetMovie.getId()); // 排除已看过的这部电影本身
            }
        }

        wrapper.ge(Movie::getGlobalRating, 3.5); // 评分底线降一点，扩大池子
        wrapper.last("ORDER BY RAND() LIMIT 10");
        List<Movie> list = movieService.list(wrapper);
        return Result.success(list);
    }


    /**
     * 4. 实时在线相似推荐 (RC-04)
     * 触发场景: 用户刚给某部电影打了高分（比如4星以上），实时推荐
     */
    @GetMapping("/online/similar")
    public Result<List<Movie>> getOnlineSimilarMovies(
            @RequestParam Long targetMovieId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");

        // 1. 获取目标电影的信息（提取它的类型 genres）
        Movie targetMovie = movieService.getById(targetMovieId);
        if (targetMovie == null || !StringUtils.hasText(targetMovie.getGenres())) {
            return Result.success(Collections.emptyList());
        }

        // 2. 查出当前用户已经评价过或看过的电影ID，避免重复推荐
        LambdaQueryWrapper<MovieRating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(MovieRating::getUserId, userId).select(MovieRating::getMovieId);
        List<Long> watchedMovieIds = ratingService.list(ratingWrapper).stream()
                .map(MovieRating::getMovieId)
                .collect(Collectors.toList());

        // 3. 在数据库中寻找拥有相同类型的电影，排除已看过的
        // 这里做一个简单的基于内容的过滤 (Content-Based)
        String[] genres = targetMovie.getGenres().split(",");
        LambdaQueryWrapper<Movie> similarWrapper = new LambdaQueryWrapper<>();

        // 拼接 LIKE 条件 (只要匹配其中一个类型即可)
        similarWrapper.and(w -> {
            for (int i = 0; i < genres.length; i++) {
                if (i == 0) w.like(Movie::getGenres, genres[i].trim());
                else w.or().like(Movie::getGenres, genres[i].trim());
            }
        });

        // 排除自己和已看过的电影
        watchedMovieIds.add(targetMovieId);
        similarWrapper.notIn(Movie::getId, watchedMovieIds)
                .orderByDesc(Movie::getGlobalRating) // 在相似类型里挑评分高的
                .last("LIMIT 8");

        List<Movie> similarMovies = movieService.list(similarWrapper);
        return Result.success(similarMovies);
    }

    /**
     * 5. 冷启动推荐 (RC-05) - 专供新用户或无评分记录的用户
     */
    @GetMapping("/cold-start")
    public Result<List<Movie>> getColdStartRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = userService.getById(userId);

        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();

        // 1. 获取用户的偏好设置（假设存的是 JSON 或逗号分隔的字符串，如 "科幻,动作"）
        extracted(user, wrapper);

        // 2. 在符合偏好的电影中，按热度（或评分人数）降序排列，取 Top 10
        wrapper.orderByDesc(Movie::getHeat)
                .orderByDesc(Movie::getRatingCount)
                .last("LIMIT 10");

        List<Movie> list = movieService.list(wrapper);

        // 兜底策略：如果按偏好没查到（或用户没填偏好），直接退化为全局热门推荐
        if (list.isEmpty()) {
            LambdaQueryWrapper<Movie> hotWrapper = new LambdaQueryWrapper<>();
            hotWrapper.orderByDesc(Movie::getHeat).last("LIMIT 10");
            list = movieService.list(hotWrapper);
        }

        return Result.success(list);
    }

    private static void extracted(User user, LambdaQueryWrapper<Movie> wrapper) {
        if (user != null && StringUtils.hasText(user.getPreferences())) {
            String[] prefs = user.getPreferences().split(",");
            wrapper.and(w -> {
                for (int i = 0; i < prefs.length; i++) {
                    if (i == 0) w.like(Movie::getGenres, prefs[i]);
                    else w.or().like(Movie::getGenres, prefs[i]);
                }
            });
        }
    }
}