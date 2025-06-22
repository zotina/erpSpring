package mg.itu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.EmployeeDTO;
import mg.itu.model.PaginatedResponse;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(HrmsService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${api.base-url-hr}")
    private String baseApiUrl;

    @Value("${api.method}")
    private String apiMethod;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiResponse<EmployeeDTO> getEmployeeList(String search, String department, String designation,
            String startDate, String endDate, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        StringBuilder filters = new StringBuilder("[");
        List<String> conditions = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            conditions.add("[\"employee_name\",\"like\",\"%" + search.trim() + "%\"]");
        }
        if (department != null && !department.trim().isEmpty() && !department.equals("Tous")) {
            conditions.add("[\"department\",\"=\",\"" + department + "\"]");
        }
        if (designation != null && !designation.trim().isEmpty() && !designation.equals("Tous")) {
            conditions.add("[\"designation\",\"=\",\"" + designation + "\"]");
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            conditions.add("[\"date_of_joining\",\">=\",\"" + startDate + "\"]");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            conditions.add("[\"date_of_joining\",\"<=\",\"" + endDate + "\"]");
        }

        filters.append(String.join(",", conditions)).append("]");

        String url;
        if (conditions.isEmpty()) {
            url = baseApiUrl
                    + "/Employee?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"date_of_joining\",\"status\"]&limit_page_length=0";
        } else {
            url = baseApiUrl
                    + "/Employee?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"date_of_joining\",\"status\"]&filters="
                    + filters.toString() + "&limit_page_length=0";
        }

        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            System.out.println("response " + response);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<EmployeeDTO> employees = new ArrayList<>();

                for (Map<String, Object> item : data) {
                    EmployeeDTO employee = objectMapper.convertValue(item, EmployeeDTO.class);
                    employees.add(employee);
                }

                ApiResponse<EmployeeDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Employees fetched successfully");
                apiResponse.setData(employees);
                return apiResponse;
            }

            ApiResponse<EmployeeDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch employees");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error fetching employees", e);
            ApiResponse<EmployeeDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching employees: " + e.getMessage());
            return errorResponse;
        }
    }

    public ApiResponse<EmployeeDTO> getAllEmployees(HttpSession session) {
        return getEmployeeList(null, null, null, null, null, session);
    }

    public ApiResponse<EmployeeDTO> getEmployeeDetails(String id, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");

        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = baseApiUrl + "/Employee/" + id + "?fields=[\"*\"]" + "&limit_page_length=500";
        WebClient client = webClientBuilder.baseUrl(url).build();
        System.out.println("url employe " + url);
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
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                EmployeeDTO employee = objectMapper.convertValue(data, EmployeeDTO.class);
                ApiResponse<EmployeeDTO> apiResponse = new ApiResponse<>();
                apiResponse.setStatus("success");
                apiResponse.setMessage("Employee details fetched successfully");
                apiResponse.setData(List.of(employee));
                return apiResponse;
            }

            ApiResponse<EmployeeDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch employee details");
            return errorResponse;
        } catch (Exception e) {
            logger.error("Error fetching employee details", e);
            ApiResponse<EmployeeDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching employee details: " + e.getMessage());
            return errorResponse;
        }
    }

    @SuppressWarnings("unchecked")
    public PaginatedResponse<EmployeeDTO> getEmployeeListPaginated(String search, String department, String designation, 
                                                                String startDate, String endDate, int page, int size, 
                                                                HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        
        StringBuilder filters = new StringBuilder("[");
        List<String> conditions = new ArrayList<>(); 
        
        if (search != null && !search.trim().isEmpty()) {
            conditions.add("[\"employee_name\",\"like\",\"%" + search + "%\"]");
        }
        if (department != null && !department.trim().isEmpty() && !department.equals("Tous")) {
            conditions.add("[\"department\",\"=\",\"" + department + "\"]");
        }
        if (designation != null && !designation.trim().isEmpty() && !designation.equals("Tous")) {
            conditions.add("[\"designation\",\"=\",\"" + designation + "\"]");
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            conditions.add("[\"date_of_joining\",\">=\",\"" + startDate + "\"]");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            conditions.add("[\"date_of_joining\",\"<=\",\"" + endDate + "\"]");
        }
        
        filters.append(String.join(",", conditions)).append("]");
        
        try {
            
            String countUrl = baseApiUrl + "/Employee?fields=[\"name\"]&filters=" + filters.toString();
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
            String dataUrl = baseApiUrl + "/Employee?fields=[\"*\"]&filters=" + filters.toString() + 
                            "&limit_page_length=" + size + "&limit_start=" + offset;
            
            WebClient dataClient = webClientBuilder.baseUrl(dataUrl).build();
            ResponseEntity<String> dataResponse = dataClient.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            List<EmployeeDTO> employees = new ArrayList<>();
            if (dataResponse != null && dataResponse.getStatusCode().is2xxSuccessful() && dataResponse.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(dataResponse.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        EmployeeDTO employee = objectMapper.convertValue(item, EmployeeDTO.class);
                        employees.add(employee);
                    }
                }
            }
            
            
            int totalPages = totalCount > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
            
            logger.info("=== PAGINATION CORRECTED DEBUG ===");
            logger.info("Total Count (from count query): {}", totalCount);
            logger.info("Data Size (from paginated query): {}", employees.size());
            logger.info("Total Pages: {}", totalPages);
            logger.info("Current Page: {}", page);
            logger.info("Page Size: {}", size);
            
            return new PaginatedResponse<>(employees, page, totalPages, totalCount, size);
            
        } catch (Exception e) {
            logger.error("Error fetching employees", e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage());
        }
    }
}