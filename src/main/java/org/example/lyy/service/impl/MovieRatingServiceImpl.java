package org.example.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.MovieRating;
import org.example.lyy.mapper.MovieRatingMapper;
import org.example.lyy.service.MovieRatingService;
import org.springframework.stereotype.Service;

@Service
public class MovieRatingServiceImpl extends ServiceImpl<MovieRatingMapper, MovieRating> implements MovieRatingService {
}