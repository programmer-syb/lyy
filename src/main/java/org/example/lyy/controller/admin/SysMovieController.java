package org.example.lyy.controller.admin;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.lyy.annotation.RequireRole;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.service.MovieService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sys/movie")
@RequireRole({"SUPER_ADMIN", "MANAGER"})
public class SysMovieController {

    @Resource
    private MovieService movieService;

    // 1. 新增电影
    @PostMapping("/add")
    public Result<String> addMovie(@RequestBody Movie movie) {
        movieService.save(movie);
        return Result.success("电影添加成功");
    }

    // 2. 修改电影信息
    @PutMapping("/update")
    public Result<String> updateMovie(@RequestBody Movie movie) {
        movieService.updateById(movie);
        return Result.success("电影修改成功");
    }

    // 3. 逻辑删除电影
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteMovie(@PathVariable Long id) {
        // MyBatis Plus 配置了 is_deleted 字段的话，这里会自动转为 update is_deleted = 1
        movieService.removeById(id);
        return Result.success("电影删除成功");
    }

    // 4. 后台电影分页查询 (可按片名模糊搜索)
    @GetMapping("/page")
    public Result<Page<Movie>> pageMovies(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title) {
            
        Page<Movie> page = new Page<>(current, size);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            wrapper.like(Movie::getTitle, title);
        }
        wrapper.orderByDesc(Movie::getCreateTime);
        return Result.success(movieService.page(page, wrapper));
    }

    // 5. 管理端上传电影海报
    @PostMapping("/upload")
    public Result<String> uploadPoster(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("请选择要上传的图片");
        try {
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/posters/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            String posterUrl = "http://localhost:8080/uploads/posters/" + newFilename;
            return Result.success(posterUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("海报上传失败");
        }
    }
}