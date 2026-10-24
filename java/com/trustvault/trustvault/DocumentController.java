
package com.trustvault.trustvault;

import com.trustvault.trustvault.service.DocumentService;
import com.trustvault.trustvault.service.AuditService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;
    private final AppUserRepository appUserRepository;

    public DocumentController(
            DocumentService documentService,
            DocumentRepository documentRepository,
            AuditService auditService,
            AppUserRepository appUserRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.auditService = auditService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/documents")
    public String documents(
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "documents",
                documentService.getDocumentsByUser(
                        authentication.getName()));

        return "documents";
    }

    @GetMapping("/documents/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Document document = documentService.uploadDocument(
                    title,
                    file,
                    authentication.getName());

            auditService.logAction(
                    authentication.getName(),
                    "UPLOAD",
                    document.getTitle());

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Document uploaded successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Upload failed: " + e.getMessage());

            return "redirect:/documents/upload";
        }

        return "redirect:/documents";
    }

    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        Document document = documentRepository.findById(id)
                .filter(doc -> doc.getCompany() != null
                        && user.getCompany() != null
                        && doc.getCompany().getId()
                                .equals(user.getCompany().getId()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        Path uploadDir = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        Path filePath = uploadDir
                .resolve(document.getFileName())
                .normalize();

        if (!filePath.startsWith(uploadDir)
                || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }

        Resource resource = new UrlResource(filePath.toUri());

        auditService.logAction(
                authentication.getName(),
                "DOWNLOAD",
                document.getTitle());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/documents/delete/{id}")
    public String deleteDocument(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        Document document = documentRepository.findById(id)
                .filter(doc -> doc.getCompany() != null
                        && user.getCompany() != null
                        && doc.getCompany().getId()
                                .equals(user.getCompany().getId()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        Path uploadDir = Paths.get("uploads")
                .toAbsolutePath()
                .normalize();

        Path filePath = uploadDir
                .resolve(document.getFileName())
                .normalize();

        if (!filePath.startsWith(uploadDir)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN);
        }

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            documentRepository.delete(document);

            auditService.logAction(
                    authentication.getName(),
                    "DELETE",
                    document.getTitle());

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Document deleted successfully!");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Could not delete the document.");
        }

        return "redirect:/documents";
    }
}