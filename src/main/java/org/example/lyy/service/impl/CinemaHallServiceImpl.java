package org.example.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.CinemaHall;
import org.example.lyy.mapper.CinemaHallMapper;
import org.example.lyy.service.CinemaHallService;
import org.springframework.stereotype.Service;

@Service
public class CinemaHallServiceImpl extends ServiceImpl<CinemaHallMapper, CinemaHall> implements CinemaHallService {
}