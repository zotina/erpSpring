package mg.itu.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.PaginatedResponse;
import mg.itu.model.SalaryDetailDTO;
import mg.itu.model.SummaryDTO;

@Service
public class HrmsService {

    private static final Logger logger = LoggerFactory.getLogger(HrmsService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${api.base-url-hr}")
    private String baseApiUrl;

    @Value("${api.method}")
    private String apiMethod;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiResponse<SalaryDetailDTO> getSalaryHistory(String id, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = baseApiUrl + "/Salary Slip?fields=[\"*\"]&filters=[[\"employee\",\"=\",\"" + id + "\"]]"+ "&limit_page_length=500";
        WebClient client = webClientBuilder.baseUrl(url).build();

        System.out.println("url salary "+ url );

        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<SalaryDetailDTO> salaries = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    SalaryDetailDTO salary = objectMapper.convertValue(item, SalaryDetailDTO.class);
                    salaries.add(salary);
                }
                ApiResponse<SalaryDetailDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Salary history fetched successfully");
                apiResponse.setData(salaries);
                return apiResponse;
            }

            ApiResponse<SalaryDetailDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch salary history");
            return errorResponse;
        } catch (Exception e) {
            logger.error("Error fetching salary history", e);
            ApiResponse<SalaryDetailDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching salary history: " + e.getMessage());
            return errorResponse;
        }
    }
    
    public ApiResponse<SummaryDTO> getMonthlySummary(String start, String end, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        String url = baseApiUrl + "/Salary Slip?fields=[\"*\"]"; 

        if(start != null && start != "" && end != null && end != ""){
            url+="&filters=[[\"posting_date\",\">=\",\"" + start + "\"],[\"posting_date\",\"<=\",\"" + end + "\"]]";
        }
        url+= "&limit_page_length=500";

        WebClient client = webClientBuilder.baseUrl(url).build();
        System.out.println("summary url "+ url);

        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<SummaryDTO> summary = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    SummaryDTO itemSummary = objectMapper.convertValue(item, SummaryDTO.class);
                    System.out.println("posting date " + itemSummary.getPostingDate());
                    System.out.println("month year" + itemSummary.getMonthYear());
                    itemSummary.setMonthYear(itemSummary.getPostingDate());
                    summary.add(itemSummary);
                }
                ApiResponse<SummaryDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Monthly summary fetched successfully");
                apiResponse.setData(summary);
                return apiResponse;
            }

            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch monthly summary");
            return errorResponse;
        } catch (Exception e) {
            logger.error("Error fetching monthly summary", e);
            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching monthly summary: " + e.getMessage());
            return errorResponse;
        }
    }

    public String fetchSalaryEvolutionData(String year,HttpSession session) {
        try {

            String sid = (String) session.getAttribute("sid");
 
            
            String url = apiMethod + "/hrms.controllers.payroll_controller.get_salary_evolution_data?year=" + year;
            WebClient client = webClientBuilder.baseUrl(url).build();

            ResponseEntity<String> response = client.get()
                .cookie("sid", sid)
                .retrieve()
                .toEntity(String.class)
                .block();

            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully fetched salary evolution data");
                return response.getBody();
            } else {
                logger.error("Failed to fetch data. Status: {}", response != null ? response.getStatusCode() : "null");
                throw new RuntimeException("Failed to fetch salary evolution data from ERPNext");
            }

        } catch (WebClientResponseException e) {
            logger.error("WebClient error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error calling ERPNext API: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: ", e);
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }
    
    public PaginatedResponse<SummaryDTO> getMonthlySummaryPaginated(String start, String end, int page, int size, 
                                                                    HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        
        String filters = "";
        if (start != null && !start.trim().isEmpty() && end != null && !end.trim().isEmpty()) {
            filters = "&filters=[[\"posting_date\",\">=\",\"" + start + "\"],[\"posting_date\",\"<=\",\"" + end + "\"]]";
        }
        
        try {
            
            String countUrl = baseApiUrl + "/Salary Slip?fields=[\"name\"]" + filters;
            countUrl += "&limit_page_length=0"; 
            
            WebClient countClient = webClientBuilder.baseUrl(countUrl).build();
            ResponseEntity<String> countResponse = countClient.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            int totalCount = 0;
            if (countResponse != null && countResponse.getStatusCode().is2xxSuccessful() && countResponse.getBody() != null) {
                Map<String, Object> countResponseMap = objectMapper.readValue(countResponse.getBody(), Map.class);
                List<Map<String, Object>> countData = (List<Map<String, Object>>) countResponseMap.get("data");
                totalCount = countData != null ? countData.size() : 0;
            }
            
            
            int offset = (page - 1) * size;
            String dataUrl = baseApiUrl + "/Salary Slip?fields=[\"*\"]" + filters + 
                            "&limit_page_length=" + size + "&limit_start=" + offset;
            
            WebClient dataClient = webClientBuilder.baseUrl(dataUrl).build();
            System.out.println("summary url " + dataUrl);
            
            ResponseEntity<String> dataResponse = dataClient.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            List<SummaryDTO> summary = new ArrayList<>();
            if (dataResponse != null && dataResponse.getStatusCode().is2xxSuccessful() && dataResponse.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(dataResponse.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        SummaryDTO itemSummary = objectMapper.convertValue(item, SummaryDTO.class);
                        itemSummary.setMonthYear(itemSummary.getPostingDate());
                        summary.add(itemSummary);
                    }
                }
            }
            
            
            int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
            
            logger.info("=== SUMMARY PAGINATION CORRECTED DEBUG ===");
            logger.info("Total Count (from count query): {}", totalCount);
            logger.info("Data Size (from paginated query): {}", summary.size());
            logger.info("Total Pages: {}", totalPages);
            logger.info("Current Page: {}", page);
            logger.info("Page Size: {}", size);
            
            return new PaginatedResponse<>(summary, page, totalPages, totalCount, size);
            
        } catch (Exception e) {
            logger.error("Error fetching monthly summary", e);
            throw new RuntimeException("Error fetching monthly summary: " + e.getMessage());
        }
    }

    public ApiResponse<SummaryDTO> getAllMonthlySummary(String start, String end, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        String url = baseApiUrl + "/Salary Slip?fields=[\"*\"]"; 

        if (start != null && !start.trim().isEmpty() && end != null && !end.trim().isEmpty()) {
            url += "&filters=[[\"posting_date\",\">=\",\"" + start + "\"],[\"posting_date\",\"<=\",\"" + end + "\"]]";
        }
        
        
        url += "&limit_page_length=0"; 

        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<SummaryDTO> summary = new ArrayList<>();
                
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        SummaryDTO itemSummary = objectMapper.convertValue(item, SummaryDTO.class);
                        itemSummary.setMonthYear(itemSummary.getPostingDate());
                        summary.add(itemSummary);
                    }
                }
                
                ApiResponse<SummaryDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Monthly summary fetched successfully");
                apiResponse.setData(summary);
                return apiResponse;
            }

            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch monthly summary");
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Error fetching monthly summary", e);
            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching monthly summary: " + e.getMessage());
            return errorResponse;
        }
    }   

    public ApiResponse<SummaryDTO> insertSalarySlip(String employeeId, String start, String end, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = baseApiUrl + "/hrms.controllers.generator_controller.insert_salary_slip?emp=HR-EMP-00961&monthYear=2025-06";

        
        Map<String, Object> payload = new HashMap<>();
        payload.put("employee", employeeId);
        payload.put("start_date", start);
        payload.put("end_date", end);
        
        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            ResponseEntity<String> response = client.post()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                SummaryDTO salarySlip = objectMapper.convertValue(data, SummaryDTO.class);

                ApiResponse<SummaryDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Salary slip created successfully");
                apiResponse.setData(Collections.singletonList(salarySlip));
                return apiResponse;
            }

            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to create salary slip");
            return errorResponse;
        } catch (Exception e) {
            logger.error("Error creating salary slip", e);
            ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error creating salary slip: " + e.getMessage());
            return errorResponse;
        }
    }
}