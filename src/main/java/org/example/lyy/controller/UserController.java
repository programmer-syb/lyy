package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.lyy.common.Result;
import org.example.lyy.entity.*;
import org.example.lyy.model.dto.LoginDTO;
import org.example.lyy.model.vo.LoginVO;
import org.example.lyy.model.vo.MovieHistoryVO;
import org.example.lyy.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MovieRatingService ratingService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private OrdersService orderService;

    // Redis 中存储 token 的 key 前缀
    private static final String LOGIN_USER_KEY = "login:user:token:";

    /**
     * 1. 用户注册 (UC-01)
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody LoginDTO loginDTO) {
        userService.register(loginDTO);
        return Result.success("注册成功");
    }

    /**
     * 2. 用户登录 (UC-02)
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 3. 退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        if (userId != null) {
            // 删除 Redis 中的 token，使当前 token 失效
            redisTemplate.delete(LOGIN_USER_KEY + userId);
        }
        return Result.success("退出登录成功");
    }

    /**
     * 4. 获取个人资料 (用于前端回显)
     */
    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = userService.getById(userId);
        user.setPassword(null); // 安全起见，不返回密码
        return Result.success(user);
    }

    /**
     * 5. 修改个人资料 (UC-03)
     */
    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        user.setId(userId); // 确保只能修改自己的信息
        userService.updateById(user);
        return Result.success("资料修改成功");
    }

    /**
     * 6. 修改偏好设置 (UC-04)
     */
    @PutMapping("/preferences")
    public Result<String> updatePreferences(@RequestParam String preferences, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = new User();
        user.setId(userId);
        user.setPreferences(preferences); // 传入 JSON 字符串，例如：'{"genres":["动作","科幻"],"directors":["诺兰"]}'
        userService.updateById(user);
        return Result.success("偏好设置更新成功");
    }

    /**
     * 7. 本地上传头像 (支持 UC-03)
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        try {
            // 获取项目根目录，并在其下创建 uploads/avatars 文件夹
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/avatars/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名，防止重名覆盖
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件到本地
            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            // 拼接可访问的本地 URL（需配合 WebMvcConfig 资源映射）
            String avatarUrl = "http://localhost:8080/uploads/avatars/" + newFilename;

            // 更新数据库中该用户的 avatar 字段
            Long userId = (Long) request.getAttribute("currentUserId");
            User user = new User();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            userService.updateById(user);

            return Result.success(avatarUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("头像上传失败");
        }
    }

    /**
     * 8. 获取观影历史记录 (UC-05)
     */
    @GetMapping("/history")
    public Result<List<MovieHistoryVO>> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        List<MovieHistoryVO> historyList = new ArrayList<>();

        // 1. 获取用户的评分记录
        // 实际开发中需要注入 MovieRatingService 和 MovieService 进行查询拼装
        LambdaQueryWrapper<MovieRating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(MovieRating::getUserId, userId).orderByDesc(MovieRating::getCreateTime).last("LIMIT 20");
        List<MovieRating> ratings = ratingService.list(ratingWrapper);
        for (MovieRating rating : ratings) {
            Movie movie = movieService.getById(rating.getMovieId());
            MovieHistoryVO vo = new MovieHistoryVO();
            vo.setMovieId(movie.getId());
            vo.setTitle(movie.getTitle());
            vo.setPosterUrl(movie.getPosterUrl());
            vo.setActionType("评分");
            vo.setActionTime(rating.getCreateTime());
            vo.setDetails(rating.getRating() + " 星");
            historyList.add(vo);
        }

        // 2. 获取用户的购票记录 (关联 Order 和 Schedule 表)
        // 2. 获取用户的购票记录 (关联 Order 和 Schedule 表)
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        // status: 1 表示已支付
        orderWrapper.eq(Orders::getUserId, userId).eq(Orders::getStatus, 1).orderByDesc(Orders::getCreateTime).last("LIMIT 20");
        List<Orders> orders = orderService.list(orderWrapper);
        for (Orders order : orders) {
            Schedule schedule = scheduleService.getById(order.getScheduleId());
            if (schedule != null) {
                Movie movie = movieService.getById(schedule.getMovieId());
                if (movie != null) {
                    MovieHistoryVO vo = new MovieHistoryVO();
                    vo.setMovieId(movie.getId());
                    vo.setTitle(movie.getTitle());
                    vo.setPosterUrl(movie.getPosterUrl());
                    vo.setActionType("购票");
                    vo.setActionTime(order.getCreateTime());
                    // 将排片时间和购买的座位作为详情展示
                    vo.setDetails(schedule.getShowDate() + " " + schedule.getShowTime() + " | 座位: " + order.getSeats());
                    historyList.add(vo);
                }
            }
        }

        // 3. 内存排序：按时间倒序
        historyList.sort((a, b) -> {
            // 如果时间为空，则默认给一个极早的时间作为兜底
            java.time.LocalDateTime timeA = a.getActionTime() != null ? a.getActionTime() : java.time.LocalDateTime.of(1970, 1, 1, 0, 0);
            java.time.LocalDateTime timeB = b.getActionTime() != null ? b.getActionTime() : java.time.LocalDateTime.of(1970, 1, 1, 0, 0);
            return timeB.compareTo(timeA);
        });

        return Result.success(historyList);
    }
}