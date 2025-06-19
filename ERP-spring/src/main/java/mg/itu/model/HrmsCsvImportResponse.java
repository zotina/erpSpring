package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HrmsCsvImportResponse {
    private boolean success;
    private String message;
    private List<Map<String, Object>> validation_errors;
    private Map<String, List<Object>> inserted_records;
    private String status;

    
    @JsonProperty("message")
    private void unpackNestedMessage(Object nestedMessage) {
        if (nestedMessage instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) nestedMessage;
            this.success = (Boolean) map.getOrDefault("success", false);
            this.message = (String) map.getOrDefault("message", "Unknown error");
            this.validation_errors = (List<Map<String, Object>>) map.get("validation_errors");
            this.inserted_records = (Map<String, List<Object>>) map.get("inserted_records");
        } else if (nestedMessage instanceof String) {
            this.message = (String) nestedMessage;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Map<String, Object>> getValidation_errors() {
        return validation_errors;
    }

    public void setValidation_errors(List<Map<String, Object>> validation_errors) {
        this.validation_errors = validation_errors;
    }

    public Map<String, List<Object>> getInserted_records() {
        return inserted_records;
    }

    public void setInserted_records(Map<String, List<Object>> inserted_records) {
        this.inserted_records = inserted_records;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessageAsString() {
        return message != null ? message : "Unknown error";
    }
}