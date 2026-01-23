package com.gitranker.api.batch.strategy;

import com.gitranker.api.domain.user.User;
import com.gitranker.api.domain.user.vo.ActivityStatistics;

public interface ActivityUpdateStrategy {

    ActivityStatistics update(User user, ActivityUpdateContext context);

}
