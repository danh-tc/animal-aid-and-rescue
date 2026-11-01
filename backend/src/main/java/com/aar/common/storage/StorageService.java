package com.aar.common.storage;

import com.aar.common.config.AppProperties;
import com.aar.common.error.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu tệp đính kèm trên ổ đĩa của server (báo cáo mục 5.1), metadata ở bảng attachments.
 * Tên tệp trên đĩa là UUID nên không lộ tên gốc và không bị ghi đè.
 */
@Service
public class StorageService {

    private static final Map<String, String> ALLOWED = Map.of(
        MediaType.IMAGE_JPEG_VALUE, ".jpg",
        MediaType.IMAGE_PNG_VALUE, ".png",
        "image/webp", ".webp",
        MediaType.APPLICATION_PDF_VALUE, ".pdf");

    private final Path root;
    private final int maxFiles;
    private final AttachmentRepository attachments;

    public StorageService(AppProperties props, AttachmentRepository attachments) {
        this.root = Path.of(props.storage().dir()).toAbsolutePath().normalize();
        this.maxFiles = props.storage().maxFilesPerRequest();
        this.attachments = attachments;
    }

    @Transactional
    public List<Attachment> store(AttachmentOwner owner, Long ownerId, List<MultipartFile> files) {
        List<MultipartFile> nonEmpty = files == null ? List.of()
            : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (nonEmpty.size() > maxFiles) {
            throw BusinessException.badRequest("TOO_MANY_FILES", "Tối đa " + maxFiles + " tệp mỗi lần tải lên");
        }
        nonEmpty.forEach(this::validate);
        List<Attachment> saved = new ArrayList<>();
        for (MultipartFile file : nonEmpty) {
            String relative = write(file);
            saved.add(attachments.save(new Attachment(owner, ownerId, relative, cleanName(file.getOriginalFilename()))));
        }
        return saved;
    }

    public Resource load(Attachment attachment) {
        Path path = root.resolve(attachment.getFilePath()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw BusinessException.notFound("Không tìm thấy tệp");
        }
        return new PathResource(path);
    }

    public static MediaType mediaTypeOf(Attachment attachment) {
        String p = attachment.getFilePath();
        if (p.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (p.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (p.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.IMAGE_JPEG;
    }

    private void validate(MultipartFile file) {
        if (!ALLOWED.containsKey(file.getContentType())) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                "Chỉ nhận ảnh JPG, PNG, WEBP hoặc tệp PDF");
        }
    }

    private String write(MultipartFile file) {
        LocalDate today = LocalDate.now();
        String relative = today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/"
            + UUID.randomUUID() + ALLOWED.get(file.getContentType());
        Path target = root.resolve(relative);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Không lưu được tệp", e);
        }
        return relative;
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String base = Path.of(name).getFileName().toString();
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }
}
