package org.example.lyy.model.dto;

import lombok.Data;

@Data
public class MovieQueryDTO {
    private Integer current = 1; // 当前页
    private Integer size = 10;   // 每页条数
    private String keyword;      // 搜索关键字(片名)
    private String genre;        // 类型筛选
    // 需求文档提到按地区、年代筛选，如果数据库表暂无这两列，可在此预留字段，后续修改表结构加上
    private String region;       
    private String year;         
    private String sortBy;       // 排序字段："rating"(评分) 或 "heat"(热度)
}