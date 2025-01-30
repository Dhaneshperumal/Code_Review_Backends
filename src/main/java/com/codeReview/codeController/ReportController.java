package com.codeReview.codeController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeReview.code.Report;
import com.codeReview.codeService.ReportService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return new ResponseEntity<>(reportService.getAllReports(), HttpStatus.OK);
    }

    @GetMapping("/{id}/download-pdf")
    public ResponseEntity<byte[]> downloadPdfReport(@PathVariable Long id) throws IOException {
        Report report = reportService.getReportById(id);
        if (report != null) {
            // Replace this with your PDF generation logic
            byte[] pdfContent = generatePdfReport(report);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "report.pdf");
            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/download-excel")
    public ResponseEntity<byte[]> downloadExcelReport(@PathVariable Long id) throws IOException {
        Report report = reportService.getReportById(id);
        if (report != null) {
            // Replace this with your Excel generation logic
            byte[] excelContent = generateExcelReport(report);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "report.xlsx");
            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Placeholder PDF and Excel generation methods

    private byte[] generatePdfReport(Report report) throws IOException {
        Document document = new Document();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();
            // Add report content here (using the report data)
            // ...
            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF report", e);
        }
    }

    private byte[] generateExcelReport(Report report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Create a sheet
            Sheet sheet = workbook.createSheet("Code Review Report");

            // Add report content here (using the report data)
            // ...

            // Write the workbook to the stream
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}