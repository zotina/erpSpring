package mg.itu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import mg.itu.model.ApiResponse;
import mg.itu.model.EmployeeDTO;
import mg.itu.model.MonthlyPayrollSummary;
import mg.itu.model.PaginatedResponse;
import mg.itu.model.PayrollComponents;
import mg.itu.model.PayrollDTO;
import mg.itu.model.PayrollSlipDTO;
import mg.itu.model.SalaryComponent;
import mg.itu.model.SalaryDetailDTO;
import mg.itu.model.SummaryDTO;
import mg.itu.util.DateUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public ApiResponse<EmployeeDTO> getEmployeeList(String search, String department, String designation, String startDate, String endDate, HttpSession session) {
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
            url = baseApiUrl + "/Employee?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"date_of_joining\",\"status\"]&limit_page_length=0";
        } else {
            url = baseApiUrl + "/Employee?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"date_of_joining\",\"status\"]&filters=" + filters.toString() + "&limit_page_length=0";
        }
        
        WebClient client = webClientBuilder.baseUrl(url).build();
    
        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            System.out.println("response "+response);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
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

        String url = baseApiUrl + "/Employee/" + id + "?fields=[\"*\"]"+ "&limit_page_length=500";
        WebClient client = webClientBuilder.baseUrl(url).build();
        System.out.println("url employe "+ url);
        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
   
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
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

    public ApiResponse<PayrollSlipDTO> generatePayrollSlip(String employeeId, String startDate,String endDate, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }
         
        
        String url = baseApiUrl + "/Salary Slip?fields=[\"*\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"],[\"posting_date\",\">=\",\"" + startDate + "\"],[\"posting_date\",\"<=\",\"" + endDate + "\"]]" + "&limit_page_length=500";
        
        System.out.println("URL Salary Slip: " + url);
        
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
                ApiResponse<PayrollSlipDTO> apiResponse = new ApiResponse<>();
                
                if (!data.isEmpty()) {
                    Map<String, Object> salarySlipData = data.get(0);
                    String salarySlipName = (String) salarySlipData.get("name");
                    
                    
                    PayrollSlipDTO slip = getDetailedSalarySlip(salarySlipName, accessToken, sid);
                    
                    apiResponse.setStatus("success");
                    
                    if (slip != null) {
                        apiResponse.setMessage("Payroll slip generated successfully");
                        apiResponse.setData(List.of(slip));
                        return apiResponse;
                    }
                    
                    apiResponse.setMessage("no Payroll slip found");
                    apiResponse.setData(null);
                    return apiResponse;
                }
            }
            
            ApiResponse<PayrollSlipDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("No payroll slip found for the selected period");
            errorResponse.setData(null);
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("Error generating payroll slip", e);
            ApiResponse<PayrollSlipDTO> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error generating payroll slip: " + e.getMessage());
            errorResponse.setData(null); 
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

    private PayrollSlipDTO getDetailedSalarySlip(String salarySlipName, String accessToken, String sid) {
        try {
            
            String detailUrl = baseApiUrl + "/Salary Slip/" + salarySlipName + "?limit_page_length=500";
            
            WebClient client = webClientBuilder.baseUrl(detailUrl).build();
            
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                
                
                PayrollSlipDTO slip = new PayrollSlipDTO();
                
                
                slip.setName((String) data.get("name"));
                slip.setEmployee((String) data.get("employee"));
                slip.setEmployeeName((String) data.get("employee_name"));
                slip.setDepartment((String) data.get("department"));
                slip.setDesignation((String) data.get("designation"));
                slip.setCompany((String) data.get("company"));
                slip.setCurrency((String) data.get("currency"));
                slip.setPayrollFrequency((String) data.get("payroll_frequency"));
                
                
                if (data.get("start_date") != null) {
                    slip.setStartDate(DateUtil.parseDate((String) data.get("start_date")));
                }
                if (data.get("end_date") != null) {
                    slip.setEndDate(DateUtil.parseDate((String) data.get("end_date")));
                }
                if (data.get("posting_date") != null) {
                    slip.setPostingDate(DateUtil.parseDate((String) data.get("posting_date")));
                }
                
                
                slip.setTotalWorkingDays(DateUtil.getDoubleValue(data.get("total_working_days")));
                slip.setPaymentDays(DateUtil.getDoubleValue(data.get("payment_days")));
                slip.setHourRate(DateUtil.getDoubleValue(data.get("hour_rate")));
                slip.setTotalWorkingHours(DateUtil.getDoubleValue(data.get("total_working_hours")));
                slip.setGrossPay(DateUtil.getDoubleValue(data.get("gross_pay")));
                slip.setTotalDeduction(DateUtil.getDoubleValue(data.get("total_deduction")));
                slip.setNetPay(DateUtil.getDoubleValue(data.get("net_pay")));
                slip.setRoundedTotal(DateUtil.getDoubleValue(data.get("rounded_total")));
                
                
                List<Map<String, Object>> earningsData = (List<Map<String, Object>>) data.get("earnings");
                if (earningsData != null) {
                    List<PayrollSlipDTO.SalaryDetail> earnings = new ArrayList<>();
                    for (Map<String, Object> earning : earningsData) {
                        PayrollSlipDTO.SalaryDetail detail = new PayrollSlipDTO.SalaryDetail();
                        detail.setSalaryComponent((String) earning.get("salary_component"));
                        detail.setAbbr((String) earning.get("abbr"));
                        detail.setAmount(DateUtil.getDoubleValue(earning.get("amount")));
                        detail.setFormula((String) earning.get("formula"));
                        detail.setCondition((String) earning.get("condition"));
                        detail.setDependsOnPaymentDays(DateUtil.getBooleanValue(earning.get("depends_on_payment_days")));
                        detail.setExemptFromPayrollTax(DateUtil.getBooleanValue(earning.get("exempt_from_payroll_tax")));
                        detail.setDoNotIncludeInTotal(DateUtil.getBooleanValue(earning.get("do_not_include_in_total")));
                        detail.setStatisticalComponent(DateUtil.getBooleanValue(earning.get("statistical_component")));
                        earnings.add(detail);
                    }
                    slip.setEarnings(earnings);
                }
                
                
                List<Map<String, Object>> deductionsData = (List<Map<String, Object>>) data.get("deductions");
                if (deductionsData != null) {
                    List<PayrollSlipDTO.SalaryDetail> deductions = new ArrayList<>();
                    for (Map<String, Object> deduction : deductionsData) {
                        PayrollSlipDTO.SalaryDetail detail = new PayrollSlipDTO.SalaryDetail();
                        detail.setSalaryComponent((String) deduction.get("salary_component"));
                        detail.setAbbr((String) deduction.get("abbr"));
                        detail.setAmount(DateUtil.getDoubleValue(deduction.get("amount")));
                        detail.setFormula((String) deduction.get("formula"));
                        detail.setCondition((String) deduction.get("condition"));
                        detail.setDependsOnPaymentDays(DateUtil.getBooleanValue(deduction.get("depends_on_payment_days")));
                        detail.setExemptFromPayrollTax(DateUtil.getBooleanValue(deduction.get("exempt_from_payroll_tax")));
                        detail.setDoNotIncludeInTotal(DateUtil.getBooleanValue(deduction.get("do_not_include_in_total")));
                        detail.setStatisticalComponent(DateUtil.getBooleanValue(deduction.get("statistical_component")));
                        deductions.add(detail);
                    }
                    slip.setDeductions(deductions);
                }
                
                
                List<Map<String, Object>> loanRepaymentData = (List<Map<String, Object>>) data.get("loan_repayment");
                if (loanRepaymentData != null) {
                    List<PayrollSlipDTO.LoanRepaymentDetail> loanRepayments = new ArrayList<>();
                    for (Map<String, Object> loan : loanRepaymentData) {
                        PayrollSlipDTO.LoanRepaymentDetail detail = new PayrollSlipDTO.LoanRepaymentDetail();
                        detail.setLoan((String) loan.get("loan"));
                        detail.setLoanType((String) loan.get("loan_type"));
                        detail.setPrincipalAmount(DateUtil.getDoubleValue(loan.get("principal_amount")));
                        detail.setInterestAmount(DateUtil.getDoubleValue(loan.get("interest_amount")));
                        detail.setTotalPayment(DateUtil.getDoubleValue(loan.get("total_payment")));
                        loanRepayments.add(detail);
                    }
                    slip.setLoanRepayment(loanRepayments);
                }
                
                return slip;
            }
            
        } catch (Exception e) {
            logger.error("Error fetching detailed salary slip", e);
        }
        
        return null;
    }
    public ApiResponse<MonthlyPayrollSummary> getMonthlyPayrollSummary(String year, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String url = apiMethod + "/hrms.controllers.payroll_controller.get_monthly_payroll_summary?year=" + year;
        WebClient client = webClientBuilder.baseUrl(url).build();
        System.out.println("url = "+ url);

        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            System.out.println("payrol = " + response);

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
                List<MonthlyPayrollSummary> summaries = new ArrayList<>();

                if (data != null) {
                    for (Map<String, Object> item : data) {
                        MonthlyPayrollSummary summary = objectMapper.convertValue(item, MonthlyPayrollSummary.class);
                        summaries.add(summary);
                    }
                    ApiResponse<MonthlyPayrollSummary> apiResponse = new ApiResponse<>();
                    apiResponse.setStatus("success");
                    apiResponse.setMessage("Monthly payroll summary fetched successfully");
                    apiResponse.setData(summaries);
                    return apiResponse;
                }
            }

            ApiResponse<MonthlyPayrollSummary> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("sucess");
            errorResponse.setMessage("No monthly payroll summary");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Error fetching monthly payroll summary", e);
            ApiResponse<MonthlyPayrollSummary> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching monthly payroll summary: " + e.getMessage());
            return errorResponse;
        }
    }

    public ApiResponse<PayrollComponents> getPayrollComponents(String yearMonth, String employeeId, HttpSession session) {
        String accessToken = (String) session.getAttribute("access_token");
        String sid = (String) session.getAttribute("sid");
        if (accessToken == null || sid == null) {
            throw new IllegalStateException("User is not authenticated");
        }
    
        String url = apiMethod + "/hrms.controllers.payroll_controller.get_payroll_components?year_month=" + yearMonth;
        if(employeeId!= null && employeeId!=""){
            url+="&employee_id=" + employeeId; 
        }
        WebClient client = webClientBuilder.baseUrl(url).build();
        System.out.println("url " + url);
        try {
            ResponseEntity<String> response = client.get()
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie("sid", sid)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
            System.out.println("componnent reponse " + response);
    
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                Map<String, Object> data = (Map<String, Object>) responseMap.get("message"); 
                if (data != null) {
                    PayrollComponents components = new PayrollComponents();
                    
                    components.setMonth((String) data.get("month"));
                    components.setMonthName((String) data.get("month_name"));
                    components.setTotalGross(getDoubleValue(data.get("total_gross")));
                    components.setTotalDeduction(getDoubleValue(data.get("total_deduction")));
                    components.setTotalNet(getDoubleValue(data.get("total_net")));
                    components.setCurrency((String) data.get("currency"));
                    components.setEmployeeCount(getIntValue(data.get("employee_count")));
    
                    System.out.println("component = " + components.toString());
                    
                    List<Map<String, Object>> earningsData = (List<Map<String, Object>>) data.get("earnings");
                    List<SalaryComponent> earnings = new ArrayList<>();
                    if (earningsData != null) {
                        for (Map<String, Object> earning : earningsData) {
                            SalaryComponent component = new SalaryComponent();
                            component.setSalaryComponent((String) earning.get("salary_component"));
                            component.setAmount(getDoubleValue(earning.get("amount")));
                            earnings.add(component);
                        }
                    }
                    components.setEarnings(earnings);
    
                    System.out.println("component = " + components.toString());
                    
                    List<Map<String, Object>> deductionsData = (List<Map<String, Object>>) data.get("deductions");
                    List<SalaryComponent> deductions = new ArrayList<>();
                    if (deductionsData != null) {
                        for (Map<String, Object> deduction : deductionsData) {
                            SalaryComponent component = new SalaryComponent();
                            component.setSalaryComponent((String) deduction.get("salary_component"));
                            component.setAmount(getDoubleValue(deduction.get("amount")));
                            deductions.add(component);
                        }
                    }
                    components.setDeductions(deductions);
    
                    System.out.println("component = " + components.toString());
                    
                    ApiResponse<PayrollComponents> apiResponse = new ApiResponse<>();
                    apiResponse.setStatus("success");
                    apiResponse.setMessage("Payroll components fetched successfully");
                    apiResponse.setData(List.of(components));
                    return apiResponse;
                }
            }
    
            ApiResponse<PayrollComponents> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to fetch payroll components");
            return errorResponse;
        } catch (Exception e) {
            logger.error("Error fetching payroll components", e);
            ApiResponse<PayrollComponents> errorResponse = new ApiResponse<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Error fetching payroll components: " + e.getMessage());
            return errorResponse;
        }
    }

    
    private double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private int getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
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

    public ApiResponse<PayrollDTO> insertSalarySlip(String employeeId, String monthDebut, String monthFin, Double montant, HttpSession session) {
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
}