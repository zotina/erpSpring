package mg.itu.service;

import java.util.ArrayList;
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
import mg.itu.model.PayrollDTO;
import mg.itu.model.SalaryComponentDTO;
import mg.itu.model.SalaryDetailDTO;
import mg.itu.model.SummaryDTO;
import mg.itu.model.UpdateBaseAssignmentDTO;
import reactor.core.publisher.Mono;

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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> countResponseMap = objectMapper.readValue(countResponse.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(dataResponse.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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

    public ApiResponse<PayrollDTO> insertSalarySlip(String employeeId, String monthDebut, String monthFin, Double montant,int ecraser, int moyen, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = apiMethod + "/hrms.controllers.generator_controller.insert_slip_period";

        Map<String, Object> payload = new HashMap<>();
        payload.put("emp", employeeId);
        payload.put("monthDebut", monthDebut);
        payload.put("monthFin", monthFin);
        payload.put("montant", montant != null ? montant : 0.0);
        payload.put("ecraser",ecraser);
        payload.put("moyen", moyen);

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
                
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMap = (Map<String, Object>) responseMap.get("message");

                if (messageMap == null) {
                    ApiResponse<PayrollDTO> errorResponse = new ApiResponse<>();
                    errorResponse.setStatus("error");
                    errorResponse.setMessage("Invalid response format: 'message' object missing");
                    return errorResponse;
                }

                String status = (String) messageMap.get("status");
                String message = (String) messageMap.get("message");

                if ("success".equals(status)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) messageMap.get("data");
                    List<PayrollDTO> payrollList = new ArrayList<>();

                    for (Map<String, Object> data : dataList) {
                        PayrollDTO payroll = objectMapper.convertValue(data, PayrollDTO.class);
                        payrollList.add(payroll);
                    }

                    ApiResponse<PayrollDTO> apiResponse = new ApiResponse<>();
                    apiResponse.setStatus("success");
                    apiResponse.setMessage(message);
                    apiResponse.setData(payrollList);
                    return apiResponse;
                } else {
                    ApiResponse<PayrollDTO> errorResponse = new ApiResponse<>();
                    errorResponse.setStatus(status);
                    errorResponse.setMessage(message);
                    return errorResponse;
                }
            }

            ApiResponse<PayrollDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to create salary slip");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error creating salary slip", e);
            ApiResponse<PayrollDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error creating salary slip: " + e.getMessage());
            return errorResponse;
        }
    }

    public ApiResponse<SalaryComponentDTO> getAllSalaryComponents(HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = baseApiUrl + "/Salary Component?fields=[\"name\",\"salary_component\"]&limit_page_length=0";

        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            Mono<String> responseMono = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .bodyToMono(String.class);
            
            String response = responseMono.block();
            System.out.println("response " + response);
            if (response != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<SalaryComponentDTO> components = new ArrayList<>();

                for (Map<String, Object> item : data) {
                    SalaryComponentDTO component = objectMapper.convertValue(item, SalaryComponentDTO.class);
                    components.add(component);
                }

                ApiResponse<SalaryComponentDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Salary components fetched successfully");
                apiResponse.setData(components);
                return apiResponse;
            }

            ApiResponse<SalaryComponentDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch salary components");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error fetching salary components", e);
            ApiResponse<SalaryComponentDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching salary components: " + e.getMessage());
            return errorResponse;
        }
    }

    public ApiResponse<UpdateBaseAssignmentDTO> updateBaseAssignment(
        String salaryComponent, Double montant, Integer infOrSup, Integer minusOrPlus, Double taux, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = apiMethod + "/hrms.controllers.generator_controller.updateBaseAssignement";

        Map<String, Object> payload = new HashMap<>();
        payload.put("salary_component", salaryComponent);
        payload.put("montant", montant != null ? montant : 0.0);
        payload.put("infOrSup", infOrSup);
        payload.put("minusOrPlus", minusOrPlus);
        payload.put("taux", taux != null ? taux : 0.0);

        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            Mono<String> responseMono = client.post()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class);

            String response = responseMono.block();
            if (response != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMap = (Map<String, Object>) responseMap.get("message");

                if (messageMap == null) {
                    ApiResponse<UpdateBaseAssignmentDTO> errorResponse = new ApiResponse<>();
                    errorResponse.setStatus("error");
                    errorResponse.setMessage("Invalid response format: 'message' object missing");
                    return errorResponse;
                }

                String status = (String) messageMap.get("status");
                String message = (String) messageMap.get("message");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> updatedSlipsData = (List<Map<String, Object>>) messageMap.get("updated_slips");

                List<UpdateBaseAssignmentDTO> updatedSlips = new ArrayList<>();
                for (Map<String, Object> slipData : updatedSlipsData) {
                    UpdateBaseAssignmentDTO slip = objectMapper.convertValue(slipData, UpdateBaseAssignmentDTO.class);
                    updatedSlips.add(slip);
                }

                ApiResponse<UpdateBaseAssignmentDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus(status);
                apiResponse.setMessage(message);
                apiResponse.setData(updatedSlips);
                return apiResponse;
            }

            ApiResponse<UpdateBaseAssignmentDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to update base assignment");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error updating base assignment", e);
            ApiResponse<UpdateBaseAssignmentDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error updating base assignment: " + e.getMessage());
            return errorResponse;
        }
    }

    // Add these methods to your existing HrmsService class

public PaginatedResponse<SummaryDTO> getMonthlySummaryWithFilter(String salaryComponent, Double montant, 
                                                                 Integer infOrSup, int page, int size, 
                                                                 HttpSession session) {
    String accessToken = (String) session.getAttribute("access_token");
    String sid = (String) session.getAttribute("sid");
    if (accessToken == null || sid == null) {
        throw new IllegalStateException("User is not authenticated");
    }
    
    try {
        // First get all salary slips to count total
        String countUrl = baseApiUrl + "/Salary Slip?fields=[\"name\"]&limit_page_length=0";
        
        WebClient countClient = webClientBuilder.baseUrl(countUrl).build();
        ResponseEntity<String> countResponse = countClient.get()
                .header("Authorization", "Bearer " + accessToken)
                .cookie("sid", sid)
                .retrieve()
                .toEntity(String.class)
                .block();
        
        List<SummaryDTO> allData = new ArrayList<>();
        if (countResponse != null && countResponse.getStatusCode().is2xxSuccessful() && countResponse.getBody() != null) {
            // Get all data for filtering
            String dataUrl = baseApiUrl + "/Salary Slip?fields=[\"*\"]&limit_page_length=0";
            
            WebClient dataClient = webClientBuilder.baseUrl(dataUrl).build();
            ResponseEntity<String> dataResponse = dataClient.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (dataResponse != null && dataResponse.getStatusCode().is2xxSuccessful() && dataResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(dataResponse.getBody(), Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        SummaryDTO itemSummary = objectMapper.convertValue(item, SummaryDTO.class);
                        itemSummary.setMonthYear(itemSummary.getPostingDate());
                        allData.add(itemSummary);
                    }
                }
            }
        }
        
        // Filter the data based on salary component and amount criteria
        List<SummaryDTO> filteredData = filterBySalaryComponent(allData, salaryComponent, montant, infOrSup, session);
        
        // Apply pagination to filtered results
        int totalCount = filteredData.size();
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalCount);
        
        List<SummaryDTO> paginatedData = new ArrayList<>();
        if (startIndex < totalCount) {
            paginatedData = filteredData.subList(startIndex, endIndex);
        }
        
        int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
        
        logger.info("=== FILTER SEARCH DEBUG ===");
        logger.info("Total filtered records: {}", totalCount);
        logger.info("Page size: {}, Current page: {}", size, page);
        logger.info("Start index: {}, End index: {}", startIndex, endIndex);
        logger.info("Paginated data size: {}", paginatedData.size());
        
        return new PaginatedResponse<>(paginatedData, page, totalPages, totalCount, size);
        
    } catch (Exception e) {
        logger.error("Error fetching filtered monthly summary", e);
        throw new RuntimeException("Error fetching filtered monthly summary: " + e.getMessage());
    }
}

public ApiResponse<SummaryDTO> getAllMonthlySummaryWithFilter(String salaryComponent, Double montant, 
                                                             Integer infOrSup, HttpSession session) {
    String accessToken = (String) session.getAttribute("access_token");
    String sid = (String) session.getAttribute("sid");
    if (accessToken == null || sid == null) {
        throw new IllegalStateException("User is not authenticated");
    }
    
    try {
        // Get all salary slips
        String url = baseApiUrl + "/Salary Slip?fields=[\"*\"]&limit_page_length=0";
        
        WebClient client = webClientBuilder.baseUrl(url).build();
        ResponseEntity<String> response = client.get()
                .header("Authorization", "Bearer " + accessToken)
                .cookie("sid", sid)
                .retrieve()
                .toEntity(String.class)
                .block();
        
        List<SummaryDTO> allData = new ArrayList<>();
        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            
            if (data != null) {
                for (Map<String, Object> item : data) {
                    SummaryDTO itemSummary = objectMapper.convertValue(item, SummaryDTO.class);
                    itemSummary.setMonthYear(itemSummary.getPostingDate());
                    allData.add(itemSummary);
                }
            }
        }
        
        // Filter the data
        List<SummaryDTO> filteredData = filterBySalaryComponent(allData, salaryComponent, montant, infOrSup, session);
        
        ApiResponse<SummaryDTO> apiResponse = new ApiResponse<>();
        apiResponse.setStatus("success");
        apiResponse.setMessage("Filtered monthly summary fetched successfully");
        apiResponse.setData(filteredData);
        return apiResponse;
        
    } catch (Exception e) {
        logger.error("Error fetching all filtered monthly summary", e);
        ApiResponse<SummaryDTO> errorResponse = new ApiResponse<>();
        errorResponse.setStatus("error");
        errorResponse.setMessage("Error fetching filtered monthly summary: " + e.getMessage());
        return errorResponse;
    }
}

private List<SummaryDTO> filterBySalaryComponent(List<SummaryDTO> allData, String salaryComponent, 
                                                Double montant, Integer infOrSup, HttpSession session) {
    List<SummaryDTO> filteredResults = new ArrayList<>();
    String accessToken = (String) session.getAttribute("access_token");
    String sid = (String) session.getAttribute("sid");
    
    if (accessToken == null || sid == null) {
        return filteredResults;
    }
    
    try {
        for (SummaryDTO salary : allData) {
            // Get salary slip details for each employee to check salary components
            String detailUrl = baseApiUrl + "/Salary Slip/" + salary.getName() + "?fields=[\"*\"]";
            
            WebClient detailClient = webClientBuilder.baseUrl(detailUrl).build();
            ResponseEntity<String> detailResponse = detailClient.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            if (detailResponse != null && detailResponse.getStatusCode().is2xxSuccessful() && 
                detailResponse.getBody() != null) {
                
                @SuppressWarnings("unchecked")
                Map<String, Object> detailMap = objectMapper.readValue(detailResponse.getBody(), Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) detailMap.get("data");
                
                if (data != null) {
                    // Check earnings
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> earnings = (List<Map<String, Object>>) data.get("earnings");
                    if (earnings != null && checkSalaryComponentMatch(earnings, salaryComponent, montant, infOrSup)) {
                        filteredResults.add(salary);
                        continue;
                    }
                    
                    // Check deductions
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> deductions = (List<Map<String, Object>>) data.get("deductions");
                    if (deductions != null && checkSalaryComponentMatch(deductions, salaryComponent, montant, infOrSup)) {
                        filteredResults.add(salary);
                    }
                }
            }
        }
    } catch (Exception e) {
        logger.error("Error filtering by salary component", e);
    }
    
    return filteredResults;
}

private boolean checkSalaryComponentMatch(List<Map<String, Object>> components, String targetComponent, 
                                        Double targetAmount, Integer comparison) {
    for (Map<String, Object> component : components) {
        String componentName = (String) component.get("salary_component");
        Object amountObj = component.get("amount");
        
        if (componentName != null && componentName.equals(targetComponent) && amountObj != null) {
            Double componentAmount = 0.0;
            if (amountObj instanceof Number) {
                componentAmount = ((Number) amountObj).doubleValue();
            }
            
            // Apply comparison logic
            switch (comparison) {
                case 0: // Inférieur (<)
                    return componentAmount < targetAmount;
                case 1: // Supérieur (>)
                    return componentAmount > targetAmount;
                case 2: // Supérieur ou égal (>=)
                    return componentAmount >= targetAmount;
                case 3: // Inférieur ou égal (<=)
                    return componentAmount <= targetAmount;
                case 4: // Égal (=)
                    return componentAmount.equals(targetAmount);
                default:
                    return false;
            }
        }
    }
    return false;
}
}