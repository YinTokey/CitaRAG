
package com.yin.cita.model;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;

public class ChunkMetadata {

    @Description("A concise 1-2 sentence summary of the chunk's core insight.")
    private String summary;

    @Description("A list of 5-7 key topics, entities, or concepts mentioned in the chunk.")
    private List<String> keywords;

    @Description("A list of 3-5 hypothetical questions that this chunk specifically answers.")
    private List<String> hypotheticalQuestions;

    @Description("If the chunk is a table, a natural language summary of its key trends. Null if not a table.")
    private String tableSummary;

    @Description("The page number(s) where this chunk is located in the original document (e.g., '5', '5-6').")
    private String pageNumber;

    @Description("The hierarchical section header context for this chunk (e.g., '1. Business > 1.1 Risks').")
    private String sectionHeader;

    @Description("A list of key numerical statistics or data points extracted from the chunk.")
    private List<String> keyStatistics;

    public ChunkMetadata() {
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getHypotheticalQuestions() {
        return hypotheticalQuestions;
    }

    public void setHypotheticalQuestions(List<String> hypotheticalQuestions) {
        this.hypotheticalQuestions = hypotheticalQuestions;
    }

    public String getTableSummary() {
        return tableSummary;
    }

    public void setTableSummary(String tableSummary) {
        this.tableSummary = tableSummary;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getSectionHeader() {
        return sectionHeader;
    }

    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
    }

    public List<String> getKeyStatistics() {
        return keyStatistics;
    }

    public void setKeyStatistics(List<String> keyStatistics) {
        this.keyStatistics = keyStatistics;
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (summary != null)
            map.put("summary", summary);
        if (keywords != null)
            map.put("keywords", keywords);
        if (hypotheticalQuestions != null)
            map.put("hypothetical_questions", hypotheticalQuestions);
        if (tableSummary != null)
            map.put("table_summary", tableSummary);
        if (pageNumber != null)
            map.put("page_number", pageNumber); // Ensure override or merge happens correctly upstream
        if (sectionHeader != null)
            map.put("section_header", sectionHeader);
        if (keyStatistics != null)
            map.put("key_statistics", keyStatistics);
        return map;
    }
}
