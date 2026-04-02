package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.entity.MovieRating;
import org.example.lyy.entity.User;
import org.example.lyy.service.MovieRatingService;
import org.example.lyy.service.MovieService;
import org.example.lyy.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    @Resource
    private MovieService movieService;

    @Resource
    private MovieRatingService ratingService;

    @Resource
    private UserService userService;

    private static final List<String> DEFAULT_RECOMMEND_GENRES = Arrays.asList("科幻", "动作", "剧情");

    private static final int LATENT_DIM = 16;
    private static final int TOP_N = 12;

    private volatile ModelBundle svdBundle;
    private volatile ModelBundle alsBundle;

    @GetMapping("/hot")
    public Result<List<Movie>> getHotRecommendations(@RequestParam(defaultValue = "10") Integer limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Movie::getHeat)
                .orderByDesc(Movie::getRatingCount)
                .last("LIMIT " + limit);
        return Result.success(movieService.list(wrapper));
    }

    @GetMapping("/svd")
    public Result<List<Movie>> getSvdRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        TrainData data = buildTrainData();
        if (data.ratings.isEmpty()) {
            return Result.success(coldStartByPreference(currentUser, 10));
        }

        ModelBundle local = getOrTrainSvdModel(data);
        List<Long> recMovieIds = recommendByModel(local, data, userId, TOP_N, currentUser);
        if (recMovieIds.isEmpty()) {
            return Result.success(coldStartByPreference(currentUser, 10));
        }

        return Result.success(sortMoviesByIdOrder(recMovieIds));
    }

    @GetMapping("/als")
    public Result<List<Movie>> getAlsRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        TrainData data = buildTrainData();
        if (data.ratings.isEmpty()) {
            return Result.success(coldStartByPreference(currentUser, 10));
        }

        ModelBundle local = getOrTrainAlsModel(data);
        List<Long> recMovieIds = recommendByModel(local, data, userId, TOP_N, currentUser);
        if (recMovieIds.isEmpty()) {
            return Result.success(coldStartByPreference(currentUser, 10));
        }

        return Result.success(sortMoviesByIdOrder(recMovieIds));
    }

    @GetMapping("/online/similar")
    public Result<List<Movie>> getOnlineSimilarMovies(
            @RequestParam Long targetMovieId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        Movie targetMovie = movieService.getById(targetMovieId);
        if (targetMovie == null || !StringUtils.hasText(targetMovie.getGenres())) {
            return Result.success(Collections.emptyList());
        }

        LambdaQueryWrapper<MovieRating> ratingWrapper = new LambdaQueryWrapper<>();
        ratingWrapper.eq(MovieRating::getUserId, userId).select(MovieRating::getMovieId);
        List<Long> watchedMovieIds = ratingService.list(ratingWrapper).stream()
                .map(MovieRating::getMovieId)
                .collect(Collectors.toList());

        String[] genres = targetMovie.getGenres().split(",");
        LambdaQueryWrapper<Movie> similarWrapper = new LambdaQueryWrapper<>();
        similarWrapper.and(w -> {
            for (int i = 0; i < genres.length; i++) {
                if (i == 0) w.like(Movie::getGenres, genres[i].trim());
                else w.or().like(Movie::getGenres, genres[i].trim());
            }
        });

        watchedMovieIds.add(targetMovieId);
        similarWrapper.notIn(Movie::getId, watchedMovieIds)
                .orderByDesc(Movie::getGlobalRating)
                .last("LIMIT 8");

        List<Movie> similarMovies = movieService.list(similarWrapper);
        return Result.success(filterPreferredMovies(similarMovies, currentUser));
    }

    @GetMapping("/cold-start")
    public Result<List<Movie>> getColdStartRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User currentUser = userService.getById(userId);
        return Result.success(coldStartByPreference(currentUser, 10));
    }

    private TrainData buildTrainData() {
        List<MovieRating> allRatings = ratingService.list();
        Set<Long> userIds = allRatings.stream().map(MovieRating::getUserId).collect(Collectors.toSet());
        Set<Long> movieIds = allRatings.stream().map(MovieRating::getMovieId).collect(Collectors.toSet());

        Map<Long, Integer> userIndex = new HashMap<>();
        Map<Integer, Long> indexUser = new HashMap<>();
        int u = 0;
        for (Long userId : userIds) {
            userIndex.put(userId, u);
            indexUser.put(u, userId);
            u++;
        }

        Map<Long, Integer> movieIndex = new HashMap<>();
        Map<Integer, Long> indexMovie = new HashMap<>();
        int i = 0;
        for (Long movieId : movieIds) {
            movieIndex.put(movieId, i);
            indexMovie.put(i, movieId);
            i++;
        }

        List<RatingTriple> triples = new ArrayList<>(allRatings.size());
        for (MovieRating r : allRatings) {
            Integer ui = userIndex.get(r.getUserId());
            Integer mi = movieIndex.get(r.getMovieId());
            if (ui != null && mi != null && r.getRating() != null) {
                triples.add(new RatingTriple(ui, mi, r.getRating().doubleValue()));
            }
        }

        return new TrainData(triples, userIndex, indexUser, movieIndex, indexMovie);
    }

    private synchronized ModelBundle getOrTrainSvdModel(TrainData data) {
        if (isModelValid(svdBundle, data)) {
            return svdBundle;
        }
        svdBundle = trainSvdSgd(data, LATENT_DIM, 35, 0.01, 0.04);
        return svdBundle;
    }

    private synchronized ModelBundle getOrTrainAlsModel(TrainData data) {
        if (isModelValid(alsBundle, data)) {
            return alsBundle;
        }
        alsBundle = trainAls(data, LATENT_DIM, 12, 0.2);
        return alsBundle;
    }

    private boolean isModelValid(ModelBundle bundle, TrainData data) {
        if (bundle == null) return false;
        if (System.currentTimeMillis() - bundle.trainTimestamp > 5 * 60 * 1000L) return false;
        return bundle.ratingSize == data.ratings.size()
                && bundle.userSize == data.userIndex.size()
                && bundle.movieSize == data.movieIndex.size();
    }

    private ModelBundle trainSvdSgd(TrainData data, int k, int epochs, double lr, double reg) {
        int userCount = data.userIndex.size();
        int movieCount = data.movieIndex.size();
        double[][] p = randomMatrix(userCount, k);
        double[][] q = randomMatrix(movieCount, k);

        double globalMean = data.ratings.stream().mapToDouble(r -> r.rating).average().orElse(3.0);
        double[] bu = new double[userCount];
        double[] bi = new double[movieCount];

        List<RatingTriple> samples = new ArrayList<>(data.ratings);
        for (int e = 0; e < epochs; e++) {
            Collections.shuffle(samples);
            for (RatingTriple t : samples) {
                int u = t.userIdx;
                int i = t.movieIdx;
                double pred = globalMean + bu[u] + bi[i] + dot(p[u], q[i]);
                double err = t.rating - pred;

                bu[u] += lr * (err - reg * bu[u]);
                bi[i] += lr * (err - reg * bi[i]);

                for (int f = 0; f < k; f++) {
                    double pu = p[u][f];
                    double qi = q[i][f];
                    p[u][f] += lr * (err * qi - reg * pu);
                    q[i][f] += lr * (err * pu - reg * qi);
                }
            }
        }
        return new ModelBundle(p, q, bu, bi, globalMean, data);
    }

    private ModelBundle trainAls(TrainData data, int k, int epochs, double lambda) {
        int userCount = data.userIndex.size();
        int movieCount = data.movieIndex.size();
        double[][] uFactor = randomMatrix(userCount, k);
        double[][] iFactor = randomMatrix(movieCount, k);

        Map<Integer, List<RatingTriple>> byUser = data.ratings.stream().collect(Collectors.groupingBy(r -> r.userIdx));
        Map<Integer, List<RatingTriple>> byMovie = data.ratings.stream().collect(Collectors.groupingBy(r -> r.movieIdx));

        for (int e = 0; e < epochs; e++) {
            for (int u = 0; u < userCount; u++) {
                List<RatingTriple> rows = byUser.get(u);
                if (rows == null || rows.isEmpty()) continue;
                uFactor[u] = solveNormalEquation(rows, iFactor, k, lambda);
            }
            for (int i = 0; i < movieCount; i++) {
                List<RatingTriple> rows = byMovie.get(i);
                if (rows == null || rows.isEmpty()) continue;
                iFactor[i] = solveNormalEquationForMovie(rows, uFactor, k, lambda);
            }
        }

        double globalMean = data.ratings.stream().mapToDouble(r -> r.rating).average().orElse(3.0);
        return new ModelBundle(uFactor, iFactor, new double[userCount], new double[movieCount], globalMean, data);
    }

    private double[] solveNormalEquation(List<RatingTriple> rows, double[][] itemFactor, int k, double lambda) {
        double[][] a = new double[k][k];
        double[] b = new double[k];

        for (RatingTriple t : rows) {
            double[] v = itemFactor[t.movieIdx];
            for (int x = 0; x < k; x++) {
                b[x] += t.rating * v[x];
                for (int y = 0; y < k; y++) {
                    a[x][y] += v[x] * v[y];
                }
            }
        }
        for (int d = 0; d < k; d++) {
            a[d][d] += lambda * rows.size();
        }
        return gaussianSolve(a, b);
    }

    private double[] solveNormalEquationForMovie(List<RatingTriple> rows, double[][] userFactor, int k, double lambda) {
        double[][] a = new double[k][k];
        double[] b = new double[k];

        for (RatingTriple t : rows) {
            double[] u = userFactor[t.userIdx];
            for (int x = 0; x < k; x++) {
                b[x] += t.rating * u[x];
                for (int y = 0; y < k; y++) {
                    a[x][y] += u[x] * u[y];
                }
            }
        }
        for (int d = 0; d < k; d++) {
            a[d][d] += lambda * rows.size();
        }
        return gaussianSolve(a, b);
    }

    private double[] gaussianSolve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }

        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(m[pivot][col]) < 1e-10) {
                m[pivot][col] = 1e-10;
            }
            if (pivot != col) {
                double[] tmp = m[pivot];
                m[pivot] = m[col];
                m[col] = tmp;
            }

            double div = m[col][col];
            for (int j = col; j <= n; j++) {
                m[col][j] /= div;
            }

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = m[row][col];
                for (int j = col; j <= n; j++) {
                    m[row][j] -= factor * m[col][j];
                }
            }
        }

        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = m[i][n];
        }
        return x;
    }

    private List<Long> recommendByModel(ModelBundle model, TrainData data, Long userId, int topN, User currentUser) {
        Integer uid = data.userIndex.get(userId);
        if (uid == null) {
            return Collections.emptyList();
        }

        Set<Integer> ratedMovieIdx = data.ratings.stream()
                .filter(r -> r.userIdx == uid)
                .map(r -> r.movieIdx)
                .collect(Collectors.toSet());

        Map<Long, Double> scoreMap = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : data.indexMovie.entrySet()) {
            Integer midx = entry.getKey();
            Long movieId = entry.getValue();
            if (ratedMovieIdx.contains(midx)) continue;

            double score = model.globalMean + model.bu[uid] + model.bi[midx] + dot(model.userFactors[uid], model.itemFactors[midx]);
            Movie movie = movieService.getById(movieId);
            score += calcGenreWeight(movie != null ? movie.getGenres() : null, currentUser);
            scoreMap.put(movieId, score);
        }

        return scoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<Movie> sortMoviesByIdOrder(List<Long> ids) {
        List<Movie> movies = movieService.listByIds(ids);
        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            rank.put(ids.get(i), i);
        }
        movies.sort(Comparator.comparingInt(m -> rank.getOrDefault(m.getId(), Integer.MAX_VALUE)));
        return movies;
    }

    private List<Movie> coldStartByPreference(User currentUser, int limit) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        List<String> preferred = getUserPreferredGenres(currentUser);

        if (!preferred.isEmpty()) {
            wrapper.and(w -> {
                for (int i = 0; i < preferred.size(); i++) {
                    if (i == 0) w.like(Movie::getGenres, preferred.get(i));
                    else w.or().like(Movie::getGenres, preferred.get(i));
                }
            });
        } else {
            wrapper.and(w -> {
                for (int i = 0; i < DEFAULT_RECOMMEND_GENRES.size(); i++) {
                    if (i == 0) w.like(Movie::getGenres, DEFAULT_RECOMMEND_GENRES.get(i));
                    else w.or().like(Movie::getGenres, DEFAULT_RECOMMEND_GENRES.get(i));
                }
            });
        }
        wrapper.orderByDesc(Movie::getHeat).last("LIMIT " + limit);

        List<Movie> list = movieService.list(wrapper);
        if (!list.isEmpty()) return list;

        LambdaQueryWrapper<Movie> fallback = new LambdaQueryWrapper<>();
        fallback.orderByDesc(Movie::getHeat).last("LIMIT " + limit);
        return movieService.list(fallback);
    }

    private List<Movie> filterPreferredMovies(List<Movie> movies, User user) {
        List<String> preferred = getUserPreferredGenres(user);
        if (preferred.isEmpty()) return movies;

        List<Movie> preferredFirst = new ArrayList<>();
        List<Movie> others = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie == null || !StringUtils.hasText(movie.getGenres())) {
                others.add(movie);
                continue;
            }

            boolean hit = preferred.stream().anyMatch(g -> movie.getGenres().contains(g.trim()));
            if (hit) preferredFirst.add(movie);
            else others.add(movie);
        }
        preferredFirst.addAll(others);
        return preferredFirst;
    }

    private double calcGenreWeight(String genres, User user) {
        if (!StringUtils.hasText(genres)) return 0;
        List<String> preferred = getUserPreferredGenres(user);
        if (preferred.isEmpty()) return 0;

        double weight = 0;
        for (String g : preferred) {
            if (genres.contains(g.trim())) {
                weight += 0.15;
            }
        }
        return weight;
    }

    private List<String> getUserPreferredGenres(User user) {
        if (user == null || !StringUtils.hasText(user.getPreferences())) {
            return Collections.emptyList();
        }
        return Arrays.stream(user.getPreferences().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private double[][] randomMatrix(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                matrix[r][c] = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
            }
        }
        return matrix;
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static class RatingTriple {
        private final int userIdx;
        private final int movieIdx;
        private final double rating;

        private RatingTriple(int userIdx, int movieIdx, double rating) {
            this.userIdx = userIdx;
            this.movieIdx = movieIdx;
            this.rating = rating;
        }
    }

    private static class TrainData {
        private final List<RatingTriple> ratings;
        private final Map<Long, Integer> userIndex;
        private final Map<Integer, Long> indexUser;
        private final Map<Long, Integer> movieIndex;
        private final Map<Integer, Long> indexMovie;

        private TrainData(List<RatingTriple> ratings,
                          Map<Long, Integer> userIndex,
                          Map<Integer, Long> indexUser,
                          Map<Long, Integer> movieIndex,
                          Map<Integer, Long> indexMovie) {
            this.ratings = ratings;
            this.userIndex = userIndex;
            this.indexUser = indexUser;
            this.movieIndex = movieIndex;
            this.indexMovie = indexMovie;
        }
    }

    private static class ModelBundle {
        private final double[][] userFactors;
        private final double[][] itemFactors;
        private final double[] bu;
        private final double[] bi;
        private final double globalMean;
        private final long trainTimestamp;
        private final int ratingSize;
        private final int userSize;
        private final int movieSize;

        private ModelBundle(double[][] userFactors,
                            double[][] itemFactors,
                            double[] bu,
                            double[] bi,
                            double globalMean,
                            TrainData data) {
            this.userFactors = userFactors;
            this.itemFactors = itemFactors;
            this.bu = bu;
            this.bi = bi;
            this.globalMean = globalMean;
            this.trainTimestamp = System.currentTimeMillis();
            this.ratingSize = data.ratings.size();
            this.userSize = data.userIndex.size();
            this.movieSize = data.movieIndex.size();
        }
    }
}
