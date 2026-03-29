package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.model.dto.MovieQueryDTO;
import org.example.lyy.model.dto.MovieRatingDTO;
import org.example.lyy.service.MovieService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/movie")
public class MovieController {

    @Resource
    private MovieService movieService;

    /**
     * 1. 电影库浏览 (MV-01) - 分页与多条件查询
     */
    @PostMapping("/page")
    public Result<Page<Movie>> getMoviePage(@RequestBody MovieQueryDTO queryDTO) {
        Page<Movie> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();

        // 关键字模糊查询(片名)
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Movie::getTitle, queryDTO.getKeyword());
        }
        // 按类型筛选(假设 genres 字段是用逗号分隔的字符串，比如 "动作,科幻")
        if (StringUtils.hasText(queryDTO.getGenre())) {
            wrapper.like(Movie::getGenres, queryDTO.getGenre());
        }

        // 动态排序
        if ("rating".equals(queryDTO.getSortBy())) {
            wrapper.orderByDesc(Movie::getGlobalRating); // 按评分倒序
        } else if ("heat".equals(queryDTO.getSortBy())) {
            wrapper.orderByDesc(Movie::getHeat); // 按热度倒序
        } else {
            wrapper.orderByDesc(Movie::getReleaseDate); // 默认按上映时间倒序
        }

        Page<Movie> resultPage = movieService.page(page, wrapper);
        return Result.success(resultPage);
    }

    /**
     * 2. 电影详情查询 (MV-02)
     */
    @GetMapping("/{id}")
    public Result<Movie> getMovieDetail(@PathVariable Long id) {
        Movie movie = movieService.getById(id);
        if (movie == null) {
            return Result.error("电影不存在");
        }
        return Result.success(movie);
    }

    /**
     * 3. 观众评价与评分 (MV-03)
     */
    @PostMapping("/rate")
    public Result<String> rateMovie(@RequestBody MovieRatingDTO ratingDTO, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        movieService.rateMovie(userId, ratingDTO);
        return Result.success("评价成功");
    }
}