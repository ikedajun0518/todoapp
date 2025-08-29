package todoapp.application.service.search;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.HighlightParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import todoapp.application.domain.Goal;
import todoapp.application.domain.Task;

@Service
public class SolrIndexService {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexService.class);
    private final SolrClient solr;
    private final int defaultRows;
    private final String collection;

    public SolrIndexService(
        SolrClient solr, 
        @Value("${app.search.max-rows:50}") int defaultRows,
        @Value("${app.search.collection:todoapp}") String collection 
    ) {
        this.solr = solr;
        this.defaultRows = defaultRows;
        this.collection = collection;
    }

    private static final String HL_PRE = "[[[HL]]]";
    private static final String HL_POST = "[[[/HL]]]";

    public static final class TaskHit {
        public Long id;
        public String nameHtml;
        public Instant updatedAt;
    }

    public static final class GoalHit {
        public Long goalId;
        public String goalNameHtml;
        public List<TaskHit> tasks = new ArrayList<>();
    }

    public void indexGoal(Goal g) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", goalDocId(g.getId()));
        doc.addField("type", "goal");
        doc.addField("goal_id_l", g.getId());
        if (g.getName() != null) doc.addField("goal_name_ja", g.getName());
        doc.addField("updated_at_dt", toDate(g.getUpdatedAt()));
        addAndCommit(doc);
    }

    public void indexTask(Task t) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", taskDocId(t.getId()));
        doc.addField("type", "task");
        doc.addField("goal_id_l", t.getGoal().getId());
        doc.addField("task_name_ja", t.getName());
        doc.addField("updated_at_dt", toDate(t.getUpdatedAt()));
        addAndCommit(doc);
    }

    public void indexGoalWithTasks(Goal g, List<Task> tasks) {
        List<SolrInputDocument> docs = new ArrayList<>();
        docs.add(toGoalDoc(g));
        for (Task t : tasks) docs.add(toTaskDoc(t));
        addAndCommit(docs);
    }

    public void deleteByGoalId(long goalId) {
        deleteByQuery("goal_id_l:" + goalId);
    }

    public void deleteTask(long taskId) {
        deleteById(taskDocId(taskId));
    }

    public List<Long>searchGoalsIds(String keyword, Integer rows) {
        try {
            SolrQuery q = new SolrQuery();
            q.setRequestHandler("/select");
            q.set("defType", "edismax");
            q.setQuery((keyword == null || keyword.isBlank()) ? "*:*" : keyword);
            q.set("qf", "goal_name_ja^3 task_name_ja");
            q.setRows(rows != null ? rows : defaultRows);
            q.addSort("updated_at_dt", SolrQuery.ORDER.desc);

            QueryResponse rsp = solr.query(collection, q);
            return rsp.getResults().stream()
                .map(doc -> (Long) doc.getFieldValue("goal_id_l"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr search failed", e);
        }
    }

    private void addAndCommit(SolrInputDocument doc) {
        try {
            solr.add(collection, doc);
            solr.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr index failed", e);
        }
    }

    private void addAndCommit(List<SolrInputDocument> docs) {
        if (docs == null || docs.isEmpty()) return;
        try {
            solr.add(collection, docs);
            solr.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr bulk index failed", e);
        }
    }

    private void deleteById(String id) {
        try {
            solr.deleteById(collection, id);
            solr.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr deleteById failed", e);
        }
    }

    private void deleteByQuery(String q) {
        try {
            solr.deleteByQuery(collection, q);
            solr.commit(collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr deleteByQuery failed", e);
        }
    }

    private Date toDate(Instant inst) {
        if (inst == null) return new Date();
        return Date.from(inst);
    }

    public String goalDocId(Long id) { return "goal:" + id; }
    public String taskDocId(Long id) { return "task:" + id; }

    public Set<String> fetchExistingIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        List<String> list = new ArrayList<>(ids);
        final int BATCH = 500;

        for (int i = 0; i < list.size(); i += BATCH) {
            List<String> sub = list.subList(i, Math.min(i + BATCH, list.size()));
            try {
                SolrDocumentList docs = solr.getById(collection, sub);
                for (SolrDocument d : docs) {
                    Object v = d.getFieldValue("id");
                    if (v != null) out.add(v.toString());
                }
            } catch (Exception e) {
                log.warn("Solr getById failed (skip this batch): {}", e.getMessage());
            }
        }
        return out;
    }

    private SolrInputDocument toGoalDoc(Goal g) {
        SolrInputDocument d = new SolrInputDocument();
        d.addField("id", goalDocId(g.getId()));
        d.addField("type", "goal");
        d.addField("goal_id_l", g.getId());
        d.addField("goal_name_ja", safe(g.getName()));
        d.addField("updated_at_dt", toDate(g.getUpdatedAt()));
        return d;
    }
    private SolrInputDocument toTaskDoc(Task t) {
        SolrInputDocument d = new SolrInputDocument();
        d.addField("id", taskDocId(t.getId()));
        d.addField("type", "task");
        d.addField("goal_id_l", t.getGoal().getId());
        d.addField("task_name_ja", safe(t.getName()));
        d.addField("updated_at_dt", toDate(t.getUpdatedAt()));
        return d;
    }
    private static String safe(String s){ return s == null ? "" : s; }

    public void indexGoalsWithTasksBulk(Map<Goal, List<Task>> batch) {
        if (batch == null || batch.isEmpty()) return; 
        List<SolrInputDocument> docs = new ArrayList<>();
        for (var e : batch.entrySet()) {
            docs.add(toGoalDoc(e.getKey()));
            for (Task t : e.getValue()) docs.add(toTaskDoc(t));
        }
        addAndCommit(docs);
    }

    public Map<Long, GoalHit> searchGoalTaskHits(String keyword, int rows) {
        try {
            SolrQuery q = new SolrQuery();
            q.setRequestHandler("/select");
            q.set("defType", "edismax");
            q.setQuery((keyword == null || keyword.isBlank()) ? "*:*" : keyword);
            q.set("qf", "goal_name_ja^3 task_name_ja");
            q.setRows(rows > 0 ? rows : defaultRows);
            q.addSort("updated_at_dt", SolrQuery.ORDER.desc);

            // 返却フィールドとハイライト設定
            q.setFields("id", "type", "goal_id_l", "updated_at_dt", "goal_name_ja", "task_name_ja");
            q.setParam(HighlightParams.HIGHLIGHT, "true");
            q.setParam(HighlightParams.FIELDS, "goal_name_ja,task_name_ja");
            q.setParam(HighlightParams.SIMPLE_PRE, HL_PRE);
            q.setParam(HighlightParams.SIMPLE_POST, HL_POST);

            QueryResponse rsp = solr.query(collection, q);
            var docs = rsp.getResults();
            Map<String, Map<String, List<String>>> hl = rsp.getHighlighting();

            Map<Long, GoalHit> out = new LinkedHashMap<>();
            for (var doc : docs) {
                String id = String.valueOf(doc.getFieldValue("id"));
                String type = String.valueOf(doc.getFieldValue("type"));
                Long goalId = (Long) doc.getFieldValue("goal_id_l");
                Instant updated = ((Date) doc.getFieldValue("updated_at_dt")).toInstant();

                var fields = (hl != null) ? hl.get(id) : null;

                out.computeIfAbsent(goalId, k -> {
                    GoalHit gh = new GoalHit();
                    gh.goalId = k;
                    return gh;
                });
                GoalHit gh = out.get(goalId);

                if ("goal".equals(type)) {
                    String snippet = pickFirst(fields, "goal_name_ja");
                    if (snippet != null) {
                        gh.goalNameHtml = toMarkedHtml(snippet);
                    } else {
                        Object raw = doc.getFieldValue("goal_name_ja");
                        gh.goalNameHtml = (raw != null) ? HtmlUtils.htmlEscape(raw.toString()) : "";
                    }
                } else if ("task".equals(type)) {
                    String snippet = pickFirst(fields, "task_name_ja");
                    TaskHit th = new TaskHit();
                    th.id = parseDocIdForTask(id);
                    th.nameHtml = (snippet != null)
                     ? toMarkedHtml(snippet)
                     : HtmlUtils.htmlEscape(Objects.toString(doc.getFieldValue("task_name_ja"), ""));
                    th.updatedAt = updated;
                    gh.tasks.add(th);
                }
            }
            return out;
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Solr search failed", e);
        }
    }

    private static String pickFirst(Map<String, List<String>> fields, String key) {
        if (fields == null) return null;
        var list = fields.get(key);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    private static Long parseDocIdForTask(String id) {
        // "task:123" → 123
        if (id != null && id.startsWith("task:")) {
            try { return Long.valueOf(id.substring("task:".length())); } catch (Exception ignore) {}
        }
        return null;
    }

    // Solrのハイライトマーカー → HTML <mark class="hl"> に変換（ついでにエスケープ）
    private static String toMarkedHtml(String snippet) {
        if (snippet == null) return null;
        String esc = HtmlUtils.htmlEscape(snippet);
        return esc.replace(HL_PRE, "<mark class=\"hl\">")
                .replace(HL_POST, "</mark>");
    }
}
