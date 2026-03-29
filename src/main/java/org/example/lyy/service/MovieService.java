package org.example.lyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.lyy.entity.Movie;
import org.example.lyy.model.dto.MovieRatingDTO;

public interface MovieService extends IService<Movie> {
    void rateMovie(Long userId, MovieRatingDTO dto);
}