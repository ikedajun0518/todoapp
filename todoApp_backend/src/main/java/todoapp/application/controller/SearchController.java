package todoapp.application.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import todoapp.application.domain.Goal;
import todoapp.application.repository.GoalRepository;
import todoapp.application.service.search.SolrIndexService;

@RestController
public class SearchController {

    private final GoalRepository goals;
    private final SolrIndexService solr;

    public SearchController(GoalRepository goals, SolrIndexService solr) {
        this.goals = goals;
        this.solr = solr;
    }

    // /api/search/goals?q=キーワード
    @GetMapping("/api/search/goals")
    public List<ResultDto> search(@RequestParam(name="q", required=false) String q) {
        if (q == null || q.trim().isEmpty()) {
            // 空ならフロント側が通常一覧取得にフォールバックするため、空配列返却
            return List.of();
        }
        Map<Long, SolrIndexService.GoalHit> hits = solr.searchGoalTaskHits(q.trim(), 2000);

        // ゴールをDBから取得（更新日時で並べたいのでDB基準にソート）
        List<Goal> goalList = new ArrayList<>(goals.findAllById(hits.keySet()));
        goalList.sort(Comparator.comparing(Goal::getUpdatedAt).reversed());

        List<ResultDto> out = new ArrayList<>();
        for (Goal g : goalList) {
            var gh = hits.get(g.getId());

            ResultDto dto = new ResultDto();
            dto.id = g.getId();
            dto.updatedAt = g.getUpdatedAt();
            // 目標名: ヒットがあればハイライトHTML、なければプレーン
            dto.nameHtml = (gh != null && gh.goalNameHtml != null) ? gh.goalNameHtml : escape(g.getName());

            // タスクリスト（ヒットしたもののみ）
            if (gh != null && gh.tasks != null && !gh.tasks.isEmpty()) {
                for (var th : gh.tasks) {
                    TaskDto td = new TaskDto();
                    td.id = th.id;
                    td.nameHtml = th.nameHtml;
                    td.updatedAt = th.updatedAt;
                    dto.tasks.add(td);
                }
            }
            out.add(dto);
        }
        return out;
    }

    private static String escape(String s) {
        // 目標名をそのまま返すときも安全のため最小限エスケープ
        return s == null ? "" : s
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // --- DTOs ---
    public static class TaskDto {
        public Long id;
        public String nameHtml;
        public Instant updatedAt;
    }
    public static class ResultDto {
        public Long id;
        public String nameHtml;  // v-html で描画
        public Instant updatedAt;
        public List<TaskDto> tasks = new ArrayList<>();
    }
}
