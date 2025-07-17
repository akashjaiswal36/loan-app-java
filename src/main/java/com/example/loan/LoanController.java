@Controller
public class LoanController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/foreclosure")
    public String foreclosure() {
        return "foreclosure";
    }

    @PostMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateExcel(
            @RequestParam double principal,
            @RequestParam double rate,
            @RequestParam int months
    ) throws IOException {

        double r = rate / 12;
        double emi = principal * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);
        List<AmortizationEntry> schedule = new ArrayList<>();

        double remaining = principal;
        for (int month = 1; month <= months; month++) {
            double interest = remaining * r;
            double principalPaid = emi - interest;
            remaining -= principalPaid;
            if (remaining < 0) remaining = 0;

            schedule.add(new AmortizationEntry(month, emi, interest, principalPaid, remaining));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Schedule");

        String[] headers = {"Month", "EMI", "Interest Paid", "Principal Paid", "Remaining Balance"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++)
            headerRow.createCell(i).setCellValue(headers[i]);

        int rowIdx = 1;
        for (AmortizationEntry entry : schedule) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getMonth());
            row.createCell(1).setCellValue(round(entry.getEmi()));
            row.createCell(2).setCellValue(round(entry.getInterest()));
            row.createCell(3).setCellValue(round(entry.getPrincipal()));
            row.createCell(4).setCellValue(round(entry.getRemaining()));
        }

        workbook.write(out);
        workbook.close();

        ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
        HttpHeaders headersOut = new HttpHeaders();
        headersOut.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loan_schedule.xlsx");

        return ResponseEntity.ok()
                .headers(headersOut)
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/calculate_foreclosure")
    public String calculateForeclosure(
            @RequestParam double principal,
            @RequestParam double rate,
            @RequestParam int months,
            @RequestParam int foreclose_month,
            Model model
    ) {
        double r = rate / 12;
        double emi = principal * r * Math.pow(1 + r, months) / (Math.pow(1 + r, months) - 1);

        double remaining = principal;
        for (int m = 1; m <= foreclose_month; m++) {
            double interest = remaining * r;
            double principalPaid = emi - interest;
            remaining -= principalPaid;
            if (remaining < 0) {
                remaining = 0;
                break;
            }
        }

        double charge = 0.04 * remaining;
        double gst = 0.18 * charge;
        double total = remaining + charge + gst;

        model.addAttribute("results", Map.of(
                "month", foreclose_month,
                "remaining_balance", round(remaining),
                "charge", round(charge),
                "gst", round(gst),
                "total", round(total)
        ));

        return "foreclosure";
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
