package mg.itu.service;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import mg.itu.model.HrmsCsvImportRequest;
import mg.itu.model.HrmsCsvImportResponse;
import mg.itu.model.HrmsResetResponse;

@Service
public class HrmsCsvImportService {

    private static final Logger logger = LoggerFactory.getLogger(HrmsCsvImportService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${api.method}")
    private String baseApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HrmsCsvImportResponse importCsvFiles(MultipartFile employeesCsv, MultipartFile salaryStructureCsv, 
                                               MultipartFile payrollCsv, HttpSession session) throws Exception {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");

        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        
        HrmsCsvImportRequest request = new HrmsCsvImportRequest();
        if (employeesCsv != null && !employeesCsv.isEmpty()) {
            request.setEmployeesCsv(Base64.getEncoder().encodeToString(employeesCsv.getBytes()));
            System.out.println("employeesCsv encoded, size: " + employeesCsv.getSize());
        }
        if (salaryStructureCsv != null && !salaryStructureCsv.isEmpty()) {
            request.setSalaryStructureCsv(Base64.getEncoder().encodeToString(salaryStructureCsv.getBytes()));
            System.out.println("salaryStructureCsv encoded, size: "+ salaryStructureCsv.getSize());
        }
        if (payrollCsv != null && !payrollCsv.isEmpty()) {
            request.setPayrollCsv(Base64.getEncoder().encodeToString(payrollCsv.getBytes()));
            System.out.println("payrollCsv encoded, size: "+ payrollCsv.getSize());
        }

        String url = baseApiUrl + "/hrms.controllers.hrms_controller.import_csvs_from_json";
        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            System.out.println("Sending request to {} with body: "+ requestBody);
    
            ResponseEntity<String> response = client.post()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("Received response: " + response.getBody());
                HrmsCsvImportResponse hrmsResponse = objectMapper.readValue(response.getBody(), HrmsCsvImportResponse.class);
                return hrmsResponse;
            }

            System.out.println("Received null or unsuccessful response:"+ response);
            HrmsCsvImportResponse errorResponse = new HrmsCsvImportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to import CSV files");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error importing CSV files", e);
            HrmsCsvImportResponse errorResponse = new HrmsCsvImportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error occurred: " + e.getMessage());
            return errorResponse;
        }
    }
    
    public HrmsResetResponse resetHrmsData(HttpSession session) throws Exception {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");

        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = baseApiUrl + "/hrms.controllers.hrms_reset_controller.reset_hrms_data";
        WebClient client = webClientBuilder.baseUrl(url).build();

        try {
            ResponseEntity<String> response = client.post()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Received reset response: {}", response.getBody());
                return objectMapper.readValue(response.getBody(), HrmsResetResponse.class);
            }

            logger.warn("Received null or unsuccessful response: {}", response);
            HrmsResetResponse errorResponse = new HrmsResetResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to reset HRMS data");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error resetting HRMS data", e);
            HrmsResetResponse errorResponse = new HrmsResetResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error occurred: " + e.getMessage());
            return errorResponse;
        }
    }
} 