package com.team.tasktracker.task;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.List;

public final class TaskSpecification {

    private TaskSpecification() {}

    public static Specification<Task> filterTasks(
            List<Long> accessibleProjectIds,
            Long projectId,
            Long assigneeId,
            TaskStatus status,
            String search
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (accessibleProjectIds.isEmpty()) {
                predicate.getExpressions().add(cb.disjunction());
                return predicate;
            }
            predicate.getExpressions().add(root.get("project").get("id").in(accessibleProjectIds));

            if (projectId != null) {
                predicate.getExpressions().add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (assigneeId != null) {
                predicate.getExpressions().add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (status != null) {
                predicate.getExpressions().add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                var titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                var descMatch = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern);
                predicate.getExpressions().add(cb.or(titleMatch, descMatch));
            }
            return predicate;
        };
    }
}
