package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;
import com.nhom4.xoxo.service.PostService;
import com.nhom4.xoxo.service.ReportService;
import com.nhom4.xoxo.service.StoryService;
import com.nhom4.xoxo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Report Management", description = "APIs for managing reports and content moderation")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final PostService postService;
    private final StoryService storyService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getName())) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    private boolean isContentOwner(User user, ReportTargetType targetType, Long targetId) {
        try {
            switch (targetType) {
                case USER:
                    return user.getId().equals(targetId);
                case POST:
                    var post = postService.getPostById(targetId);
                    return post.isPresent() && post.get().getAuthor().getId().equals(user.getId());
                case COMMENT:
                    // For now, return false - would need CommentService to check ownership
                    return false;
                case GROUP:
                    // Would need to check if user is group creator/admin
                    // For now, return false - would need GroupService to check ownership
                    return false;
                case MESSAGE:
                    // For now, return false - would need ChatService to check ownership
                    return false;
                case STORY:
                    var story = storyService.getStoryById(targetId);
                    return story.isPresent() && story.get().getUser().getId().equals(user.getId());
                default:
                    return false;
            }
        } catch (Exception e) {
            // If any error occurs, assume user is not owner for security
            return false;
        }
    }

    @Operation(
        summary = "Create a new report",
        description = "Create a report for inappropriate content. The reporter ID is automatically set from the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report created successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<WrapRes<ReportResponse>> createReport(
        @Parameter(description = "Report details", required = true)
        @Valid @RequestBody ReportRequest request) {
        request.setReporterId(getCurrentUser().getId());
        ReportResponse response = reportService.createReport(request);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(
        summary = "Get my reports",
        description = "Retrieve all reports created by the current user with pagination and sorting"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-reports")
    public ResponseEntity<WrapRes<Page<ReportResponse>>> getMyReports(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "Sort parameters (field,direction)", example = "createdAt,desc")
        @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<ReportResponse> responses = reportService.getReportsByUser(getCurrentUser().getId(), pageable);
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @Operation(
        summary = "Get reports by target",
        description = "Retrieve all reports for a specific target (post, user, group, etc.). Only accessible by admin/owner or content owner."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view these reports"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<WrapRes<Page<ReportResponse>>> getReportsByTarget(
        @Parameter(description = "Type of target being reported", required = true)
        @PathVariable ReportTargetType targetType,
        @Parameter(description = "ID of the target being reported", required = true)
        @PathVariable Long targetId,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        
        // Check if user is admin or owner of the content
        if (!userService.isAdminOrOwner(currentUser) && !isContentOwner(currentUser, targetType, targetId)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("FORBIDDEN", "You can only view reports for your own content or be an admin"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> responses = reportService.getReportsByTarget(targetType, targetId, pageable);
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @Operation(
        summary = "Get reports by reason",
        description = "Retrieve all reports filtered by violation reason. Admin/Owner access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Owner role required"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/reason/{reason}")
    public ResponseEntity<WrapRes<Page<ReportResponse>>> getReportsByReason(
        @Parameter(description = "Reason for the report", required = true)
        @PathVariable ReportReason reason,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("FORBIDDEN", "Admin or Owner role required"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> responses = reportService.getReportsByReason(reason, pageable);
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @Operation(
        summary = "Get reports by priority",
        description = "Retrieve all reports filtered by priority level. Admin/Owner access required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reports retrieved successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Owner role required"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/priority/{priority}")
    public ResponseEntity<WrapRes<Page<ReportResponse>>> getReportsByPriority(
        @Parameter(description = "Priority level (1-5)", required = true, example = "3")
        @PathVariable Integer priority,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @RequestParam(defaultValue = "10") int size) {
        
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("FORBIDDEN", "Admin or Owner role required"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> responses = reportService.getReportsByPriority(priority, pageable);
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @Operation(
        summary = "Get report by ID",
        description = "Retrieve detailed information about a specific report. Only accessible by the reporter or admin/owner."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report retrieved successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Can only view your own reports or be an admin"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{reportId}")
    public ResponseEntity<WrapRes<ReportResponse>> getReportById(
        @Parameter(description = "Report ID", required = true, example = "1")
        @PathVariable Long reportId) {
        ReportResponse response = reportService.getReportById(reportId);
        User currentUser = getCurrentUser();
        
        // Only reporter or admin can view report details
        if (!response.getReporterId().equals(currentUser.getId()) && !userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("FORBIDDEN", "You can only view your own reports or be an admin"));
        }
        
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(
        summary = "Update report",
        description = "Update an existing report. Only the reporter can update their own report and only if it's still in PENDING status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report updated successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Can only update your own pending reports"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or report already processed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{reportId}")
    public ResponseEntity<WrapRes<ReportResponse>> updateReport(
        @Parameter(description = "Report ID", required = true, example = "1")
        @PathVariable Long reportId,
        @Parameter(description = "Updated report details", required = true)
        @Valid @RequestBody ReportRequest request) {
        ReportResponse existingReport = reportService.getReportById(reportId);
        User currentUser = getCurrentUser();
        
        if (!existingReport.getReporterId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(WrapRes.error("FORBIDDEN", "You can only update your own reports"));
        }
        
        // Check if report is still in PENDING status
        if (existingReport.getStatus() != ReportStatus.PENDING) {
            return ResponseEntity.status(400).body(WrapRes.error("BAD_REQUEST", "Can only update reports that are still pending"));
        }
        
        request.setReporterId(currentUser.getId());
        ReportResponse response = reportService.updateReport(reportId, request);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(
        summary = "Delete report",
        description = "Delete a report. Only the reporter can delete their own report and only if it's still in PENDING status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Report deleted successfully",
            content = @Content(schema = @Schema(implementation = WrapRes.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Can only delete your own pending reports"),
        @ApiResponse(responseCode = "404", description = "Report not found"),
        @ApiResponse(responseCode = "400", description = "Report already processed, cannot be deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{reportId}")
    public ResponseEntity<WrapRes<Void>> deleteReport(
        @Parameter(description = "Report ID", required = true, example = "1")
        @PathVariable Long reportId) {
        ReportResponse existingReport = reportService.getReportById(reportId);
        User currentUser = getCurrentUser();
        
        if (!existingReport.getReporterId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(WrapRes.error("FORBIDDEN", "You can only delete your own reports"));
        }
        
        // Check if report is still in PENDING status
        if (existingReport.getStatus() != ReportStatus.PENDING) {
            return ResponseEntity.status(400).body(WrapRes.error("BAD_REQUEST", "Can only delete reports that are still pending"));
        }
        
        reportService.deleteReport(reportId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

}
