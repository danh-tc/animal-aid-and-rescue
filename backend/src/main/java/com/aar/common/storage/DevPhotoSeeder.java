package com.aar.common.storage;

import com.aar.pet.PetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("dev")
public class DevPhotoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevPhotoSeeder.class);
    private static final String BASE = "dev-photos/";

    private final PetRepository pets;
    private final AttachmentRepository attachments;
    private final StorageService storage;
    private final ObjectMapper json;

    public DevPhotoSeeder(PetRepository pets, AttachmentRepository attachments, StorageService storage, ObjectMapper json) {
        this.pets = pets;
        this.attachments = attachments;
        this.storage = storage;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource manifest = new ClassPathResource(BASE + "manifest.json");
        if (!manifest.exists()) {
            return;
        }
        JsonNode petsNode;
        try (InputStream in = manifest.getInputStream()) {
            petsNode = json.readTree(in).path("pets");
        }
        int seeded = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = petsNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            var pet = pets.findByCode(entry.getKey());
            if (pet.isEmpty() || !attachments.findByPetIdOrderById(pet.get().getId()).isEmpty()) {
                continue;
            }
            List<MultipartFile> files = new ArrayList<>();
            for (JsonNode name : entry.getValue()) {
                ClassPathResource photo = new ClassPathResource(BASE + name.asText());
                try (InputStream in = photo.getInputStream()) {
                    files.add(new BytesFile(name.asText(), in.readAllBytes()));
                }
            }
            storage.store(AttachmentOwner.PET, pet.get().getId(), files);
            seeded++;
        }
        if (seeded > 0) {
            log.info("Đã gắn ảnh mẫu cho {} thú trong dữ liệu dev", seeded);
        }
    }

    private record BytesFile(String name, byte[] bytes) implements MultipartFile {

        @Override public String getName() { return "files"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return name.endsWith(".png") ? "image/png" : "image/jpeg"; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
        @Override public void transferTo(Path dest) throws IOException { Files.write(dest, bytes); }
    }
}
