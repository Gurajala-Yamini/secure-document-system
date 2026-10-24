
package com.trustvault.trustvault.service;

import com.trustvault.trustvault.AppUser;
import com.trustvault.trustvault.AppUserRepository;
import com.trustvault.trustvault.Company;
import com.trustvault.trustvault.Document;
import com.trustvault.trustvault.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final AppUserRepository userRepository;

    public DocumentService(
            DocumentRepository repository,
            AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // Get documents belonging to the user's company
    public List<Document> getDocumentsByUser(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        Company company = user.getCompany();

        if (company == null) {
            throw new IllegalStateException(
                    "User is not assigned to a company.");
        }

        return repository.findByCompanyId(company.getId());
    }

    // Upload a document and assign it to the user's company
    public Document uploadDocument(
            String title,
            MultipartFile file,
            String uploadedBy) throws IOException {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Document title is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file.");
        }

        AppUser user = userRepository.findByUsername(uploadedBy)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        Company company = user.getCompany();

        if (company == null) {
            throw new IllegalStateException(
                    "User is not assigned to a company.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid file name.");
        }

        String originalName = Paths.get(originalFilename)
                .getFileName().toString();

        String storedName = UUID.randomUUID()
                + "_" + originalName;

        Path uploadDir = Paths.get("uploads")
                .toAbsolutePath().normalize();

        Files.createDirectories(uploadDir);

        Path destination = uploadDir.resolve(storedName).normalize();

        if (!destination.startsWith(uploadDir)) {
            throw new IOException("Invalid file path.");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        Document document = new Document(
                title.trim(), storedName, uploadedBy);

        document.setCompany(company);

        return repository.save(document);
    }
}
