package org.example.lyy.controller.admin;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.lyy.annotation.RequireRole;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.entity.MovieRating;
import org.example.lyy.service.MovieRatingService;
import org.example.lyy.service.MovieService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sys/data")
@RequireRole({"SUPER_ADMIN"}) // 数据导入极为危险，仅限超级管理员
public class SysDataImportController {

    @Resource
    private MovieService movieService;
    @Resource
    private MovieRatingService movieRatingService;

    // TMDB API 的申请密钥 (实际开发中配置在 application.yml 里)
    private static final String TMDB_API_KEY = "你的TMDB_API_KEY"; 

    /**
     * 1. 导入 MovieLens 电影数据 (DT-01)
     * 文件格式假设: movieId,title,genres
     */
    @PostMapping("/import-movies")
    public Result<String> importMovies(@RequestParam("file") MultipartFile file) {
        try {
            CsvData data = CsvUtil.getReader().read(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            List<CsvRow> rows = data.getRows();
            List<Movie> movieList = new ArrayList<>();

            // 略过表头，从 i=1 开始
            for (int i = 1; i < rows.size(); i++) {
                CsvRow row = rows.get(i);
                Movie movie = new Movie();
                movie.setId(Long.parseLong(row.get(0)));
                movie.setTitle(row.get(1));
                // MovieLens 的 genres 通常是 Action|Adventure，我们替换为逗号
                movie.setGenres(row.get(2).replace("|", ",")); 
                movieList.add(movie);
                
                // 每 1000 条批量插入一次，防止内存溢出
                if (movieList.size() >= 1000) {
                    movieService.saveBatch(movieList);
                    movieList.clear();
                }
            }
            if (!movieList.isEmpty()) {
                movieService.saveBatch(movieList);
            }
            return Result.success("电影数据导入成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 2. 导入 MovieLens 评分数据 (DT-02)
     * 文件格式假设: userId,movieId,rating,timestamp
     */
    @PostMapping("/import-ratings")
    public Result<String> importRatings(@RequestParam("file") MultipartFile file) {
        try {
            CsvData data = CsvUtil.getReader().read(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            List<CsvRow> rows = data.getRows();
            List<MovieRating> ratingList = new ArrayList<>();

            for (int i = 1; i < rows.size(); i++) {
                CsvRow row = rows.get(i);
                MovieRating rating = new MovieRating();
                rating.setUserId(Long.parseLong(row.get(0)));
                rating.setMovieId(Long.parseLong(row.get(1)));
                rating.setRating(new BigDecimal(row.get(2)));
                
                // 将时间戳转换为 LocalDateTime
                long timestamp = Long.parseLong(row.get(3));
                rating.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
                
                ratingList.add(rating);

                if (ratingList.size() >= 2000) {
                    movieRatingService.saveBatch(ratingList);
                    ratingList.clear();
                }
            }
            if (!ratingList.isEmpty()) {
                movieRatingService.saveBatch(ratingList);
            }
            return Result.success("评分数据导入成功！");
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 3. 同步 TMDB 扩展信息 (DT-03)
     * 逻辑：找出数据库中还没有海报(poster_url 为空)的电影，去 TMDB 查详情并更新
     */
    @PostMapping("/sync-tmdb")
    public Result<String> syncTmdbInfo() {
        // 查找没有海报的电影（每次处理100条防止 API 限制）
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(Movie::getPosterUrl).last("LIMIT 100");
        List<Movie> pendingMovies = movieService.list(wrapper);

        int successCount = 0;
        for (Movie movie : pendingMovies) {
            try {
                // 第 1 步：使用 TMDB Search API 根据片名搜索电影 TMDB ID
                // (注意：MovieLens 的片名通常带有年份，如 "Toy Story (1995)"，实际使用中可能需要正则去掉年份)
                String searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=" + TMDB_API_KEY + "&query=" + movie.getTitle();
                String searchResp = HttpUtil.get(searchUrl);
                JSONObject searchJson = JSONUtil.parseObj(searchResp);
                
                if (searchJson.getJSONArray("results").size() > 0) {
                    // 取搜索结果的第一条
                    JSONObject firstMatch = searchJson.getJSONArray("results").getJSONObject(0);
                    
                    // 拼接海报 URL
                    String posterPath = firstMatch.getStr("poster_path");
                    if (posterPath != null) {
                        movie.setPosterUrl("https://image.tmdb.org/t/p/w500" + posterPath);
                    }
                    
                    // 获取简介
                    movie.setIntroduction(firstMatch.getStr("overview"));
                    
                    // 选做：可以继续用 TMDB ID 请求 /movie/{id}/credits 获取演员导演信息...
                    
                    movieService.updateById(movie);
                    successCount++;
                }
                
                // 睡眠 200 毫秒，防止被 TMDB API 限流封禁
                Thread.sleep(200);
            } catch (Exception e) {
                // 单条失败不影响整体进度
                e.printStackTrace();
            }
        }
        return Result.success("TMDB 同步执行完毕，成功更新 " + successCount + " 部电影信息");
    }
}