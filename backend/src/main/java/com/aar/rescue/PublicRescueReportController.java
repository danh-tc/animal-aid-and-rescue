package com.aar.rescue;

import com.aar.rescue.RescueDtos.ReportReceipt;
import com.aar.rescue.RescueDtos.ReportRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** P03: báo cứu hộ. Gửi multipart (phần "report" là JSON, phần "photos" là ảnh) hoặc JSON thuần nếu không có ảnh. */
@RestController
@RequestMapping("/api/public/rescue-reports")
public class PublicRescueReportController {

    private final RescueReportService service;

    public PublicRescueReportController(RescueReportService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportReceipt submitWithPhotos(@Valid @RequestPart("report") ReportRequest report,
                                          @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return service.submit(report, photos);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportReceipt submit(@Valid @RequestBody ReportRequest report) {
        return service.submit(report, List.of());
    }
}
