package org.example.lyy.controller.admin;

import org.example.lyy.annotation.RequireRole;
import org.example.lyy.common.Result;
import org.example.lyy.entity.CinemaHall;
import org.example.lyy.service.CinemaHallService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sys/hall")
@RequireRole({"SUPER_ADMIN", "MANAGER"})
public class SysCinemaHallController {

    @Resource
    private CinemaHallService cinemaHallService;

    // 1. 新增影厅
    @PostMapping("/add")
    public Result<String> addHall(@RequestBody CinemaHall hall) {
        // hall.getSeatLayout() 接收前端传来的 JSON 字符串，例如描述过道、损坏座位等
        cinemaHallService.save(hall);
        return Result.success("影厅添加成功");
    }

    // 2. 修改影厅 (比如修改座位布局)
    @PutMapping("/update")
    public Result<String> updateHall(@RequestBody CinemaHall hall) {
        cinemaHallService.updateById(hall);
        return Result.success("影厅修改成功");
    }

    // 3. 删除影厅
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteHall(@PathVariable Long id) {
        cinemaHallService.removeById(id);
        return Result.success("影厅删除成功");
    }

    // 4. 获取所有影厅列表 (排片时需要下拉选择影厅)
    @GetMapping("/list")
    public Result<List<CinemaHall>> listHalls() {
        return Result.success(cinemaHallService.list());
    }
}