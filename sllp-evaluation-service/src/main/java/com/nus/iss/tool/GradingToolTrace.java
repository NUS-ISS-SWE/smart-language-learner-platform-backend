package com.nus.iss.tool;

import com.nus.iss.dto.AnchorPaperResponse;

import java.util.List;

public class GradingToolTrace {
    private volatile boolean anchorToolCalled;
    private volatile List<AnchorPaperResponse> anchorPapers = List.of();

    public void recordAnchorPapers(List<AnchorPaperResponse> papers) {
        anchorToolCalled = true;
        anchorPapers = List.copyOf(papers);
    }

    public boolean wasAnchorToolCalled() {
        return anchorToolCalled;
    }

    public List<AnchorPaperResponse> getAnchorPapers() {
        return anchorPapers;
    }
}
