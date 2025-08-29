package todoapp.application.boot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import todoapp.application.domain.Goal;
import todoapp.application.domain.Task;
import todoapp.application.repository.GoalRepository;
import todoapp.application.repository.TaskRepository;
import todoapp.application.service.search.SolrIndexService;

@Component
@Order(100)
public class StartupReindexer implements ApplicationRunner {
    
    private final GoalRepository goals;
    private final TaskRepository tasks;
    private final SolrIndexService solr;

    @Value("${app.search.reindex-on-startup:true}")
    boolean reindexOnStartup;

    public StartupReindexer(GoalRepository goals, TaskRepository tasks, SolrIndexService solr) {
        this.goals = goals;
        this.tasks = tasks;
        this.solr = solr;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments arg) throws Exception {
        if (!reindexOnStartup) return;

        int pageSize = 200;
        int pageNum = 0;
        Page<Goal> page;

        do {
            page = goals.findAll(PageRequest.of(pageNum, pageSize, Sort.by("id").ascending()));
            List<Goal> batch = page.getContent();

            List<String> idsToCheck = new ArrayList<>();
            Map<Long, List<Task>> tasksByGoal = new HashMap<>();
            
            for (Goal g : batch) {
                idsToCheck.add(solr.goalDocId(g.getId()));
                List<Task> ts = tasks.findByGoalIdOrderByIdAsc(g.getId());
                tasksByGoal.put(g.getId(), ts);
                for (Task t : ts) {
                    idsToCheck.add(solr.taskDocId(t.getId()));
                }
            }

            Set<String> existing = solr.fetchExistingIds(idsToCheck);

            Map<Goal, List<Task>> needIndex = batch.stream()
                .filter(g -> {
                    String gid = solr.goalDocId(g.getId());
                    if (!existing.contains(gid)) return true;
                    return tasksByGoal.get(g.getId()).stream()
                        .map(t -> solr.taskDocId(t.getId()))
                        .anyMatch(id -> !existing.contains(id));
                })
                .collect(Collectors.toMap(g -> g, g -> tasksByGoal.get(g.getId())));
            
            if (!needIndex.isEmpty()) {
                solr.indexGoalsWithTasksBulk(needIndex);
            }

            pageNum++;
        } while (page.hasNext());
    }
}
