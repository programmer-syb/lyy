package org.example.lyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.lyy.entity.Orders;
import org.example.lyy.model.vo.BoxOfficeVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    /**
     * 统计指定时间范围内的电影票房排行 (取前10名)
     */
    @Select("SELECT m.id AS movieId, m.title, m.poster_url AS posterUrl, SUM(o.total_price) AS boxOffice " +
            "FROM orders o " +
            "JOIN schedule s ON o.schedule_id = s.id " +
            "JOIN movie m ON s.movie_id = m.id " +
            "WHERE o.status = 1 " + // 仅统计已支付的订单
            "AND (o.create_time IS NULL OR (o.create_time >= #{startTime} AND o.create_time <= #{endTime})) " +
            "GROUP BY m.id, m.title, m.poster_url " +
            "ORDER BY boxOffice DESC " +
            "LIMIT 10")
    List<BoxOfficeVO> getBoxOfficeRanking(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}