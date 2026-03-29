package org.example.lyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.lyy.entity.Schedule;
import org.example.lyy.model.vo.ScheduleVO;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
    /**
     * 根据电影ID和日期查询排片场次，并关联影厅名称
     */
    @Select("SELECT s.id AS scheduleId, s.movie_id, s.hall_id, s.show_date, s.show_time, s.price, h.name AS hallName " +
            "FROM schedule s " +
            "LEFT JOIN cinema_hall h ON s.hall_id = h.id " +
            "WHERE s.movie_id = #{movieId} AND s.show_date = #{showDate} AND s.is_deleted = 0")
    List<ScheduleVO> selectScheduleWithHall(@Param("movieId") Long movieId, @Param("showDate") LocalDate showDate);
}