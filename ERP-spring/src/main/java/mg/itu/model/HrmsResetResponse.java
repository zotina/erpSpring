package mg.itu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HrmsResetResponse {
    private boolean success;
    private String message;
    private List<Map<String, Object>> errors;
    private Map<String, List<String>> deleted_records;

    @JsonProperty("message")
    private void unpackNestedMessage(Object nestedMessage) {
        if (nestedMessage instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) nestedMessage;
            this.success = (Boolean) map.getOrDefault("success", false);
            this.message = (String) map.getOrDefault("message", "Unknown error");
            this.errors = (List<Map<String, Object>>) map.get("errors");
            this.deleted_records = (Map<String, List<String>>) map.get("deleted_records");
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

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public void setErrors(List<Map<String, Object>> errors) {
        this.errors = errors;
    }

    public Map<String, List<String>> getDeleted_records() {
        return deleted_records;
    }

    public void setDeleted_records(Map<String, List<String>> deleted_records) {
        this.deleted_records = deleted_records;
    }

    public String getMessageAsString() {
        return message != null ? message : "Unknown error";
    }
}