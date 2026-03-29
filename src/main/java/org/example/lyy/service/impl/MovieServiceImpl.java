package org.example.lyy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.Movie;
import org.example.lyy.entity.MovieRating;
import org.example.lyy.mapper.MovieMapper;
import org.example.lyy.model.dto.MovieRatingDTO;
import org.example.lyy.service.MovieRatingService;
import org.example.lyy.service.MovieService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    @Resource
    private MovieRatingService movieRatingService;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public void rateMovie(Long userId, MovieRatingDTO dto) {
        // 1. 检查用户是否已经评价过
        LambdaQueryWrapper<MovieRating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MovieRating::getUserId, userId).eq(MovieRating::getMovieId, dto.getMovieId());
        if (movieRatingService.count(wrapper) > 0) {
            throw new RuntimeException("您已经评价过该电影，不能重复评价");
        }

        // 2. 插入评价记录
        MovieRating rating = new MovieRating();
        rating.setUserId(userId);
        rating.setMovieId(dto.getMovieId());
        rating.setRating(dto.getRating());
        rating.setComment(dto.getComment());
        movieRatingService.save(rating);

        // 3. 更新电影的全局平均分和评分人数 (使用悲观锁或直接 SQL 更新更安全，这里演示重新计算逻辑)
        Movie movie = this.getById(dto.getMovieId());
        int currentCount = movie.getRatingCount() == null ? 0 : movie.getRatingCount();
        BigDecimal currentRating = movie.getGlobalRating() == null ? BigDecimal.ZERO : movie.getGlobalRating();

        // 计算新的平均分：(旧平均分 * 旧人数 + 新评分) / (旧人数 + 1)
        BigDecimal totalScore = currentRating.multiply(new BigDecimal(currentCount)).add(dto.getRating());
        int newCount = currentCount + 1;
        BigDecimal newRating = totalScore.divide(new BigDecimal(newCount), 1, RoundingMode.HALF_UP); // 保留1位小数

        movie.setRatingCount(newCount);
        movie.setGlobalRating(newRating);
        this.updateById(movie);
    }
}