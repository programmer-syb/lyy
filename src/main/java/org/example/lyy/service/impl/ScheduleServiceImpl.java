package org.example.lyy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.Schedule;
import org.example.lyy.mapper.ScheduleMapper;
import org.example.lyy.service.ScheduleService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {
}