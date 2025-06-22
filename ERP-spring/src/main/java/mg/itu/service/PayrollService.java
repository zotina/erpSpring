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
import mg.itu.model.MonthlyPayrollSummary;
import mg.itu.model.PayrollComponents;
import mg.itu.model.PayrollSlipDTO;
import mg.itu.model.SalaryComponent;
import mg.itu.util.DateUtil;

@Service
public class PayrollService {
    
    private static final Logger logger = LoggerFactory.getLogger(HrmsService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${api.base-url-hr}")
    private String baseApiUrl;

    @Value("${api.method}")
    private String apiMethod;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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
                
                
                @SuppressWarnings("unchecked")
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
                
                
                @SuppressWarnings("unchecked")
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
                
                
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
}